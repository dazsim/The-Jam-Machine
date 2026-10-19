package org.atomicworkshop.jammachine.ter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.cables.CableNetwork;
import org.atomicworkshop.jammachine.cables.CableNetwork.Link;
import org.atomicworkshop.jammachine.tiles.CableTileEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

//draws each cable as if it were on the floor, turned onto its face
public class CableRenderer implements BlockEntityRenderer<CableTileEntity, CableRenderer.State> {
    private static final SpriteId PLAIN = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Reference.resource("block/cable"));
    private static final SpriteId DYED = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Reference.resource("block/cable_dyed"));
    //floor directions, in the order links are stored
    private static final Direction[] FLOOR = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
    //every piece has the same cross section, so joins line up with no steps
    private static final float LO = 6, HI = 10, H = 1;

    public static class State extends BlockEntityRenderState {
        final int[] colours = new int[6];
        final Link[][] links = new Link[6][4];
        //at a corner both cables could draw the shared corner cube, only the owner does
        final boolean[][] owns = new boolean[6][4];
    }

    public CableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private static Quaternionf rotationFor(Direction face) {
        return switch (face) {
            case DOWN -> new Quaternionf();
            case UP -> Axis.XP.rotationDegrees(180);
            case NORTH -> Axis.XP.rotationDegrees(90);
            case SOUTH -> Axis.XP.rotationDegrees(-90);
            case EAST -> Axis.ZP.rotationDegrees(90);
            case WEST -> Axis.ZP.rotationDegrees(-90);
        };
    }

    private static Direction turned(Direction face, Direction floorDirection) {
        final Vector3f v = rotationFor(face).transform(new Vector3f(floorDirection.getStepX(), floorDirection.getStepY(), floorDirection.getStepZ()));
        return Direction.getApproximateNearest(v.x, v.y, v.z);
    }

    @Override
    public void extractRenderState(CableTileEntity cable, State s, float partialTicks, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(cable, s, partialTicks, camera, breaking);
        final Level level = cable.getLevel();
        for (final Direction face : Direction.values()) {
            final int i = face.get3DDataValue();
            s.colours[i] = cable.getColour(face);
            if (s.colours[i] == CableTileEntity.NONE || level == null) continue;
            for (int d = 0; d < 4; d++) {
                final Direction dir = turned(face, FLOOR[d]);
                final Link link = CableNetwork.link(level, cable.getBlockPos(), face, s.colours[i], dir);
                s.links[i][d] = link;
                final Direction other = link == Link.WRAP ? dir.getOpposite() : dir;
                s.owns[i][d] = face.get3DDataValue() < other.get3DDataValue();
            }
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        final var atlas = Minecraft.getInstance().getAtlasManager();
        final TextureAtlasSprite plain = atlas.get(PLAIN);
        final TextureAtlasSprite dyed = atlas.get(DYED);
        for (final Direction face : Direction.values()) {
            final int i = face.get3DDataValue();
            final int colour = s.colours[i];
            if (colour == CableTileEntity.NONE) continue;
            final TextureAtlasSprite sprite = colour == CableTileEntity.PLAIN ? plain : dyed;
            final int tint = colour == CableTileEntity.PLAIN ? 0xFFFFFFFF : DyeColor.byId(colour).getTextureDiffuseColor() | 0xFF000000;
            final Link[] links = s.links[i].clone();
            final boolean[] owns = s.owns[i].clone();
            final int light = s.lightCoords;

            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(rotationFor(face));
            pose.translate(-0.5, -0.5, -0.5);
            out.submitCustomGeometry(pose, Sheets.cutoutBlockSheet(), (p, vc) -> drawFloorCable(p, vc, sprite, tint, light, links, owns));
            pose.popPose();
        }
    }

    private static void drawFloorCable(PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite sprite, int tint, int light, Link[] links, boolean[] owns) {
        box(pose, vc, sprite, tint, light, LO, 0, LO, HI, H, HI);
        for (int d = 0; d < 4; d++) {
            final Link link = links[d];
            if (link == null || link == Link.NONE) continue;
            //how far past the near edge of the space the arm reaches, negative goes over the edge
            final float end = switch (link) {
                case WRAP -> owns[d] ? -H : 0;
                case INNER -> owns[d] ? 0 : H;
                case DEVICE -> 1;
                default -> 0;
            };
            switch (FLOOR[d]) {
                case NORTH -> box(pose, vc, sprite, tint, light, LO, 0, end, HI, H, LO);
                case SOUTH -> box(pose, vc, sprite, tint, light, LO, 0, HI, HI, H, 16 - end);
                case WEST -> box(pose, vc, sprite, tint, light, end, 0, LO, LO, H, HI);
                case EAST -> box(pose, vc, sprite, tint, light, HI, 0, LO, 16 - end, H, HI);
                default -> {}
            }
            if (link == Link.DEVICE) {
                switch (FLOOR[d]) {
                    case NORTH -> box(pose, vc, sprite, tint, light, LO - 0.5f, 0, 0, HI + 0.5f, H + 0.5f, 1);
                    case SOUTH -> box(pose, vc, sprite, tint, light, LO - 0.5f, 0, 15, HI + 0.5f, H + 0.5f, 16);
                    case WEST -> box(pose, vc, sprite, tint, light, 0, 0, LO - 0.5f, 1, H + 0.5f, HI + 0.5f);
                    case EAST -> box(pose, vc, sprite, tint, light, 15, 0, LO - 0.5f, 16, H + 0.5f, HI + 0.5f);
                    default -> {}
                }
            }
        }
    }

    //a cuboid in sixteenths, every face textured from the matching part of the sprite
    private static void box(PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite sprite, int tint, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        x0 /= 16; y0 /= 16; z0 /= 16; x1 /= 16; y1 /= 16; z1 /= 16;
        quad(pose, vc, sprite, tint, light, Direction.UP, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, z0, x1, z1);
        quad(pose, vc, sprite, tint, light, Direction.DOWN, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, z0, x1, z1);
        quad(pose, vc, sprite, tint, light, Direction.NORTH, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, x0, y0, x1, y1);
        quad(pose, vc, sprite, tint, light, Direction.SOUTH, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y0, x1, y1);
        quad(pose, vc, sprite, tint, light, Direction.WEST, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, z0, y0, z1, y1);
        quad(pose, vc, sprite, tint, light, Direction.EAST, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, z0, y0, z1, y1);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite sprite, int tint, int light, Direction normal,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        final float shade = switch (normal) {
            case DOWN -> 0.5f;
            case UP -> 1f;
            case NORTH, SOUTH -> 0.8f;
            case WEST, EAST -> 0.6f;
        };
        final int colour = shaded(tint, shade);
        final Matrix4f m = pose.pose();
        final Vector3f n = pose.transformNormal(normal.getStepX(), normal.getStepY(), normal.getStepZ(), new Vector3f());
        final float su0 = sprite.getU(clamp(u0)), su1 = sprite.getU(clamp(u1));
        final float sv0 = sprite.getV(clamp(1 - v1)), sv1 = sprite.getV(clamp(1 - v0));
        vertex(vc, m, ax, ay, az, colour, su0, sv0, light, n);
        vertex(vc, m, bx, by, bz, colour, su0, sv1, light, n);
        vertex(vc, m, cx, cy, cz, colour, su1, sv1, light, n);
        vertex(vc, m, dx, dy, dz, colour, su1, sv0, light, n);
    }

    private static float clamp(float f) {
        return Math.max(0, Math.min(1, f));
    }

    private static int shaded(int argb, float shade) {
        final int r = (int) (((argb >> 16) & 0xFF) * shade);
        final int g = (int) (((argb >> 8) & 0xFF) * shade);
        final int b = (int) ((argb & 0xFF) * shade);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, int colour, float u, float v, int light, Vector3f n) {
        final Vector3f p = m.transformPosition(x, y, z, new Vector3f());
        vc.addVertex(p.x(), p.y(), p.z(), colour, u, v, OverlayTexture.NO_OVERLAY, light, n.x(), n.y(), n.z());
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
