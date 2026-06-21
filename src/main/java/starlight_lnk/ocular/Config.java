package starlight_lnk.ocular;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_BLOOD;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RAIN;
    public static final ForgeConfigSpec.DoubleValue  BLOOD_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue  RAIN_STRENGTH;

    static {
        BUILDER.push("ocular");
        ENABLE_BLOOD   = BUILDER.comment("Enable blood vignette effect").define("enableBlood", true);
        ENABLE_RAIN    = BUILDER.comment("Enable rain drops effect").define("enableRain", true);
        BLOOD_STRENGTH = BUILDER.comment("Blood effect strength multiplier").defineInRange("bloodStrength", 1.0, 0.0, 2.0);
        RAIN_STRENGTH  = BUILDER.comment("Rain effect strength multiplier").defineInRange("rainStrength", 1.0, 0.0, 2.0);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private Config() {}
}
