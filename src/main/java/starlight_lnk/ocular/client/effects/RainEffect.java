package starlight_lnk.ocular.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class RainEffect {
    // 0.08f - скорость появления капель, 0.02f - скорость исчезновения
    public static final EffectChannel CHANNEL = new EffectChannel(0.08f, 0.02f);
    public static boolean isStorming = false;

    private RainEffect() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        Level level = mc.level;

        if (p == null || level == null) {
            CHANNEL.reset();
            isStorming = false;
            return;
        }

        float rain = level.getRainLevel(1.0f);
        float thunder = level.getThunderLevel(1.0f);

        BlockPos pos = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());

        // ТРИГГЕР 1: Проверяем, есть ли над головой блоки (дерево/крыша)
        boolean canSeeSky = level.canSeeSky(pos);

        // ТРИГГЕР 2: Проверяем, опущена ли голова ниже 45 градусов
        boolean isLookingDown = p.getXRot() > 45.0f;

        // --- НОВАЯ ЛОГИКА БИОМОВ ---
        Biome biome = level.getBiome(pos).value();

        // Проверяем, бывают ли в этом биоме вообще осадки (исключает пустыни, саванны, Незер)
        boolean hasPrecipitation = biome.hasPrecipitation();

        // Проверяем, снежный ли это биом (В Minecraft снег идёт там, где базовая температура ниже 0.15)
        boolean isSnowBiome = biome.getBaseTemperature() < 0.15f;
        // ---------------------------

        // ЕДИНАЯ ЛОГИКА:
        // Если дождя в мире нет ИЛИ мы под крышей ИЛИ опустили голову
        // ИЛИ биом без осадков ИЛИ биом снежный -> сводим капли к нулю.
        if (rain <= 0.01f || !canSeeSky || isLookingDown || !hasPrecipitation || isSnowBiome) {
            CHANNEL.setTarget(0.0f);
        } else {
            CHANNEL.setTarget(rain);
        }

        CHANNEL.tick();

        // Отключаем молнии на экране, если смотрим в пол, стоим под блоком, в пустыне или в снегу
        isStorming = (thunder > 0.01f && rain > 0.01f && canSeeSky && !isLookingDown && hasPrecipitation && !isSnowBiome);
    }
}