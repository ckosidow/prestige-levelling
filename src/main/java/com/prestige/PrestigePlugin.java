package com.prestige;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

import static net.runelite.api.ScriptID.XPDROPS_SETDROPSIZE;

@Slf4j
@PluginDescriptor(
        name = "Prestige",
        description = "Resets xp to 0 and doubles xp rate between levels 92 and 99"
)
public class PrestigePlugin extends Plugin {
    private static final String TOTAL_LEVEL_TEXT_PREFIX = "Total level:<br>";
    private static final int HALF_XP = 6517253;
    private static final int MAX_XP = 13034431;

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;

    @Inject
    private PrestigeConfig config;

    @Provides
    PrestigeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PrestigeConfig.class);
    }

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
                    xp = Integer.parseInt(textWidget.getText());
                }

                for (Widget xpDropEntry : Arrays.stream(xpdrop.getChildren()).skip(1).collect(Collectors.toList())) {
                    int spriteId = xpDropEntry.getSpriteId();
                    SkillData skillData = SkillData.get(spriteId);

                    if (skillData == null) {
                        break;
                    }

                    Skill skill = skillData.getSkill();

                    int currentXp = client.getSkillExperience(skill);

                    if (currentXp < HALF_XP || currentXp >= MAX_XP) {
                        allDoubled = false;
                    }
                }

                if (allDoubled) {
                    textWidget.setText(" " + xp * 2);
                    textWidget.revalidate();
                    xpdrop.revalidate();
                }
            }
        }
    }

    @Override
    protected void shutDown() {
        clientThread.invoke(this::simulateSkillChange);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (!configChanged.getGroup().equals("Prestige")) {
            return;
        }

        clientThread.invoke(this::simulateSkillChange);
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent e) {
        final int[] intStack = client.getIntStack();
        final int intStackSize = client.getIntStackSize();
        final String[] stringStack = client.getStringStack();
        final int stringStackSize = client.getStringStackSize();

        switch (e.getEventName()) {
            case "skillTabBaseLevel":
                final int skillId = intStack[intStackSize - 2];
                final Skill skill = Skill.values()[skillId];
                int exp = client.getSkillExperience(skill);

                // Reset the skill
                // Set xp rate to x2
                if (exp > HALF_XP && exp < MAX_XP) {
                    exp = (exp - HALF_XP) * 2;
                }

                // alter the local variable containing the level to show
                intStack[intStackSize - 1] = Experience.getLevelForXp(exp);
                break;
            case "skillTabTotalLevel":
                int level = 0;

                for (Skill s : Skill.values()) {
                    if (s == Skill.OVERALL) {
                        continue;
                    }

                    int xp = client.getSkillExperience(s);

                    // Reset the skill
                    // Set xp rate to x2
                    if (xp > HALF_XP && xp < MAX_XP) {
                        xp = (xp - HALF_XP) * 2;
                    }

                    level += Experience.getLevelForXp(xp);
                }

                stringStack[stringStackSize - 1] = TOTAL_LEVEL_TEXT_PREFIX + level;
                break;
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        Skill skill = statChanged.getSkill();

        int xp = client.getSkillExperience(skill);

        // Reset the skill
        // Set xp rate to x2
        if (xp > HALF_XP && xp < MAX_XP) {
            xp = (xp - HALF_XP) * 2;

            client.getRealSkillLevels()[skill.ordinal()] = Experience.getLevelForXp(xp);
            client.getSkillExperiences()[skill.ordinal()] = xp;
        }
    }

    private void simulateSkillChange() {
        // this fires widgets listening for all skill changes
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                client.queueChangedSkill(skill);
            }
        }
    }
}
