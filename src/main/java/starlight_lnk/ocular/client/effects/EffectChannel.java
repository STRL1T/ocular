package starlight_lnk.ocular.client.effects;

import net.minecraft.util.Mth;

/**
 * Универсальный канал: цель -> текущая интенсивность с раздельным сглаживанием.
 * Используем для всех эффектов (blood, rain, freeze...).
 */
public class EffectChannel {
    private float current = 0.0f;
    private float target = 0.0f;

    private float attackK  = 0.25f;
    private float releaseK = 0.04f;

    public EffectChannel(float attackK, float releaseK) {
        this.attackK = attackK;
        this.releaseK = releaseK;
    }

    public void setTarget(float t) {
        this.target = Mth.clamp(t, 0.0f, 1.0f);
    }

    public void pulse(float v) {
        this.current = Math.max(this.current, Mth.clamp(v, 0.0f, 1.0f));
    }

    public void tick() {
        float k = (target > current) ? attackK : releaseK;
        current += (target - current) * k;
        if (current < 0.001f) current = 0.0f;
    }

    public float get() { return current; }
    public boolean isActive() { return current > 0.001f; }
    public void reset() { current = 0.0f; target = 0.0f; }
}