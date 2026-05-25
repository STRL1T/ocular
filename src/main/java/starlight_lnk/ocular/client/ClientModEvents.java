package starlight_lnk.ocular.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import starlight_lnk.ocular.Ocular;

@Mod.EventBusSubscriber(modid = Ocular.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws Exception {
        ShaderInstance s = new ShaderInstance(
                event.getResourceProvider(),
                new ResourceLocation(Ocular.MODID, "screen_fx"),
                DefaultVertexFormat.POSITION_TEX
        );
        event.registerShader(s, OcularRenderer::setShader);
    }
}