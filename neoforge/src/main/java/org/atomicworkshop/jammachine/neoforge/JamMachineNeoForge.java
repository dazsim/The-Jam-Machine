package org.atomicworkshop.jammachine.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.atomicworkshop.jammachine.JamMachineMod;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.events.RegistrationEvents;
import org.atomicworkshop.jammachine.gui.ControllerContainer;
import org.atomicworkshop.jammachine.gui.SequencerContainer;
import org.atomicworkshop.jammachine.network.MachineControl;
import org.atomicworkshop.jammachine.libraries.ContainerTypeLibrary;
import org.atomicworkshop.jammachine.network.SequencerSetPitchAtInterval;
import org.atomicworkshop.jammachine.sequencing.MusicPlayer;

@Mod(Reference.MOD_ID)
public class JamMachineNeoForge {
    public JamMachineNeoForge(IEventBus modBus) {
        modBus.addListener(JamMachineNeoForge::onRegister);
        modBus.addListener(JamMachineNeoForge::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> MusicPlayer.onWorldUnloaded(e.getLevel()));

        JamMachineMod.PLATFORM = new JamMachineMod.Platform() {
            @Override
            public void openMenu(ServerPlayer player, MenuProvider provider, BlockPos pos) {
                player.openMenu(provider, pos);
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                JamMachineNeoForgeClient.sendToServer(payload);
            }
        };
    }

    private static void onRegister(RegisterEvent e) {
        e.register(Registries.BLOCK, h -> RegistrationEvents.onRegisterBlocks(h::register));
        e.register(Registries.ITEM, h -> RegistrationEvents.onRegisterItems(h::register));
        e.register(Registries.BLOCK_ENTITY_TYPE, h -> RegistrationEvents.onRegisterTileEntityTypes(h::register));
        e.register(Registries.CREATIVE_MODE_TAB, h -> RegistrationEvents.onRegisterCreativeTabs(h::register));
        e.register(Registries.MENU, h -> {
            ContainerTypeLibrary.sequencer_container = IMenuTypeExtension.create(
                    (id, inventory, buf) -> new SequencerContainer(id, inventory, buf.readBlockPos()));
            h.register(Reference.Container.SEQUENCER, ContainerTypeLibrary.sequencer_container);
            ContainerTypeLibrary.controller_container = IMenuTypeExtension.create(
                    (id, inventory, buf) -> new ControllerContainer(id, inventory, buf.readBlockPos()));
            h.register(Reference.Container.CONTROLLER, ContainerTypeLibrary.controller_container);
        });
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToServer(SequencerSetPitchAtInterval.TYPE, SequencerSetPitchAtInterval.STREAM_CODEC,
                (payload, context) -> payload.handle((ServerPlayer) context.player()))
                .playToServer(MachineControl.TYPE, MachineControl.STREAM_CODEC,
                (payload, context) -> payload.handle((ServerPlayer) context.player()));
    }
}
