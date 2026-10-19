package org.atomicworkshop.jammachine.gui;

import com.google.common.collect.ImmutableList;
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
import org.atomicworkshop.jammachine.network.SequencerSetPitchAtInterval;
import org.atomicworkshop.jammachine.sequencing.AdjacentNoteBlock;
import org.atomicworkshop.jammachine.sequencing.MusicPlayer;
import org.atomicworkshop.jammachine.sequencing.Sequencer;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

public class SequencerScreen extends AbstractContainerScreen<SequencerContainer> {
    private final SequencerTileEntity tileEntity;
    private final Sequencer sequencer;
    private int lastInterval = -1;

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

        int bpmXPos = 340;
        int bpmYPos = 30;
        int bpmWidth = 14;
        int bpmHeight = 10;

        addRenderableWidget(Button.builder(Component.literal("<<"), (button) -> {
            final int beatsPerMinute = sequencer.getBeatsPerMinute() - 10;
            sequencer.setDesiredBPM(beatsPerMinute);
        }).bounds(bpmXPos, bpmYPos, bpmWidth, bpmHeight).build());

        bpmXPos += bpmWidth + 1;
        addRenderableWidget(Button.builder(Component.literal("<"), (button) -> {
            final int beatsPerMinute = sequencer.getBeatsPerMinute() - 1;
            sequencer.setDesiredBPM(beatsPerMinute);
        }).bounds(bpmXPos, bpmYPos, bpmWidth, bpmHeight).build());

        bpmXPos += bpmWidth + 1;
        addRenderableWidget(Button.builder(Component.literal(">"), (button) -> {
            final int beatsPerMinute = sequencer.getBeatsPerMinute() + 1;
            sequencer.setDesiredBPM(beatsPerMinute);
        }).bounds(bpmXPos, bpmYPos, bpmWidth, bpmHeight).build());

        bpmXPos += bpmWidth + 1;
        addRenderableWidget(Button.builder(Component.literal(">>"), (button) -> {
            final int beatsPerMinute = sequencer.getBeatsPerMinute() + 10;
            sequencer.setDesiredBPM(beatsPerMinute);
        }).bounds(bpmXPos, bpmYPos, bpmWidth, bpmHeight).build());

        addRenderableWidget(Button.builder(Component.literal("Prog 1"), (button) -> {

        }).bounds(340, 45, (bpmWidth * 4) + 3, 18).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int currentInterval = sequencer.getCurrentInterval();
        if (!sequencer.isPlaying()) {
            currentInterval = -1;
        }

        if (currentInterval != lastInterval) {
            if (lastInterval != -1) {
                for (PatternButton patternButton : patternButtons[lastInterval]) {
                    patternButton.updateInterval();
                }
            }

            if (currentInterval != -1) {
                for (PatternButton patternButton : patternButtons[currentInterval]) {
                    patternButton.updateInterval();
                }
            }

            lastInterval = currentInterval;
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        graphics.text(font, "SEQ-9001", leftPos + 200, 8, 0xFFFFFFFF);

        float textScale = 1.5f;
        graphics.pose().pushMatrix();
        graphics.pose().scale(textScale, textScale);
        final String bpmText = String.valueOf(sequencer.getBeatsPerMinute());
        final int offset = font.width(bpmText) / 2;
        graphics.text(font, bpmText, leftPos + 105 - offset, 12, 0xFFFFFFFF);
        graphics.pose().popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, guiTexture, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    private void onPatternPressed(int finalPitch, int finalInterval, boolean isChecked) {
        menu.set(finalPitch, finalInterval, isChecked);
        final Level world = tileEntity.getLevel();
        assert world != null;
        JamMachineMod.PLATFORM.sendToServer(
                new SequencerSetPitchAtInterval(
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
