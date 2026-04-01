package dev.panimal.shadermaxxing;

import dev.panimal.shadermaxxing.network.VFXStopS2CPacket;
import dev.panimal.shadermaxxing.network.VFXSyncS2CPacket;
import dev.panimal.shadermaxxing.registry.VFXCommandsRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Shadermaxxing implements ModInitializer {
    public static final String MOD_ID = "shadermaxxing";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(VFXSyncS2CPacket.ID, VFXSyncS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(VFXStopS2CPacket.ID, VFXStopS2CPacket.CODEC);
        CommandRegistrationCallback.EVENT.register(VFXCommandsRegistry::register);
    }
}