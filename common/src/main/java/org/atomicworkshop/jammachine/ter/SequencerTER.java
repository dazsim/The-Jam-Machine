package org.atomicworkshop.jammachine.ter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.atomicworkshop.jammachine.libraries.ItemLibrary;
import org.atomicworkshop.jammachine.sequencing.Pattern;
import org.atomicworkshop.jammachine.sequencing.Sequencer;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

@SuppressWarnings("OverlyComplexMethod")
public class SequencerTER implements BlockEntityRenderer<SequencerTileEntity, SequencerTER.State>
{
    private static final int DISABLED_INACTIVE = 0;
    private static final int ENABLED_INACTIVE = 1;
    private static final int DISABLED_ACTIVE = 2;
    private static final int ENABLED_ACTIVE = 3;
    private static final int DISABLED_INACTIVE_SHARP = 4;
    private static final int ENABLED_INACTIVE_SHARP = 5;
    private static final int DISABLED_ACTIVE_SHARP = 6;
    private static final int ENABLED_ACTIVE_SHARP = 7;
    private static final int PUNCH_CARD = 8;

    private ItemStack[] stacks;

    private final float disabledButtonHeight = 0.015f;
    private static final int LIGHT = LightCoordsUtil.FULL_BRIGHT;

    private final Font font;
    private final ItemModelResolver items;

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[9];
        float facing;
        boolean hasCard;
        int bpm;
        int currentInterval;
        int currentPatternIndex;
        int pendingPatternIndex;
        boolean isProgramming;
        int programLength;
        final boolean[][] pattern = new boolean[16][25];

        State() {
            for (int i = 0; i < items.length; i++) {
                items[i] = new ItemStackRenderState();
            }
        }
    }

    public SequencerTER(BlockEntityRendererProvider.Context context)
    {
        this.font = context.font();
        this.items = context.itemModelResolver();
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(SequencerTileEntity te, State s, float partialTicks, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking)
    {
        BlockEntityRenderer.super.extractRenderState(te, s, partialTicks, camera, breaking);
        final Sequencer sequencer = te.getSequencer();
        if (stacks == null) {
            stacks = new ItemStack[] {
                    new ItemStack(Blocks.LIGHT_GRAY_CONCRETE, 1),
                    new ItemStack(Blocks.BROWN_CONCRETE, 1),
                    new ItemStack(Blocks.WHITE_CONCRETE, 1),
                    new ItemStack(Blocks.YELLOW_CONCRETE, 1),
                    new ItemStack(Blocks.BLACK_CONCRETE, 1),
                    new ItemStack(Blocks.BROWN_CONCRETE, 1),
                    new ItemStack(Blocks.GRAY_CONCRETE, 1),
                    new ItemStack(Blocks.ORANGE_CONCRETE, 1),
                    new ItemStack(ItemLibrary.punchCardBlank, 1)
            };
        }
        for (int i = 0; i < stacks.length; i++) {
            items.updateForTopItem(s.items[i], stacks[i], ItemDisplayContext.FIXED, te.getLevel(), null, i);
        }
        s.facing = te.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
        s.hasCard = te.getHasCard();
        s.bpm = sequencer.getBeatsPerMinute();
        s.currentInterval = sequencer.getCurrentInterval();
        s.currentPatternIndex = sequencer.getCurrentPatternIndex();
        s.pendingPatternIndex = sequencer.getPendingPatternIndex();
        s.isProgramming = sequencer.isProgramming();
        s.programLength = sequencer.getProgramLength();
        final Pattern p = sequencer.getCurrentPattern();
        for (int interval = 0; interval < 16; interval++) {
            System.arraycopy(p.getRawPatternData(interval), 0, s.pattern[interval], 0, 25);
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera)
    {
        pose.pushPose();
        {
            //Adjust Origin for rotations.
            pose.translate(0.5, 0.5, 0.5);
            //Rotate according to facing
            pose.mulPose(Axis.YP.rotationDegrees(-s.facing));
            //Tilt plane
            //TODO: Calculate angle accurately rather than eyeballing it.
            pose.mulPose(Axis.XP.rotationDegrees(23));
            //Return origin
            pose.translate(-0.5, -0.2, -0.5);
            //Scale the UI so that it is made up of roughly 28 squares.
            final float scale = 1 / 28.0f;
            pose.scale(scale, scale, scale);
            //Three squares of space along the top/left hand side, giving roughly 7 blocks of space on the right hand side.
            pose.translate(3, 0, 3.5);

            drawBPM(s, pose, out);

            renderSequence(s, pose, out);
            renderPatternButtons(s, pose, out);
            renderBPMButtons(pose, out, s);
            if (s.hasCard)
            {
                renderCard(s, pose, out);
            }
        }
        pose.popPose();
    }

    private void renderItem(State s, int item, PoseStack pose, SubmitNodeCollector out)
    {
        s.items[item].submit(pose, out, LIGHT, OverlayTexture.NO_OVERLAY, 0);
    }

    private void drawString(PoseStack pose, SubmitNodeCollector out, String text, float x)
    {
        out.submitText(pose, x, 0, FormattedCharSequence.forward(text, Style.EMPTY), false, Font.DisplayMode.NORMAL, LIGHT, 0xFFFFFFFF, 0, 0);
    }

    private void renderPatternButtons(State s, PoseStack pose, SubmitNodeCollector out)
    {
        final int currentPatternIndex = s.currentPatternIndex;
        final int pendingPatternIndex = s.pendingPatternIndex;

        pose.pushPose();
        {
            pose.translate(17.5, 0, 12);
            pose.scale(2, 2, 2);
            for (int patternIndex = 0; patternIndex < 8; patternIndex++)
            {
                final int patternButtonX = patternIndex & 3;
                final int patternButtonY = (patternIndex & 4) >> 2;

                final boolean isEnabled = patternIndex == pendingPatternIndex;
                final boolean isCurrent = patternIndex == currentPatternIndex;

                pose.pushPose();
                {
                    pose.translate(patternButtonX, isEnabled ? 0.0f : disabledButtonHeight, patternButtonY);

                    if (isCurrent)
                    {
                        renderItem(s, ENABLED_ACTIVE, pose, out);
                    } else if (isEnabled)
                    {
                        renderItem(s, ENABLED_INACTIVE, pose, out);
                    } else
                    {
                        renderItem(s, DISABLED_INACTIVE, pose, out);
                    }
                }
                pose.popPose();
            }
        }

        //Render Run Button
        pose.pushPose();
        {
            int itemToRender = s.isProgramming ? ENABLED_ACTIVE : DISABLED_INACTIVE;

            pose.scale(0.5f, 0.5f, 0.5f);
            pose.translate(3, 0, -2.5);
            pose.scale(14, 2, 4);
            renderItem(s, itemToRender, pose, out);
        }
        pose.popPose();

        pose.pushPose();
        {
            final float textScale = 0.05f;
            final String run = "Prog " + s.programLength;

            pose.translate(1.5, 0.5, -1.3);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.scale(textScale, -textScale, textScale);
            drawString(pose, out, run, -font.width(run) / 2.0f);
        }
        pose.popPose();

        pose.popPose();
    }

    private void renderBPMButtons(PoseStack pose, SubmitNodeCollector out, State s)
    {
        //0.7045204265288701,0.21970650094097977
        //0.9327918869342732,0.2705630830397361
        final float textScale = 0.05f;

        pose.pushPose();
        pose.translate(17.8, 0, 5.3);
        pose.scale(2.7f, 2.0f, 2.7f);

        final String[] labels = { "<<", "<", ">", ">>" };
        for (int i = 0; i < labels.length; i++)
        {
            if (i > 0) {
                pose.translate(0.6, 0, 0);
            }
            renderItem(s, ENABLED_INACTIVE, pose, out);

            pose.pushPose();
            pose.translate(0, 0.3, -0.2);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.scale(textScale, -textScale, textScale);
            drawString(pose, out, labels[i], -font.width(labels[i]) / 2);
            pose.popPose();
        }

        pose.popPose();
    }

    private void renderSequence(State s, PoseStack pose, SubmitNodeCollector out)
    {
        pose.pushPose(); // Matrix for pattern data

        for (int interval=0;interval<16;interval++)
        {
            final boolean[] rawPatternData = s.pattern[interval];

            //outer loop. reset after every outer loop.
            for (int pitch=0;pitch<25;pitch++)
            {
                final boolean isEnabled = rawPatternData[pitch];

                final int renderItem = getSequenceButtonColour(s.currentInterval, interval, pitch, isEnabled);

                pose.pushPose(); // Matrix for an individual button on the pattern sequence data

                pose.translate(
                        interval,
                        isEnabled ? 0.0f : disabledButtonHeight,
                        (25 - pitch)
                );

                renderItem(s, renderItem, pose, out);

                pose.popPose(); // Matrix for an individual button on the pattern sequence data
            }
        }

        pose.popPose(); // Matrix for pattern data
    }

    private int getSequenceButtonColour(int currentInterval, int interval, int pitch, boolean isEnabled)
    {
        if (isSharpPitch(pitch)) {
            //Use sharp colours.
            if (currentInterval == interval)
            {
                return isEnabled ? ENABLED_ACTIVE_SHARP : DISABLED_ACTIVE_SHARP;
            } else {
                return isEnabled ? ENABLED_INACTIVE_SHARP : DISABLED_INACTIVE_SHARP;
            }
        } else {
            //Use non-sharp colours.
            if (currentInterval == interval) {
                return isEnabled ? ENABLED_ACTIVE : DISABLED_ACTIVE;
            } else {
                return isEnabled ? ENABLED_INACTIVE : DISABLED_INACTIVE;
            }
        }
    }

    private void renderCard(State s, PoseStack pose, SubmitNodeCollector out) {
        final float cardScale = 5.0f;

        pose.pushPose();
        pose.translate(20.25, 0, 25);
        pose.scale(cardScale, cardScale, cardScale);

        renderItem(s, PUNCH_CARD, pose, out);

        pose.popPose();
    }

    private void drawBPM(State s, PoseStack pose, SubmitNodeCollector out) {
        final float textScale = 0.30f;

        pose.pushPose();
        pose.translate(20, 0.09, 1.5);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.scale(textScale, -textScale, textScale);

        final String bpmText = String.valueOf(s.bpm);
        drawString(pose, out, bpmText, -font.width(bpmText) / 2);

        pose.popPose();
    }

    @SuppressWarnings("OverlyComplexBooleanExpression")
    private static boolean isSharpPitch(int pitch)
    {
        return pitch == 0 || pitch == 2 || pitch == 4 || pitch == 7 || pitch == 9 || pitch == 12 ||
                pitch == 14 || pitch == 16 || pitch == 19 || pitch == 21 || pitch == 24;
    }
}
