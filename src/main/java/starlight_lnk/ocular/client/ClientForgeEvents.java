package starlight_lnk.ocular.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import starlight_lnk.ocular.Ocular;
import starlight_lnk.ocular.client.effects.*;

@Mod.EventBusSubscriber(modid = Ocular.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            BloodEffect.tick();
            RainEffect.tick();
            SnowEffect.tick();
            SandEffect.tick();
            ExplosionEffect.tick();
        } catch (Throwable ignored) {}
    }

    // НОВЫЙ ТРИГГЕР: Ловим звук взрыва на клиенте!
    @SubscribeEvent
    public static void onSoundPlay(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        // Если в системном названии звука есть слово "explode" (взрыв)
        if (sound.getLocation().getPath().contains("explode")) {
            // Получаем координаты, откуда исходит звук
            Vec3 pos = new Vec3(sound.getX(), sound.getY(), sound.getZ());

            // Вызываем нашу контузию (радиус 4.0 - стандартный взрыв ТНТ)
            ExplosionEffect.onExplosion(pos, 4.0f);
        }
    }
}