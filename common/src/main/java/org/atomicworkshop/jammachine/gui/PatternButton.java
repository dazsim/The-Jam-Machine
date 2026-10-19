package org.atomicworkshop.jammachine.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.DyeColor;
import org.atomicworkshop.jammachine.sequencing.Sequencer;

public class PatternButton extends AbstractButton {
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
    private final int interval;
    private final int pitch;
    private final Sequencer sequencer;
    protected final IPressable onPress;
    private int colour;
    private boolean isChecked;

    public PatternButton(int x, int y, int width, int height, int interval, int pitch, Sequencer sequencer, IPressable onPressed) {
        super(x, y, width, height, Component.empty());
        this.interval = interval;
        this.pitch = pitch;
        this.sequencer = sequencer;
        this.onPress = onPressed;

        this.isChecked = sequencer.isPlaying() && sequencer.getCurrentPattern().isPitchActiveAtInterval(interval, pitch);
        updateColour();
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.isChecked = !this.isChecked;
        this.onPress.onPress(pitch, interval, isChecked);
        updateColour();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        //No click for you
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int r = Math.min(255, ((colour & 0xff0000) >> 16) * 2);
        int g = Math.min(255, ((colour & 0x00ff00) >> 8) * 2);
        int b = Math.min(255, (colour & 0x0000ff) * 2);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.isHoveredOrFocused() ? BUTTON_HIGHLIGHTED : BUTTON,
                this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.color(ARGB.as8BitChannel(this.alpha), r, g, b));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    private static int getSequenceButtonColour(int currentInterval, int interval, int pitch, boolean isEnabled)
    {
        int disabledItemInactiveInterval = DyeColor.LIGHT_GRAY.getTextureDiffuseColor();
        int enabledItemInactiveInterval = DyeColor.BROWN.getTextureDiffuseColor();
        int disabledItemActiveInterval = DyeColor.WHITE.getTextureDiffuseColor();
        int enabledItemActiveInterval = DyeColor.YELLOW.getTextureDiffuseColor();

        int disabledItemInactiveIntervalSharp = DyeColor.BLACK.getTextureDiffuseColor();
        int enabledItemInactiveIntervalSharp = DyeColor.BROWN.getTextureDiffuseColor();
        int disabledItemActiveIntervalSharp = DyeColor.GRAY.getTextureDiffuseColor();
        int enabledItemActiveIntervalSharp = DyeColor.ORANGE.getTextureDiffuseColor();

        if (isSharpPitch(pitch)) {
            //Use sharp colours.
            if (currentInterval == interval)
            {
                return isEnabled ? enabledItemActiveIntervalSharp : disabledItemActiveIntervalSharp;
            } else {
                return isEnabled ? enabledItemInactiveIntervalSharp : disabledItemInactiveIntervalSharp;
            }
        } else {
            //Use non-sharp colours.
            if (currentInterval == interval) {
                return isEnabled ? enabledItemActiveInterval : disabledItemActiveInterval;
            } else {
                return isEnabled ? enabledItemInactiveInterval : disabledItemInactiveInterval;
            }
        }
    }

    @SuppressWarnings("OverlyComplexBooleanExpression")
    private static boolean isSharpPitch(int pitch)
    {
        return pitch == 0 || pitch == 2 || pitch == 4 || pitch == 7 || pitch == 9 || pitch == 12 ||
                pitch == 14 || pitch == 16 || pitch == 19 || pitch == 21 || pitch == 24;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
        updateColour();
    }

    private void updateColour() {
        this.colour = getSequenceButtonColour(sequencer.getCurrentInterval(), interval, pitch, isChecked);
    }

    public void updateInterval() {
        updateColour();
    }

    public interface IPressable {
        void onPress(int pitch, int interval, boolean isChecked);
    }
}
