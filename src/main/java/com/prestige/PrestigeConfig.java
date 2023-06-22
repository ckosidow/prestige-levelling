package com.prestige;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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

    @ConfigItem(
            keyName = "enableHP",
            name = "Enable HP Prestige",
            description = "Enables prestige levels for HP NOTE: This may not work well with boosts or HP tracking",
            position = 2
    )
    default boolean enableHP() {
        return false;
    }

    @ConfigItem(
            keyName = "enablePrayer",
            name = "Enable Prayer Prestige",
            description = "Enables prestige levels for Prayer  NOTE: This may not work well with boosts or prayer tracking",
            position = 3
    )
    default boolean enablePrayer() {
        return false;
    }

    @ConfigItem(
            keyName = "enableCombat",
            name = "Enable Combat Prestige",
            description = "Enables prestige levels for combat skills (Attack, Strength, Defense, Ranged, Magic) NOTE: This may not work well with boosts",
            position = 4
    )
    default boolean enableCombat() {
        return true;
    }

    @ConfigItem(
            keyName = "enableNonCombat",
            name = "Enable Non-Combat Prestige",
            description = "Enables prestige levels for non-combat skills",
            position = 5
    )
    default boolean enableNonCombat() {
        return true;
    }
}
