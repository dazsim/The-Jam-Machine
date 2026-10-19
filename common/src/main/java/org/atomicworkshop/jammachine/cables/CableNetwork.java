package org.atomicworkshop.jammachine.cables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.blocks.CableBlock;
import org.atomicworkshop.jammachine.blocks.ControllerBlock;
import org.atomicworkshop.jammachine.blocks.SequencerBlock;
import org.atomicworkshop.jammachine.tiles.CableTileEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

/*
 * Cables join when they touch in the same block space (floor and wall meeting), straight on into
 * the next space, or wrapped around the outside edge of the block they sit on. Dyed cables only join
 * their own colour or undyed cable. Undyed cable is a bundle: it carries all seventeen channels, a
 * dyed cable carries only its own. Power is worked out per channel, so red going into a bundle only
 * comes out on red branches, and anything next to the bundle sees every channel.
 */
public final class CableNetwork {
    private static final int MAX_NODES = 4096;

    public enum Link { NONE, INNER, STRAIGHT, WRAP, DEVICE }

    public record Node(BlockPos pos, Direction face) {}

    public final Set<Node> nodes = new HashSet<>();
    public final Set<BlockPos> devices = new HashSet<>();

    public static int colourAt(BlockGetter level, BlockPos pos, Direction face) {
        final BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof CableTileEntity cable) {
            return cable.getColour(face);
        }
        return CableTileEntity.NONE;
    }

    public static boolean isCable(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof CableBlock;
    }

    //the four directions a cable on this face can run in
    public static Direction[] along(Direction face) {
        final List<Direction> out = new ArrayList<>(4);
        for (final Direction d : Direction.values()) {
            if (d.getAxis() != face.getAxis()) out.add(d);
        }
        return out.toArray(new Direction[0]);
    }

    public static final TagKey<Block> CONNECTS = TagKey.create(Registries.BLOCK, Reference.resource("cable_connects"));

    private static final Map<Class<?>, Boolean> LISTENS = new ConcurrentHashMap<>();

    //there is no flag for "listens to redstone", so go by what redstone blocks have in common
    public static boolean isDevice(BlockState state) {
        final Block block = state.getBlock();
        if (block instanceof CableBlock || block instanceof RedStoneWireBlock || block instanceof LiquidBlock) return false;
        if (block instanceof NoteBlock || block instanceof SequencerBlock || block instanceof ControllerBlock) return true;
        if (state.isSignalSource() || state.is(CONNECTS)) return true;
        if (state.hasProperty(BlockStateProperties.POWERED) || state.hasProperty(BlockStateProperties.TRIGGERED)
                || state.hasProperty(BlockStateProperties.ENABLED) || state.hasProperty(BlockStateProperties.EXTENDED)) return true;
        return LISTENS.computeIfAbsent(block.getClass(), CableNetwork::hearsNeighbours);
    }

    //blocks from any mod that react to their neighbours changing, which is how redstone reaches them
    private static boolean hearsNeighbours(Class<?> type) {
        for (Class<?> c = type; c != null && c != BlockBehaviour.class; c = c.getSuperclass()) {
            try {
                c.getDeclaredMethod("neighborChanged", BlockState.class, Level.class, BlockPos.class, Block.class, Orientation.class, boolean.class);
                return true;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return false;
    }

    //dyes never meet each other, undyed cable joins any colour
    public static boolean joins(int colour, int other) {
        if (colour == CableTileEntity.NONE || other == CableTileEntity.NONE) return false;
        return colour == other || colour == CableTileEntity.PLAIN || other == CableTileEntity.PLAIN;
    }

    public static boolean carries(int colour, int channel) {
        return colour == channel || colour == CableTileEntity.PLAIN;
    }

    public static Link link(BlockGetter level, BlockPos pos, Direction face, int colour, Direction dir) {
        return link(level, pos, face, dir, other -> joins(colour, other));
    }

    private static Link link(BlockGetter level, BlockPos pos, Direction face, Direction dir, IntPredicate accepts) {
        if (accepts.test(colourAt(level, pos, dir))) return Link.INNER;
        final BlockPos next = pos.relative(dir);
        if (accepts.test(colourAt(level, next, face))) return Link.STRAIGHT;
        final BlockState nextState = level.getBlockState(next);
        if (isDevice(nextState)) return Link.DEVICE;
        if (nextState.isAir() || isCable(level, next)) {
            if (accepts.test(colourAt(level, next.relative(face), dir.getOpposite()))) return Link.WRAP;
        }
        return Link.NONE;
    }

    private static Node follow(BlockPos pos, Direction face, Direction dir, Link link) {
        return switch (link) {
            case INNER -> new Node(pos, dir);
            case STRAIGHT -> new Node(pos.relative(dir), face);
            case WRAP -> new Node(pos.relative(dir).relative(face), dir.getOpposite());
            default -> null;
        };
    }

    //everything joined to this cable, whatever it carries
    public static CableNetwork scan(BlockGetter level, Node start) {
        return scan(level, start, -1);
    }

    //only the cables that carry one channel, or everything when channel is -1
    public static CableNetwork scan(BlockGetter level, Node start, int channel) {
        final CableNetwork network = new CableNetwork();
        final int startColour = colourAt(level, start.pos(), start.face());
        if (startColour == CableTileEntity.NONE || (channel >= 0 && !carries(startColour, channel))) return network;

        final ArrayDeque<Node> open = new ArrayDeque<>();
        open.add(start);
        network.nodes.add(start);
        while (!open.isEmpty() && network.nodes.size() < MAX_NODES) {
            final Node node = open.poll();
            final BlockPos support = node.pos().relative(node.face());
            if (isDevice(level.getBlockState(support))) network.devices.add(support);

            final int colour = colourAt(level, node.pos(), node.face());
            final IntPredicate accepts = channel < 0 ? other -> joins(colour, other) : other -> other != CableTileEntity.NONE && carries(other, channel);
            for (final Direction dir : along(node.face())) {
                final Link link = link(level, node.pos(), node.face(), dir, accepts);
                if (link == Link.DEVICE) {
                    network.devices.add(node.pos().relative(dir));
                    continue;
                }
                final Node next = follow(node.pos(), node.face(), dir, link);
                if (next != null && network.nodes.add(next)) {
                    open.add(next);
                }
            }
        }
        return network;
    }

    //cables next to a device that run into it
    public static List<Node> touching(BlockGetter level, BlockPos device) {
        final List<Node> out = new ArrayList<>();
        for (final Direction side : Direction.values()) {
            final BlockPos pos = device.relative(side);
            final BlockEntity te = level.getBlockEntity(pos);
            if (!(te instanceof CableTileEntity cable)) continue;
            for (final Direction face : Direction.values()) {
                if (!cable.hasCable(face)) continue;
                //sitting on the device, or running into its side
                if (face == side.getOpposite() || face.getAxis() != side.getAxis()) {
                    out.add(new Node(pos, face));
                }
            }
        }
        return out;
    }

    //every device reachable from this one on a channel, or on anything when channel is -1
    public static Set<BlockPos> devicesFrom(BlockGetter level, BlockPos device, int channel) {
        final Set<BlockPos> out = new LinkedHashSet<>();
        final Set<Node> seen = new HashSet<>();
        for (final Node node : touching(level, device)) {
            if (seen.contains(node)) continue;
            final CableNetwork network = scan(level, node, channel);
            seen.addAll(network.nodes);
            out.addAll(network.devices);
        }
        out.remove(device);
        return out;
    }

    public static Set<BlockPos> devicesFrom(BlockGetter level, BlockPos device) {
        return devicesFrom(level, device, -1);
    }

    //a signal from outside going into a cable of this colour, never one the cables are giving out
    private boolean findPower(Level level, int channel) {
        return CableBlock.withoutSignal(() -> hasInput(level, channel));
    }

    private boolean hasInput(Level level, int channel) {
        for (final Node node : nodes) {
            //a bundle only takes power in on its own undyed channel
            if (colourAt(level, node.pos(), node.face()) != channel) continue;
            final List<Direction> sides = new ArrayList<>(List.of(along(node.face())));
            sides.add(node.face());
            for (final Direction side : sides) {
                final BlockPos from = node.pos().relative(side);
                final BlockState state = level.getBlockState(from);
                if (state.getBlock() instanceof CableBlock || state.getBlock() instanceof RedStoneWireBlock) continue;
                if (level.getSignal(from, side) > 0) return true;
            }
        }
        return false;
    }

    //recalculate every channel of every network that runs through or next to this space
    public static void refreshAround(Level level, BlockPos centre) {
        if (level.isClientSide()) return;
        final Set<Node> done = new HashSet<>();
        for (final BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
            final BlockEntity te = level.getBlockEntity(pos);
            if (!(te instanceof CableTileEntity cable)) continue;
            for (final Direction face : Direction.values()) {
                final Node start = new Node(pos.immutable(), face);
                if (!cable.hasCable(face) || done.contains(start)) continue;
                final CableNetwork whole = scan(level, start);
                done.addAll(whole.nodes);

                final Set<Integer> channels = new TreeSet<>();
                for (final Node node : whole.nodes) channels.add(colourAt(level, node.pos(), node.face()));
                for (final int channel : channels) {
                    final Set<Node> seen = new HashSet<>();
                    for (final Node node : whole.nodes) {
                        if (seen.contains(node) || !carries(colourAt(level, node.pos(), node.face()), channel)) continue;
                        final CableNetwork part = scan(level, node, channel);
                        seen.addAll(part.nodes);
                        part.applyPower(level, channel, part.findPower(level, channel));
                    }
                }
            }
        }
    }

    private void applyPower(Level level, int channel, boolean isPowered) {
        final Set<BlockPos> changed = new LinkedHashSet<>();
        for (final Node node : nodes) {
            if (level.getBlockEntity(node.pos()) instanceof CableTileEntity cable && cable.setPowered(node.face(), channel, isPowered)) {
                changed.add(node.pos());
                changed.add(node.pos().relative(node.face()));
            }
        }
        for (final BlockPos pos : changed) {
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }
    }

    //a sequencer step: the channel goes on or off on every cable touching the device that carries it
    public static void pulse(Level level, BlockPos device, int channel, boolean on) {
        final Set<Node> seen = new HashSet<>();
        final Set<BlockPos> changed = new LinkedHashSet<>();
        for (final Node start : touching(level, device)) {
            if (seen.contains(start)) continue;
            final CableNetwork network = scan(level, start, channel);
            seen.addAll(network.nodes);
            for (final Node node : network.nodes) {
                if (level.getBlockEntity(node.pos()) instanceof CableTileEntity cable && cable.setPulsed(node.face(), channel, on)) {
                    changed.add(node.pos());
                }
            }
        }
        if (changed.isEmpty()) return;
        CableBlock.pulsing(() -> {
            for (final BlockPos pos : changed) {
                level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
            }
        });
    }
}
