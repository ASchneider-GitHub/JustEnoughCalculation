package me.towdium.jecalculation.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

public class JecaConfig {
    public static ModConfigSpec common;
    public static ModConfigSpec.BooleanValue clientMode;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("General");
        clientMode = b.define("clientMode", false);
        b.pop();
        common = b.build();
    }
}
