package starlight_lnk.ocular.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import starlight_lnk.ocular.client.effects.BloodEffect;
import starlight_lnk.ocular.client.effects.RainEffect;
import starlight_lnk.ocular.client.effects.SnowEffect;
import starlight_lnk.ocular.client.effects.SandEffect;
import starlight_lnk.ocular.client.effects.ExplosionEffect;
import starlight_lnk.ocular.client.OcularConfig;

public final class OcularRenderer {
    private static ShaderInstance shader;

    private static RenderTarget fxTarget;
    private static int lastW = -1, lastH = -1;
    private static boolean renderedThisFrame = false;

    private static float renderTime = 0.0f;
    private static long lastTime = 0;

    private static float lastYaw = Float.NaN;
    private static float smoothedYawDelta = 0.0f;

    private static float smoothedPitch = 0f;
    private static float smoothedStorm = 0f;
    private static float smoothedRain = 0f;
    private static float smoothedSnow = 0f;
    private static float smoothedSand = 0f;

    private static boolean wasInWater = false;
    private static float splashIntensity = 0.0f;
    private static float splashTemp = 0.5f;

    private OcularRenderer() {}

    public static void setShader(ShaderInstance s) { shader = s; }

    public static void beginFrame() {
        renderedThisFrame = false;
    }

    public static void renderAfterHand() {
        if (shader == null || renderedThisFrame) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        if (lastTime == 0) lastTime = now;
        float dt = (now - lastTime) / 1000.0f;
        lastTime = now;

        if (dt > 0.1f) dt = 0.1f;
        renderTime += dt;
        if (renderTime > 100000f) renderTime -= 100000f;

        float currentYaw = mc.gameRenderer.getMainCamera().getYRot();
        if (Float.isNaN(lastYaw)) lastYaw = currentYaw;
        float rawDelta = Mth.wrapDegrees(currentYaw - lastYaw);
        lastYaw = currentYaw;

        smoothedYawDelta += rawDelta * 0.015f;
        smoothedYawDelta *= 0.85f;
        smoothedYawDelta = Mth.clamp(smoothedYawDelta, -1.5f, 1.5f);

        float targetPitch = mc.player.getXRot();
        float pitchLerpSpeed = (targetPitch > smoothedPitch) ? 0.8f : 5.0f;
        smoothedPitch += (targetPitch - smoothedPitch) * Math.min(1.0f, dt * pitchLerpSpeed);

        float targetStorm = RainEffect.isStorming ? 1.0f : 0.0f;
        smoothedStorm += (targetStorm - smoothedStorm) * Math.min(1.0f, dt * 0.5f);

        boolean inWater = mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        if (!inWater && wasInWater) {
            splashIntensity = 1.0f;
            splashTemp = mc.level.getBiome(mc.player.blockPosition()).value().getBaseTemperature();
        }
        wasInWater = inWater;

        if (splashIntensity > 0.0f) {
            splashIntensity -= dt * 0.4f;
            if (splashIntensity < 0.0f) splashIntensity = 0.0f;
        }
        float isColdBiome = splashTemp < 0.15f ? 1.0f : 0.0f;

        float blood = BloodEffect.CHANNEL.get();
        float rain  = RainEffect.CHANNEL.get();
        float snow  = SnowEffect.CHANNEL.get();
        float sand  = SandEffect.CHANNEL.get();
        float explosion = ExplosionEffect.CHANNEL.get();

        // Удалили тестовый Shift

        // Если все эффекты по нулям, выходим и не тратим ресурсы
        if (blood <= 0.001f && rain <= 0.001f && snow <= 0.001f && sand <= 0.001f && explosion <= 0.001f && splashIntensity <= 0.001f) return;

        RenderTarget main = mc.getMainRenderTarget();
        if (main == null) return;

        ensureTarget(main.width, main.height);
        if (fxTarget == null) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        smoothedRain += (rain - smoothedRain) * Math.min(1.0f, dt * 0.5f);
        smoothedSnow += (snow - smoothedSnow) * Math.min(1.0f, dt * 0.5f);
        smoothedSand += (sand - smoothedSand) * Math.min(1.0f, dt * 3.0f);

        fxTarget.bindWrite(true);
        RenderSystem.viewport(0, 0, fxTarget.width, fxTarget.height);

        drawFullscreen(main.getColorTextureId(), blood, rain, snow, sand, explosion, splashIntensity, isColdBiome, main.width, main.height, true);

        main.bindWrite(true);
        RenderSystem.viewport(0, 0, main.width, main.height);
        drawFullscreen(fxTarget.getColorTextureId(), 0f, 0f, 0f, 0f, 0f, 0f, 0f, main.width, main.height, false);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        main.bindWrite(false);
        renderedThisFrame = true;
    }

    private static void ensureTarget(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (fxTarget != null && w == lastW && h == lastH) return;
        if (fxTarget != null) { fxTarget.destroyBuffers(); fxTarget = null; }
        fxTarget = new TextureTarget(w, h, false, Minecraft.ON_OSX);
        lastW = w; lastH = h;
    }

    private static void drawFullscreen(int texId, float blood, float rain, float snow, float sand, float explosion, float splash, float isCold, int w, int h, boolean apply) {
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, texId);
        shader.setSampler("DiffuseSampler", texId);

        if (shader.safeGetUniform("BloodIntensity") != null) shader.safeGetUniform("BloodIntensity").set(apply ? blood : 0f);
        if (shader.safeGetUniform("RainIntensity") != null) shader.safeGetUniform("RainIntensity").set(apply ? smoothedRain : 0f);
        if (shader.safeGetUniform("RainDensityMulti") != null) shader.safeGetUniform("RainDensityMulti").set(OcularConfig.RAIN_DENSITY.get().floatValue());
        if (shader.safeGetUniform("RainOpacityMulti") != null) shader.safeGetUniform("RainOpacityMulti").set(OcularConfig.RAIN_OPACITY.get().floatValue());
        if (shader.safeGetUniform("SnowIntensity") != null) shader.safeGetUniform("SnowIntensity").set(apply ? smoothedSnow : 0f);
        if (shader.safeGetUniform("SandIntensity") != null) shader.safeGetUniform("SandIntensity").set(apply ? smoothedSand : 0f);
        if (shader.safeGetUniform("ExplosionIntensity") != null) shader.safeGetUniform("ExplosionIntensity").set(apply ? explosion : 0f);
        if (shader.safeGetUniform("WaterSplash") != null) shader.safeGetUniform("WaterSplash").set(apply ? splash : 0f);
        if (shader.safeGetUniform("IsColdBiome") != null) shader.safeGetUniform("IsColdBiome").set(isCold);
        if (shader.safeGetUniform("Time") != null) shader.safeGetUniform("Time").set(renderTime);
        if (shader.safeGetUniform("CameraYawDelta") != null) shader.safeGetUniform("CameraYawDelta").set(apply ? smoothedYawDelta : 0f);
        if (shader.safeGetUniform("CameraPitch") != null) shader.safeGetUniform("CameraPitch").set(smoothedPitch);
        if (shader.safeGetUniform("ScreenSize") != null) shader.safeGetUniform("ScreenSize").set((float)w, (float)h);
        if (shader.safeGetUniform("StormBlend") != null) shader.safeGetUniform("StormBlend").set(smoothedStorm);

        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(-1, -1, 0).uv(0, 1).endVertex();
        b.vertex( 1, -1, 0).uv(1, 1).endVertex();
        b.vertex( 1,  1, 0).uv(1, 0).endVertex();
        b.vertex(-1,  1, 0).uv(0, 0).endVertex();
        BufferUploader.drawWithShader(b.end());
    }
}
