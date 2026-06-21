package starlight_lnk.ocular.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class BloodEffect {
    public static final EffectChannel CHANNEL = new EffectChannel(0.55f, 0.015f);

    private static int hurtCooldown = 0;

    private BloodEffect() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            CHANNEL.reset();
            return;
        }

        float hp = p.getHealth();
        float maxHp = p.getMaxHealth();
        float hpRatio = Mth.clamp(hp / maxHp, 0.0f, 1.0f);

        float lowHpBase = (1.0f - Mth.clamp((hpRatio - 0.2f) / 0.3f, 0.0f, 1.0f)) * 0.45f;

        float hurtBoost = 0.0f;
        if (p.hurtTime > 0) {
            hurtBoost = (p.hurtTime / (float) p.hurtDuration) * 1.0f;
        }

        float target = Math.max(lowHpBase, hurtBoost);
        CHANNEL.setTarget(target);

        if (p.hurtTime == p.hurtDuration - 1 && hurtCooldown <= 0) {
            CHANNEL.pulse(0.9f);
            hurtCooldown = 5;
        }
        if (hurtCooldown > 0) hurtCooldown--;

        CHANNEL.tick();
    }
}
