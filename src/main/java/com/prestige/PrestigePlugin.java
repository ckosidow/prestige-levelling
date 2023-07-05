package com.prestige;

import javax.inject.Inject;

import com.google.inject.Provides;
import com.prestige.levelUp.LevelUpDisplayInput;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static net.runelite.api.ScriptID.XPDROPS_SETDROPSIZE;

@Slf4j
@PluginDescriptor(
        name = "Prestige",
        description = "Resets xp to 0 and doubles xp rate between levels 92 and 99",
        tags = {"levelling", "reset", "99", "92"}
)
public class PrestigePlugin extends Plugin {
    private static final String TOTAL_LEVEL_TEXT_PREFIX = "Total level:<br>";
    private int xpFactor = 2;
    private int maxXp = Experience.getXpForLevel(99);
    private int prestigeXP = maxXp - (maxXp / xpFactor);
    private static final List<Skill> COMBAT_SKILLS = Arrays.asList(Skill.ATTACK, Skill.DEFENCE, Skill.STRENGTH, Skill.MAGIC, Skill.RANGED);
    private static final Map<Skill, Integer> ACTUAL_SKILL_XP = new HashMap<>();
    private static final Map<Skill, Integer> ACTUAL_SKILL_BOOST = new HashMap<>();

    static {
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                ACTUAL_SKILL_XP.put(skill, 0);
                ACTUAL_SKILL_BOOST.put(skill, 0);
            }
        }
    }

    @Inject
    @Getter(AccessLevel.PUBLIC)
    private Client client;
    @Inject
    @Getter(AccessLevel.PUBLIC)
    private ClientThread clientThread;
    @Inject
    @Getter(AccessLevel.PUBLIC)
    private ChatboxPanelManager chatboxPanelManager;
    @Inject
    @Getter(AccessLevel.PUBLIC)
    private ChatMessageManager chatMessageManager;
    @Inject
    private PrestigeConfig config;
    private LevelUpDisplayInput input;

    @Provides
    PrestigeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PrestigeConfig.class);
    }

    private final Map<Skill, Integer> updatedSkills = new HashMap<>();
    private final List<Skill> levelledSkills = new ArrayList<>();

    @Subscribe
    public void onScriptPreFired(ScriptPreFired scriptPreFired) {
        if (scriptPreFired.getScriptId() == XPDROPS_SETDROPSIZE) {
            final int[] intStack = client.getIntStack();
            final Widget xpdrop = client.getWidget(intStack[0]);

            if (xpdrop != null && xpdrop.getChildren() != null) {
                Widget textWidget = xpdrop.getChild(0);
                String text = textWidget.getText();
                int xp = 0;
                boolean allDoubled = true;

                if (StringUtils.isNotBlank(text)) {
                    xp = Integer.parseInt(StringUtils.trim(textWidget.getText().replaceAll(",", "")));
                }

                for (Widget xpDropEntry : Arrays.stream(xpdrop.getChildren()).skip(1).collect(Collectors.toList())) {
                    if (xpDropEntry != null) {
                        int spriteId = xpDropEntry.getSpriteId();
                        SkillData skillData = SkillData.get(spriteId);

                        if (skillData == null) {
                            break;
                        }

                        Skill skill = skillData.getSkill();

                        int currentXp = client.getSkillExperience(skill);

                        if (!isPrestiged(currentXp)) {
                            allDoubled = false;
                        }
                    }
                }

                if (allDoubled) {
                    textWidget.setText(" " + xp * xpFactor);
                    textWidget.revalidate();
                    xpdrop.revalidate();
                }
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (input != null) {
            input.closeIfTriggered();
        }

        if (levelledSkills.isEmpty() || !chatboxPanelManager.getContainerWidget().isHidden()) {
            return;
        }

        final Skill skill = levelledSkills.remove(0);

        int xp = client.getSkillExperience(skill);

        // Reset the skill
        // Set xp rate to the xp modifier
        if (isPrestiged(xp)) {
            xp = prestigeXP(xp);
        }

        input = new LevelUpDisplayInput(this, skill, Experience.getLevelForXp(xp));
        chatboxPanelManager.openInput(input);
    }

    @Override
    protected void shutDown() {
        this.resetSkills();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (!configChanged.getGroup().equals("prestige")) {
            return;
        }

        this.calculatePrestigeRange();
        this.resetSkills();
        this.updateAllStats();
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent e) {
        final String[] stringStack = client.getStringStack();
        final int stringStackSize = client.getStringStackSize();

        switch (e.getEventName()) {
            case "skillTabTotalLevel":
                int level = 0;

                for (Skill s : Skill.values()) {
                    if (s == Skill.OVERALL) {
                        continue;
                    }

                    int xp = ACTUAL_SKILL_XP.get(s);

                    // Reset the skill
                    // Set xp rate to the xp modifier
                    if (isPrestiged(xp)) {
                        if (!config.showRealLevels() || isPrestigeLevelCloser(xp)) {
                            xp = (xp - prestigeXP) * xpFactor;
                        }
                    }

                    level += Experience.getLevelForXp(xp);
                }

                stringStack[stringStackSize - 1] = TOTAL_LEVEL_TEXT_PREFIX + level;
                break;
        }
    }

    @Override
    protected void startUp() {
        this.calculatePrestigeRange();

        this.updateAllStats();
    }

    private void updateAllStats() {
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                changeStat(skill, client.getRealSkillLevel(skill), ACTUAL_SKILL_BOOST.get(skill), true);
                client.queueChangedSkill(skill);
            }
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        this.changeStat(statChanged.getSkill(), statChanged.getLevel(), statChanged.getBoostedLevel(), false);
    }

    private void resetSkills() {
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                int xp = ACTUAL_SKILL_XP.get(skill);

                client.getRealSkillLevels()[skill.ordinal()] = Experience.getLevelForXp(xp);
                client.getSkillExperiences()[skill.ordinal()] = xp;
                client.getBoostedSkillLevels()[skill.ordinal()] = ACTUAL_SKILL_BOOST.get(skill);

                client.queueChangedSkill(skill);
            }
        }
    }

    private void changeStat(Skill skill, int level, int boostedLevel, boolean ignoreLevels) {
        int xp = client.getSkillExperience(skill);

        ACTUAL_SKILL_XP.put(skill, xp);
        ACTUAL_SKILL_BOOST.put(skill, boostedLevel);

        if (COMBAT_SKILLS.contains(skill)) {
            // Player doesn't want to show prestige for combat skills
            if (!config.enableCombat()) {
                return;
            }
        } else if (Skill.HITPOINTS == skill) {
            // Player doesn't want to show prestige for HP
            if (!config.enableHP()) {
                return;
            }
        } else if (Skill.PRAYER == skill) {
            // Player doesn't want to show prestige for Prayer
            if (!config.enablePrayer()) {
                return;
            }
        } else {
            // Player doesn't want to show prestige for non-combat skills
            if (!config.enableNonCombat()) {
                return;
            }
        }

        // Reset the skill
        // Set xp rate to the xp modifier
        if (isPrestiged(xp)) {
            if (!config.showRealLevels() || isPrestigeLevelCloser(xp)) {
                int prestigeXp = prestigeXP(xp);
                int newLevel = Experience.getLevelForXp(prestigeXp);
                int boostDiff = boostedLevel - Experience.getLevelForXp(ACTUAL_SKILL_XP.get(skill));

                // Set the prestige level and xp
                client.getRealSkillLevels()[skill.ordinal()] = newLevel;
                client.getSkillExperiences()[skill.ordinal()] = prestigeXp;
                client.getBoostedSkillLevels()[skill.ordinal()] = newLevel + boostDiff;

                int oldLevel = updatedSkills.get(skill) != null ? updatedSkills.get(skill) : level;

                if (!ignoreLevels && oldLevel < newLevel) {
                    levelledSkills.add(skill);
                }

                updatedSkills.put(skill, newLevel);
            }
        }
    }

    // Determines if the player's level is between 92 and 99, and is therefore prestiged
    private boolean isPrestiged(int xp) {
        return xp > prestigeXP && xp < maxXp;
    }

    // Determines if the player's real skill level is closer to levelling up than their prestige level
    private boolean isPrestigeLevelCloser(int xp) {
        int prestigeXp = prestigeXP(xp);
        int level = Experience.getLevelForXp(xp);
        int remaining = Experience.getXpForLevel(level + 1) - xp;
        int prestigeRemaining = Experience.getXpForLevel(Experience.getLevelForXp(prestigeXp) + 1) - prestigeXp;

        return (prestigeRemaining / xpFactor) < remaining;
    }

    private int prestigeXP(int xp) {
        return (xp - prestigeXP) * xpFactor;
    }

    private void calculatePrestigeRange() {
        xpFactor = config.xpFactor();
        maxXp = Experience.getXpForLevel(config.goalLevel());
        prestigeXP = maxXp - (maxXp / xpFactor);
    }
}
