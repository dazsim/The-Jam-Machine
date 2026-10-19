package org.atomicworkshop.jammachine.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.atomicworkshop.jammachine.JamMachineMod;
import org.atomicworkshop.jammachine.network.MachineControl;
import org.atomicworkshop.jammachine.sequencing.Sequencer;
import org.atomicworkshop.jammachine.tiles.ControllerTileEntity;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

import java.util.ArrayList;
import java.util.List;

public class ControllerScreen extends AbstractContainerScreen<ControllerContainer> {
    private static final int ROW_TOP = 60;
    private static final int ROW_HEIGHT = 15;

    private final ControllerTileEntity tileEntity;
    private final List<BlockPos> shownSequencers = new ArrayList<>();
    private final List<Button[]> patternButtons = new ArrayList<>();
    private Button playButton;

    public ControllerScreen(ControllerContainer container, Inventory inventory, Component title) {
        super(container, inventory, title, 230, ROW_TOP + ControllerTileEntity.MAX_SEQUENCERS * ROW_HEIGHT + 8);
        tileEntity = container.getTileEntity();
    }

    @Override
    protected void init() {
        super.init();
        shownSequencers.clear();
        shownSequencers.addAll(tileEntity.getSequencers());
        patternButtons.clear();

        final int[] steps = { -10, -1, 1, 10 };
        final String[] labels = { "<<", "<", ">", ">>" };
        for (int i = 0; i < steps.length; i++) {
            final int step = steps[i];
            addRenderableWidget(Button.builder(Component.literal(labels[i]), (button) ->
                    send(MachineControl.SET_BPM, tileEntity.getBeatsPerMinute() + step, 0)
            ).bounds(leftPos + 80 + i * 17, topPos + 20, 16, 12).build());
        }

        playButton = addRenderableWidget(Button.builder(Component.literal("Play"), (button) ->
                send(MachineControl.TOGGLE_PLAY, 0, 0)
        ).bounds(leftPos + 160, topPos + 18, 60, 16).build());

        for (int slot = 0; slot < shownSequencers.size(); slot++) {
            final Button[] row = new Button[8];
            for (int pattern = 0; pattern < 8; pattern++) {
                final int s = slot, p = pattern;
                row[pattern] = addRenderableWidget(Button.builder(Component.literal(String.valueOf(pattern + 1)), (button) ->
                        send(MachineControl.SEQUENCER_PATTERN, s, p)
                ).bounds(leftPos + 110 + pattern * 14, topPos + ROW_TOP + slot * ROW_HEIGHT, 13, 13).build());
            }
            patternButtons.add(row);
        }
    }

    private void send(int action, int value, int extra) {
        JamMachineMod.PLATFORM.sendToServer(new MachineControl(tileEntity.getBlockPos(), action, value, extra));
    }

    private Sequencer sequencerAt(BlockPos pos) {
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof SequencerTileEntity te) {
            return te.getSequencer();
        }
        return null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!shownSequencers.equals(tileEntity.getSequencers())) {
            rebuildWidgets();
        }
        playButton.setMessage(Component.literal(tileEntity.isPlaying() ? "Stop" : "Play"));
        for (int slot = 0; slot < patternButtons.size(); slot++) {
            final Sequencer sequencer = sequencerAt(shownSequencers.get(slot));
            for (int pattern = 0; pattern < 8; pattern++) {
                Component label = Component.literal(String.valueOf(pattern + 1));
                if (sequencer != null && pattern == sequencer.getCurrentPatternIndex()) {
                    label = label.copy().withStyle(ChatFormatting.YELLOW);
                } else if (sequencer != null && pattern == sequencer.getPendingPatternIndex()) {
                    label = label.copy().withStyle(ChatFormatting.GOLD);
                }
                patternButtons.get(slot)[pattern].setMessage(label);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.text(font, "CON-9001", leftPos + 8, topPos + 8, 0xFFFFFFFF);
        graphics.pose().pushMatrix();
        graphics.pose().translate(leftPos + 8, topPos + 20);
        graphics.pose().scale(1.5f, 1.5f);
        graphics.text(font, String.valueOf(tileEntity.getBeatsPerMinute()), 0, 0, 0xFFFFFFFF);
        graphics.pose().popMatrix();
        graphics.text(font, "BPM", leftPos + 48, topPos + 24, 0xFFAAAAAA);

        if (shownSequencers.isEmpty()) {
            graphics.text(font, "No sequencers connected", leftPos + 8, topPos + ROW_TOP + 3, 0xFFAAAAAA);
        }
        for (int slot = 0; slot < shownSequencers.size(); slot++) {
            final BlockPos pos = shownSequencers.get(slot);
            final Sequencer sequencer = sequencerAt(pos);
            String name = sequencer != null && sequencer.getName() != null && !sequencer.getName().isEmpty()
                    ? sequencer.getName()
                    : pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            name = font.plainSubstrByWidth(name, 96);
            graphics.text(font, name, leftPos + 8, topPos + ROW_TOP + slot * ROW_HEIGHT + 3, 0xFFFFFFFF);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF3A3A3A);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF5A4334);
        graphics.fill(leftPos + 4, topPos + ROW_TOP - 6, leftPos + imageWidth - 4, topPos + ROW_TOP - 5, 0xFF3A3A3A);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }
}
