package starlight_lnk.ocular.client;

import net.minecraftforge.common.ForgeConfigSpec;

public class OcularConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.DoubleValue RAIN_DENSITY;
    public static final ForgeConfigSpec.DoubleValue RAIN_OPACITY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("rain");
        RAIN_DENSITY = builder
                .comment("Multiplier for the number of raindrops. Default is 1.0.")
                .defineInRange("rainDensity", 1.0, 0.0, 1.0);
        RAIN_OPACITY = builder
                .comment("Multiplier for the opacity/visibility of raindrops. Default is 1.0.")
                .defineInRange("rainOpacity", 1.0, 0.0, 1.0);
        builder.pop();

        CLIENT_SPEC = builder.build();
    }
}
