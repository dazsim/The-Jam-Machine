package org.atomicworkshop.jammachine.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.atomicworkshop.jammachine.JamMachineMod;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.events.RegistrationEvents;
import org.atomicworkshop.jammachine.gui.SequencerContainer;
import org.atomicworkshop.jammachine.libraries.ContainerTypeLibrary;
import org.atomicworkshop.jammachine.network.SequencerSetPitchAtInterval;
import org.atomicworkshop.jammachine.sequencing.MusicPlayer;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

import java.util.function.BiConsumer;

public class JamMachineFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RegistrationEvents.onRegisterBlocks(direct(BuiltInRegistries.BLOCK));
        RegistrationEvents.onRegisterItems(direct(BuiltInRegistries.ITEM));
        RegistrationEvents.onRegisterTileEntityTypes(direct(BuiltInRegistries.BLOCK_ENTITY_TYPE));
        RegistrationEvents.onRegisterCreativeTabs(direct(BuiltInRegistries.CREATIVE_MODE_TAB));

        ContainerTypeLibrary.sequencer_container = Registry.register(BuiltInRegistries.MENU, Reference.Container.SEQUENCER,
                new ExtendedMenuType<>(SequencerContainer::new, BlockPos.STREAM_CODEC));

        PayloadTypeRegistry.serverboundPlay().register(SequencerSetPitchAtInterval.TYPE, SequencerSetPitchAtInterval.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SequencerSetPitchAtInterval.TYPE, (payload, context) -> payload.handle(context.player()));

        ServerLevelEvents.UNLOAD.register((server, level) -> MusicPlayer.onWorldUnloaded(level));

        JamMachineMod.PLATFORM = new JamMachineMod.Platform() {
            @Override
            public void openSequencer(ServerPlayer player, SequencerTileEntity tileEntity) {
                player.openMenu(new ExtendedMenuProvider<BlockPos>() {
                    @Override
                    public BlockPos getScreenOpeningData(ServerPlayer player) {
                        return tileEntity.getBlockPos();
                    }

                    @Override
                    public Component getDisplayName() {
                        return tileEntity.getDisplayName();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                        return tileEntity.createMenu(id, inventory, player);
                    }
                });
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                JamMachineFabricClient.sendToServer(payload);
            }
        };
    }

    private static <T> BiConsumer<Identifier, T> direct(Registry<T> registry) {
        return (id, value) -> Registry.register(registry, id, value);
    }
}
