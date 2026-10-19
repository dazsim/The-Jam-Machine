package org.atomicworkshop.jammachine.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.atomicworkshop.jammachine.gui.SequencerScreen;
import org.atomicworkshop.jammachine.libraries.ContainerTypeLibrary;
import org.atomicworkshop.jammachine.libraries.TileEntityTypeLibrary;
import org.atomicworkshop.jammachine.sequencing.MusicPlayer;
import org.atomicworkshop.jammachine.ter.SequencerTER;

public class JamMachineFabricClient implements ClientModInitializer {
    private static ClientLevel lastLevel;

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ContainerTypeLibrary.sequencer_container, SequencerScreen::new);
        BlockEntityRenderers.register(TileEntityTypeLibrary.sequencer_tile, SequencerTER::new);

        LevelRenderEvents.END_EXTRACTION.register(context -> MusicPlayer.onClientTick());
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((minecraft, level) -> {
            if (lastLevel != null) {
                MusicPlayer.onWorldUnloaded(lastLevel);
            }
            lastLevel = level;
        });
    }

    static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
