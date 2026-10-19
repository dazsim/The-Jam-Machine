package org.atomicworkshop.jammachine.gui;

import com.google.common.collect.ImmutableList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import org.atomicworkshop.jammachine.JamMachineMod;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.network.MachineControl;
import org.atomicworkshop.jammachine.network.SequencerSetPitchAtInterval;
import org.atomicworkshop.jammachine.sequencing.AdjacentNoteBlock;
import org.atomicworkshop.jammachine.sequencing.MusicPlayer;
import org.atomicworkshop.jammachine.sequencing.Pattern;
import org.atomicworkshop.jammachine.sequencing.Sequencer;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

public class SequencerScreen extends AbstractContainerScreen<SequencerContainer> {
    private final SequencerTileEntity tileEntity;
    private final Sequencer sequencer;

    private static final Identifier guiTexture = Reference.resource("textures/gui/sequencer.png");

    public SequencerScreen(SequencerContainer container, Inventory inv, Component titleIn) {
        super(container, inv, titleIn, 196, 246);
        tileEntity = container.getTileEntity();
        sequencer = tileEntity.getSequencer();
    }

    private PatternButton[][] patternButtons = new PatternButton[16][25];

    @Override
    protected void init() {
        super.init();

        int width = 10;
        int height = 8;

        for (int interval=0;interval<16;interval++) {
            for (int pitch=0;pitch<25;pitch++) {
                int x = interval * (width + 1) + (interval / 4) + leftPos + 8;
                int y = (24-pitch) * (height + 1) + topPos + 8;

                PatternButton b = new PatternButton(x, y, width, height, interval, pitch, sequencer, this::onPatternPressed);
                addRenderableWidget(b);
                patternButtons[interval][pitch] = b;
            }
        }

        int panelX = leftPos + 200;
        int bpmYPos = topPos + 40;
        int bpmWidth = 14;
        int bpmHeight = 10;

        final int[] steps = { -10, -1, 1, 10 };
        final String[] labels = { "<<", "<", ">", ">>" };
        for (int i = 0; i < steps.length; i++) {
            final int step = steps[i];
            bpmButtons[i] = addRenderableWidget(Button.builder(Component.literal(labels[i]), (button) ->
                    send(MachineControl.SET_BPM, sequencer.getBeatsPerMinute() + step)
            ).bounds(panelX + i * (bpmWidth + 1), bpmYPos, bpmWidth, bpmHeight).build());
        }

        //pattern banks, two rows of four
        for (int i = 0; i < 8; i++) {
            final int patternIndex = i;
            patternBankButtons[i] = addRenderableWidget(Button.builder(Component.literal(String.valueOf(i + 1)), (button) ->
                    send(MachineControl.SELECT_PATTERN, patternIndex)
            ).bounds(panelX + (i & 3) * (bpmWidth + 1), bpmYPos + 16 + (i >> 2) * 15, bpmWidth, 14).build());
        }

        progButton = addRenderableWidget(Button.builder(Component.literal("Prog"), (button) ->
                send(MachineControl.TOGGLE_PROGRAM, 0)
        ).bounds(panelX, bpmYPos + 50, (bpmWidth * 4) + 3, 18).build());

        //brush palette, undyed on its own row then the sixteen dyes
        brush = sequencer.getTrackColour();
        final int paletteY = topPos + 140;
        addRenderableWidget(new ColourSwatch(panelX, paletteY, (bpmWidth * 4) + 3, 9, Sequencer.PLAIN_CABLE, () -> brush, this::pickBrush));
        for (int i = 0; i < 16; i++) {
            addRenderableWidget(new ColourSwatch(panelX + (i & 3) * (bpmWidth + 1), paletteY + 12 + (i >> 2) * 11, bpmWidth, 10, i, () -> brush, this::pickBrush));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.jammachine.set_track"), (button) ->
                send(MachineControl.TRACK_COLOUR, brush)
        ).bounds(panelX, paletteY + 58, (bpmWidth * 4) + 3, 14).build());
    }

    private int brush;

    private void pickBrush(int colour) {
        brush = colour;
    }

    private final Button[] bpmButtons = new Button[4];
    private final Button[] patternBankButtons = new Button[8];
    private Button progButton;

    private void send(int action, int value) {
        JamMachineMod.PLATFORM.sendToServer(new MachineControl(tileEntity.getBlockPos(), action, value, 0));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < 8; i++) {
            Component label = Component.literal(String.valueOf(i + 1));
            if (i == sequencer.getCurrentPatternIndex()) {
                label = label.copy().withStyle(ChatFormatting.YELLOW);
            } else if (i == sequencer.getPendingPatternIndex()) {
                label = label.copy().withStyle(ChatFormatting.GOLD);
            }
            patternBankButtons[i].setMessage(label);
        }
        //a controller sets the tempo for everything it drives
        for (final Button button : bpmButtons) {
            button.active = !tileEntity.hasController();
        }
        progButton.setMessage(sequencer.isProgramming()
                ? Component.literal("Prog " + sequencer.getProgramLength()).withStyle(ChatFormatting.YELLOW)
                : Component.literal("Prog"));

        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        graphics.text(font, "SEQ-9001", leftPos + 200, topPos + 8, 0xFFFFFFFF);

        float textScale = 1.5f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(leftPos + 200, topPos + 22);
        graphics.pose().scale(textScale, textScale);
        final String bpmText = String.valueOf(sequencer.getBeatsPerMinute());
        graphics.text(font, bpmText, 0, 0, 0xFFFFFFFF);
        graphics.pose().popMatrix();
        if (tileEntity.hasController()) {
            graphics.text(font, "EXT", leftPos + 240, topPos + 26, 0xFFFFAA00);
        }

        graphics.text(font, Component.translatable("gui.jammachine.track"), leftPos + 200, topPos + 115, 0xFFFFFFFF);
        ColourSwatch.draw(graphics, leftPos + 245, topPos + 114, 14, 10, sequencer.getTrackColour(), false);
        graphics.text(font, Component.translatable("gui.jammachine.brush"), leftPos + 200, topPos + 129, 0xFFFFFFFF);
        ColourSwatch.draw(graphics, leftPos + 245, topPos + 128, 14, 10, brush, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, guiTexture, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    private void onPatternPressed(int finalPitch, int finalInterval, boolean isChecked, boolean isPicking) {
        final Pattern pattern = sequencer.getCurrentPattern();
        final int track = sequencer.getTrackColour();
        final int own = pattern.getColour(finalInterval, finalPitch);
        final boolean wasOn = !isChecked;

        //ctrl-click picks up a note's colour
        if (isPicking) {
            if (wasOn) brush = sequencer.colourOf(pattern, finalInterval, finalPitch);
            return;
        }
        if (brush == track) {
            if (wasOn && own != Pattern.TRACK_COLOUR) {
                paint(finalInterval, finalPitch, Pattern.TRACK_COLOUR);
                return;
            }
        } else if (!wasOn || own != brush) {
            paint(finalInterval, finalPitch, brush);
            return;
        }
        setNote(finalPitch, finalInterval, isChecked);
    }

    private void paint(int interval, int pitch, int colour) {
        final Pattern pattern = sequencer.getCurrentPattern();
        if (!pattern.isPitchActiveAtInterval(interval, pitch)) {
            setNote(pitch, interval, true);
        }
        pattern.setColour(interval, pitch, colour);
        JamMachineMod.PLATFORM.sendToServer(new MachineControl(tileEntity.getBlockPos(), MachineControl.PAINT_NOTE,
                MachineControl.noteIndex(sequencer.getCurrentPatternIndex(), interval, pitch), colour));
    }

    private void setNote(int finalPitch, int finalInterval, boolean isChecked) {
        menu.set(finalPitch, finalInterval, isChecked);
        final Level world = tileEntity.getLevel();
        assert world != null;
        JamMachineMod.PLATFORM.sendToServer(
                new SequencerSetPitchAtInterval(
                        sequencer.getCurrentPatternIndex(),
                        finalPitch,
                        finalInterval,
                        isChecked,
                        tileEntity.getBlockPos(),
                        world.dimension()
                ));
        sequencer.getCurrentPattern().setPitchAtInterval(finalInterval, finalPitch, isChecked);
        if (isChecked) {
            ImmutableList<AdjacentNoteBlock> availableNoteBlocks = sequencer.getAvailableNoteBlocks();
            if (!availableNoteBlocks.isEmpty()) {
                AdjacentNoteBlock noteBlock = availableNoteBlocks.get(0);
                MusicPlayer.playNote(sequencer, noteBlock, (byte) finalPitch);
            }
        }
    }
}
