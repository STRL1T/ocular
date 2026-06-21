package starlight_lnk.ocular.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import starlight_lnk.ocular.client.OcularConfig;

public class OcularConfigScreen extends Screen {
    private final Screen previousScreen;
    private ForgeSlider rainDensitySlider;
    private ForgeSlider rainOpacitySlider;

    public OcularConfigScreen(Screen previousScreen) {
        super(Component.translatable("ocular.config.title"));
        this.previousScreen = previousScreen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int startX = this.width / 2 - 100;
        int startY = 40;

        this.rainDensitySlider = new ForgeSlider(
                startX, startY, 200, 20,
                Component.translatable("ocular.config.rain_density"), Component.nullToEmpty("%"),
                0.0, 100.0, OcularConfig.RAIN_DENSITY.get() * 100.0, 10.0, 0, true
        );
        this.addRenderableWidget(this.rainDensitySlider);

        this.rainOpacitySlider = new ForgeSlider(
                startX, startY + 30, 200, 20,
                Component.translatable("ocular.config.rain_opacity"), Component.nullToEmpty("%"),
                0.0, 100.0, OcularConfig.RAIN_OPACITY.get() * 100.0, 10.0, 0, true
        );
        this.addRenderableWidget(this.rainOpacitySlider);

        this.addRenderableWidget(Button.builder(Component.translatable("ocular.config.apply"), btn -> {
            this.saveSettings();
        }).bounds(this.width / 2 - 100, this.height - 70, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("ocular.config.reset"), btn -> {
            this.rainDensitySlider.setValue(100.0);
            this.rainOpacitySlider.setValue(100.0);
            this.saveSettings();
        }).bounds(this.width / 2 + 2, this.height - 70, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("ocular.config.save_and_close"), btn -> {
            this.saveSettings();
            this.onClose();
        }).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    private void saveSettings() {
        OcularConfig.RAIN_DENSITY.set(this.rainDensitySlider.getValue() / 100.0);
        OcularConfig.RAIN_OPACITY.set(this.rainOpacitySlider.getValue() / 100.0);
        OcularConfig.CLIENT_SPEC.save();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
        }
    }
}
