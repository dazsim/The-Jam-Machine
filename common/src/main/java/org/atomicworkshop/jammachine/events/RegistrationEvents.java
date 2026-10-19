package org.atomicworkshop.jammachine.events;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.blocks.CableBlock;
import org.atomicworkshop.jammachine.blocks.ControllerBlock;
import org.atomicworkshop.jammachine.blocks.SequencerBlock;
import org.atomicworkshop.jammachine.items.ItemBraidedString;
import org.atomicworkshop.jammachine.items.ItemCable;
import org.atomicworkshop.jammachine.items.ItemPunchCardBlank;
import org.atomicworkshop.jammachine.items.ItemPunchCardWritten;
import org.atomicworkshop.jammachine.libraries.BlockLibrary;
import org.atomicworkshop.jammachine.libraries.ItemLibrary;
import org.atomicworkshop.jammachine.libraries.TileEntityTypeLibrary;
import org.atomicworkshop.jammachine.tiles.CableTileEntity;
import org.atomicworkshop.jammachine.tiles.ControllerTileEntity;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

import java.util.Set;
import java.util.function.BiConsumer;

public class RegistrationEvents {
    public static void onRegisterBlocks(BiConsumer<Identifier, Block> registry) {
        BlockBehaviour.Properties machineProperties = BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Reference.Blocks.SEQUENCER))
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(2.0F)
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false);

        BlockLibrary.sequencer = new SequencerBlock(machineProperties);
        registry.accept(Reference.Blocks.SEQUENCER, BlockLibrary.sequencer);

        BlockLibrary.cable = new CableBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Reference.Blocks.CABLE))
                .mapColor(MapColor.WOOL)
                .sound(SoundType.WOOL)
                .strength(0.2F)
                .noOcclusion()
                .noCollision()
                .pushReaction(PushReaction.DESTROY));
        registry.accept(Reference.Blocks.CABLE, BlockLibrary.cable);

        BlockLibrary.controller = new ControllerBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Reference.Blocks.CONTROLLER))
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(2.0F)
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        registry.accept(Reference.Blocks.CONTROLLER, BlockLibrary.controller);
    }

    private static Item.Properties properties(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }

    public static void onRegisterItems(BiConsumer<Identifier, Item> registry) {
        ItemLibrary.sequencer = new BlockItem(BlockLibrary.sequencer, properties(Reference.Blocks.SEQUENCER).useBlockDescriptionPrefix());
        registry.accept(Reference.Blocks.SEQUENCER, ItemLibrary.sequencer);
        ItemLibrary.punchCardBlank = new ItemPunchCardBlank(properties(Reference.Items.PUNCH_CARD_BLANK));
        registry.accept(Reference.Items.PUNCH_CARD_BLANK, ItemLibrary.punchCardBlank);
        ItemLibrary.punchCardWritten = new ItemPunchCardWritten(properties(Reference.Items.PUNCH_CARD_WRITTEN).stacksTo(1));
        registry.accept(Reference.Items.PUNCH_CARD_WRITTEN, ItemLibrary.punchCardWritten);
        ItemLibrary.braidedString = new ItemBraidedString(properties(Reference.Items.BRAIDED_STRING));
        registry.accept(Reference.Items.BRAIDED_STRING, ItemLibrary.braidedString);
        ItemLibrary.cable = new ItemCable(properties(Reference.Items.CABLE), CableTileEntity.PLAIN);
        registry.accept(Reference.Items.CABLE, ItemLibrary.cable);
        for (final DyeColor colour : DyeColor.values()) {
            final Identifier id = Reference.Items.dyedCable(colour);
            ItemLibrary.dyedCables[colour.getId()] = new ItemCable(properties(id), colour.getId());
            registry.accept(id, ItemLibrary.dyedCables[colour.getId()]);
        }
        ItemLibrary.controller = new BlockItem(BlockLibrary.controller, properties(Reference.Blocks.CONTROLLER).useBlockDescriptionPrefix());
        registry.accept(Reference.Blocks.CONTROLLER, ItemLibrary.controller);
    }

    public static void onRegisterTileEntityTypes(BiConsumer<Identifier, BlockEntityType<?>> registry) {
        TileEntityTypeLibrary.sequencer_tile = new BlockEntityType<>(SequencerTileEntity::new, Set.of(BlockLibrary.sequencer));
        registry.accept(Reference.TileEntities.SEQUENCER, TileEntityTypeLibrary.sequencer_tile);
        TileEntityTypeLibrary.cable_tile = new BlockEntityType<>(CableTileEntity::new, Set.of(BlockLibrary.cable));
        registry.accept(Reference.TileEntities.CABLE, TileEntityTypeLibrary.cable_tile);
        TileEntityTypeLibrary.controller_tile = new BlockEntityType<>(ControllerTileEntity::new, Set.of(BlockLibrary.controller));
        registry.accept(Reference.TileEntities.CONTROLLER, TileEntityTypeLibrary.controller_tile);
    }

    public static void onRegisterCreativeTabs(BiConsumer<Identifier, CreativeModeTab> registry) {
        CreativeModeTab itemGroup = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.jammachine.tab_label"))
                .icon(() -> new ItemStack(ItemLibrary.sequencer))
                .displayItems((parameters, output) -> {
                    output.accept(ItemLibrary.sequencer);
                    output.accept(ItemLibrary.controller);
                    output.accept(ItemLibrary.punchCardBlank);
                    output.accept(ItemLibrary.braidedString);
                    output.accept(ItemLibrary.cable);
                    for (final ItemCable dyed : ItemLibrary.dyedCables) {
                        output.accept(dyed);
                    }
                })
                .build();
        registry.accept(Reference.resource("jammachine"), itemGroup);
    }
}
