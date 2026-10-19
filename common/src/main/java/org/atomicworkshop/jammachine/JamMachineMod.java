package org.atomicworkshop.jammachine;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JamMachineMod
{
    // Directly reference a logger.
    public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);
    public static Platform PLATFORM;

    // the bits that differ between NeoForge and Fabric
    public interface Platform {
        void openMenu(ServerPlayer player, MenuProvider provider, BlockPos pos);

        void sendToServer(CustomPacketPayload payload);
    }
}
