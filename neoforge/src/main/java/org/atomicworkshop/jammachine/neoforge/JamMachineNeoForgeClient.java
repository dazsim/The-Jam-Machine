package org.atomicworkshop.jammachine.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.gui.SequencerScreen;
import org.atomicworkshop.jammachine.libraries.ContainerTypeLibrary;
import org.atomicworkshop.jammachine.libraries.TileEntityTypeLibrary;
import org.atomicworkshop.jammachine.sequencing.MusicPlayer;
import org.atomicworkshop.jammachine.ter.SequencerTER;

@Mod(value = Reference.MOD_ID, dist = Dist.CLIENT)
public class JamMachineNeoForgeClient {
    public JamMachineNeoForgeClient(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ContainerTypeLibrary.sequencer_container, SequencerScreen::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerBlockEntityRenderer(TileEntityTypeLibrary.sequencer_tile, SequencerTER::new));
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre e) -> MusicPlayer.onClientTick());
    }

    static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
