package starlight_lnk.ocular;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import starlight_lnk.ocular.client.OcularConfig;
import starlight_lnk.ocular.client.gui.OcularConfigScreen;

@Mod(Ocular.MODID)
public class Ocular {
    public static final String MODID = "ocular";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ocular() {
        LOGGER.info("Ocular loaded");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, OcularConfig.CLIENT_SPEC);
        
        // Регистрируем наш интерфейс для кнопки Config в меню модов
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new OcularConfigScreen(screen)));
    }
}
