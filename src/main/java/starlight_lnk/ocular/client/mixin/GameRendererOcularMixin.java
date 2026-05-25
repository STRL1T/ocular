package starlight_lnk.ocular.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import starlight_lnk.ocular.client.OcularRenderer;

@Mixin(GameRenderer.class)
public class GameRendererOcularMixin {

    @Inject(method = "renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"))
    private void ocular$begin(float pt, long nt, PoseStack ps, CallbackInfo ci) {
        OcularRenderer.beginFrame();
    }

    @Inject(
            method = "renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void ocular$afterHand(float pt, long nt, PoseStack ps, CallbackInfo ci) {
        OcularRenderer.renderAfterHand();
    }
}