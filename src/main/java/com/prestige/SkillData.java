package com.prestige;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;

@Getter
@AllArgsConstructor
enum SkillData {
    ATTACK(Skill.ATTACK, SpriteID.SKILL_ATTACK),
    STRENGTH(Skill.STRENGTH, SpriteID.SKILL_STRENGTH),
    DEFENCE(Skill.DEFENCE, SpriteID.SKILL_DEFENCE),
    RANGED(Skill.RANGED, SpriteID.SKILL_RANGED),
    PRAYER(Skill.PRAYER, SpriteID.SKILL_PRAYER),
    MAGIC(Skill.MAGIC, SpriteID.SKILL_MAGIC),
    RUNECRAFT(Skill.RUNECRAFT, SpriteID.SKILL_RUNECRAFT),
    CONSTRUCTION(Skill.CONSTRUCTION, SpriteID.SKILL_CONSTRUCTION),
    HITPOINTS(Skill.HITPOINTS, SpriteID.SKILL_HITPOINTS),
    AGILITY(Skill.AGILITY, SpriteID.SKILL_AGILITY),
    HERBLORE(Skill.HERBLORE, SpriteID.SKILL_HERBLORE),
    THIEVING(Skill.THIEVING, SpriteID.SKILL_THIEVING),
    CRAFTING(Skill.CRAFTING, SpriteID.SKILL_CRAFTING),
    FLETCHING(Skill.FLETCHING, SpriteID.SKILL_FLETCHING),
    SLAYER(Skill.SLAYER, SpriteID.SKILL_SLAYER),
    HUNTER(Skill.HUNTER, SpriteID.SKILL_HUNTER),
    MINING(Skill.MINING, SpriteID.SKILL_MINING),
    SMITHING(Skill.SMITHING, SpriteID.SKILL_SMITHING),
    FISHING(Skill.FISHING, SpriteID.SKILL_FISHING),
    COOKING(Skill.COOKING, SpriteID.SKILL_COOKING),
    FIREMAKING(Skill.FIREMAKING, SpriteID.SKILL_FIREMAKING),
    WOODCUTTING(Skill.WOODCUTTING, SpriteID.SKILL_WOODCUTTING),
    FARMING(Skill.FARMING, SpriteID.SKILL_FARMING);

    private final Skill skill;
    private final int sprite;

    static SkillData get(int sprite) {
        for (SkillData skill : values()) {
            if (skill.sprite == sprite) {
                return skill;
            }
        }

        return null;
    }
}
