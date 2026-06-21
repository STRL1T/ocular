package starlight_lnk.ocular.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class ExplosionEffect {
    // 1.0f - мгновенный удар по глазам.
    // УВЕЛИЧЕНО с 0.005f до 0.015f. Теперь эффект длится около 3.5 секунд вместо 10.
    public static final EffectChannel CHANNEL = new EffectChannel(1.0f, 0.015f);

    private ExplosionEffect() {}

    public static void onExplosion(Vec3 pos, float radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // получаем координаты игрока через position(), а затем меряем дистанцию
        double dist = mc.player.position().distanceTo(pos);
        double maxDist = radius * 5.0; // Волну от взрыва чувствуем на расстоянии до 5 радиусов

        if (dist < maxDist) {
            // Чем мы ближе к центру, тем ближе intensity к 1.0
            float intensity = (float) (1.0 - (dist / maxDist));

            // Заряжаем канал (pulse выберет максимум, если взрывов было несколько подряд)
            CHANNEL.pulse(intensity);
        }
    }

    public static void tick() {
        CHANNEL.setTarget(0.0f); // Всегда стремимся к нулю (восстановление)
        CHANNEL.tick();
    }
}
