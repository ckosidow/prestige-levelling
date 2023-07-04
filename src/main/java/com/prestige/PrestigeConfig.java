package com.prestige;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("prestige")
public interface PrestigeConfig extends Config {
    @ConfigItem(
            keyName = "showRealLevels",
            name = "Show real levels",
            description = "When enabled, your real level will display if it's closer to levelling-up than your prestige level",
            position = 1
    )
    default boolean showRealLevels() {
        return false;
    }

    @Range(max = 126, min = 1)
    @ConfigItem(
            keyName = "goalLevel",
            name = "Goal Level",
            description = "Max level you're trying to achieve - Half of this will be the level at which you prestige",
            position = 2
    )
    default int goalLevel() {
        return 99;
    }

    @ConfigItem(
            keyName = "enableHP",
            name = "Enable HP Prestige",
            description = "Enables prestige levels for HP NOTE: This may not work well with boosts or HP tracking",
            position = 3
    )
    default boolean enableHP() {
        return false;
    }

    @ConfigItem(
            keyName = "enablePrayer",
            name = "Enable Prayer Prestige",
            description = "Enables prestige levels for Prayer  NOTE: This may not work well with boosts or prayer tracking",
            position = 4
    )
    default boolean enablePrayer() {
        return false;
    }

    @ConfigItem(
            keyName = "enableCombat",
            name = "Enable Combat Prestige",
            description = "Enables prestige levels for combat skills (Attack, Strength, Defense, Ranged, Magic) NOTE: This may not work well with boosts",
            position = 5
    )
    default boolean enableCombat() {
        return true;
    }

    @ConfigItem(
            keyName = "enableNonCombat",
            name = "Enable Non-Combat Prestige",
            description = "Enables prestige levels for non-combat skills",
            position = 6
    )
    default boolean enableNonCombat() {
        return true;
    }
}
