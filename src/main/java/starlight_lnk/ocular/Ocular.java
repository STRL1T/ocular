package starlight_lnk.ocular;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Ocular.MODID)
public class Ocular {
    public static final String MODID = "ocular";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ocular() {
        LOGGER.info("Ocular loaded");
    }
}