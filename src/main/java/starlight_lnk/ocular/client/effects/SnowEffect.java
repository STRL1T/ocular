package starlight_lnk.ocular.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class SnowEffect {
    // ИЗМЕНЕНО: 0.015f - очень медленное плавное появление, 0.005f - долгое таяние при выходе из биома
    public static final EffectChannel CHANNEL = new EffectChannel(0.015f, 0.015f);

    private SnowEffect() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        Level level = mc.level;

        if (p == null || level == null) {
            CHANNEL.reset();
            return;
        }

        float rainLevel = level.getRainLevel(1.0f);

        BlockPos pos = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());
        boolean canSeeSky = level.canSeeSky(pos);
        boolean isLookingDown = p.getXRot() > 45.0f;

        Biome biome = level.getBiome(pos).value();
        boolean hasPrecipitation = biome.hasPrecipitation();
        boolean isSnowBiome = biome.getBaseTemperature() < 0.15f;

        if (rainLevel <= 0.01f || !canSeeSky || isLookingDown || !hasPrecipitation || !isSnowBiome) {
            CHANNEL.setTarget(0.0f);
        } else {
            CHANNEL.setTarget(rainLevel);
        }

        CHANNEL.tick();
    }
}