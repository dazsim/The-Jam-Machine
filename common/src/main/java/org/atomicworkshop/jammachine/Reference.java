package org.atomicworkshop.jammachine;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

import java.util.UUID;

public final class Reference {
    public static final String MOD_ID = "jammachine";
    public static final UUID EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final Identifier CHANNEL_NAME = resource("jam_machine");
    public static Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public static Identifier resource(String resourceName) {
        return Identifier.fromNamespaceAndPath(MOD_ID, resourceName);
    }

    public static final class Blocks {

        public static final Identifier SEQUENCER = resource("sequencer");
        public static final Identifier CABLE = resource("cable");
        public static final Identifier CONTROLLER = resource("controller");

        private Blocks() {}
    }

    public static class TileEntities {
        public static final Identifier SEQUENCER = resource("sequencer_tile");
        public static final Identifier CABLE = resource("cable_tile");
        public static final Identifier CONTROLLER = resource("controller_tile");

        private TileEntities() {}
    }

    public static class Container {
        public static final Identifier SEQUENCER = resource("sequencer_container");
        public static final Identifier CONTROLLER = resource("controller_container");

        private Container() {}
    }

    public static final class Items {

        public static final Identifier PUNCH_CARD_WRITTEN = resource("punchcardwritten");
        public static final Identifier PUNCH_CARD_BLANK = resource("punchcardblank");
        public static final Identifier BRAIDED_STRING = resource("braidedstring");
        public static final Identifier CABLE = resource("cable");

        public static Identifier dyedCable(DyeColor colour) {
            return resource(colour.getName() + "_cable");
        }

        private Items() {}
    }

    public static final class NBT {
        public static final String isPlaying = "isPlaying";
        public static String songId = "songId";
        public static String sequencerId = "sequencerId";
        public static String beatsPerMinute = "bpm";
        public static String currentPatternIndex = "currentPattern";
        public static String pendingPatternIndex = "pendingPattern";
        public static String pattern = "pattern";
        public static String sequence = "sequence";
        public static String hasCard = "hasCard";
        public static String name = "name";
        public static String program = "program";
        public static String isProgramming = "isProgramming";
        public static String trackColour = "trackColour";
        public static String colours = "colours";

        private NBT() { }
    }

    private Reference() {}
}
