package dev.panimal.shadermaxxing.client;

import dev.panimal.shadermaxxing.Shadermaxxing;
import dev.panimal.shadermaxxing.client.rendering.EventShader;
import dev.panimal.shadermaxxing.network.VFXSyncS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShadermaxxingClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("shadermaxxing-client");

    @Override
    public void onInitializeClient() {

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {

            Identifier noiseId = Identifier.of(
                    Shadermaxxing.MOD_ID,
                    "textures/noise/perlin18.png"
            );

            AbstractTexture noiseTexP18 = new ResourceTexture(noiseId);

            client.getTextureManager().registerTexture(
                    Identifier.of(Shadermaxxing.MOD_ID, "noise_tex"),
                    noiseTexP18
            );

            EventShader.INSTANCE.setNoiseTexture(noiseTexP18);
        });


        ClientPlayNetworking.registerGlobalReceiver(VFXSyncS2CPacket.ID, (payload, context) -> {
            var pos = payload.pos();

            context.client().execute(() -> {
                EventShader.INSTANCE.blockPosition = pos.toCenterPos().toVector3f();
                EventShader.INSTANCE.dimension = context.client().world.getRegistryKey();
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(EventShader.INSTANCE);
        org.ladysnake.satin.api.event.PostWorldRenderCallback.EVENT.register(EventShader.INSTANCE);
    }
}
