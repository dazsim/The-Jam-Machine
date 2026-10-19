package org.atomicworkshop.jammachine.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.atomicworkshop.jammachine.sequencing.Sequencer;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

//one cable colour in the sequencer's brush palette
public class ColourSwatch extends AbstractButton {
    private static final int PLAIN_RGB = 0xC8BEA6;

    private final int colour;
    private final IntSupplier selected;
    private final IntConsumer onPick;

    public ColourSwatch(int x, int y, int width, int height, int colour, IntSupplier selected, IntConsumer onPick) {
        super(x, y, width, height, name(colour));
        this.colour = colour;
        this.selected = selected;
        this.onPick = onPick;
        setTooltip(Tooltip.create(name(colour)));
    }

    public static int rgb(int colour) {
        if (colour < 0 || colour >= Sequencer.PLAIN_CABLE) return PLAIN_RGB;
        return DyeColor.byId(colour).getTextureDiffuseColor() & 0xFFFFFF;
    }

    public static Component name(int colour) {
        if (colour < 0 || colour >= Sequencer.PLAIN_CABLE) return Component.translatable("gui.jammachine.colour.plain");
        return Component.translatable("color.minecraft." + DyeColor.byId(colour).getName());
    }

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int colour, boolean highlight) {
        graphics.fill(x, y, x + width, y + height, highlight ? 0xFFFFFFFF : 0xFF202020);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000 | rgb(colour));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onPick.accept(colour);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        draw(graphics, getX(), getY(), getWidth(), getHeight(), colour, selected.getAsInt() == colour || isHoveredOrFocused());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
