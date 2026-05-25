package starlight_lnk.ocular.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class SandEffect {

    public static final EffectChannel CHANNEL = new EffectChannel(0.0025f, 0.05f);

    // Переменная для случайных позиций песка
    public static float currentSeed = 0.0f;
    // Счетчик тиков, когда мы НЕ бежим по песку
    private static int ticksSinceSand = 100;

    private SandEffect() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        Level level = mc.level;

        if (p == null || level == null) {
            CHANNEL.reset();
            return;
        }

        boolean isRunningOnSand = false;

        // Проверяем условия для песка
        if (p.isSprinting() && p.onGround()) {
            BlockPos posBelow = p.blockPosition().below();
            BlockState stateBelow = level.getBlockState(posBelow);

            if (stateBelow.is(BlockTags.SAND)) {
                isRunningOnSand = true;
            }
        }

        // Логика поведения
        if (isRunningOnSand) {
            // Если нас не было на песке дольше 1 секунды (20 тиков),
            // значит эффект успел исчезнуть -> генерируем новые случайные позиции
            if (ticksSinceSand > 20) {
                currentSeed = (float) (Math.random() * 1000.0);
            }
            ticksSinceSand = 0; // Сбрасываем таймер, пока мы бежим по песку
            CHANNEL.setTarget(1.0f);
        } else {
            // Если остановились, ушли с песка или просто прыгнули (в воздухе)
            ticksSinceSand++;
            CHANNEL.setTarget(0.0f);
        }

        CHANNEL.tick();
    }
}