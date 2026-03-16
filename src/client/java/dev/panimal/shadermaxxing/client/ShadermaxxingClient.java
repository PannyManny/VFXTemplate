package dev.panimal.shadermaxxing.client;

import dev.panimal.shadermaxxing.Shadermaxxing;
import dev.panimal.shadermaxxing.client.rendering.AbstractEventShader;
import dev.panimal.shadermaxxing.client.rendering.CubeShader;
import dev.panimal.shadermaxxing.client.rendering.ShaderRegistry;
import dev.panimal.shadermaxxing.client.rendering.SphereShader;
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

        // Initialize shader registry first!
        ShaderRegistry.init();

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

            // Set noise texture for all shaders
            CubeShader.INSTANCE.setNoiseTexture(noiseTexP18);
            SphereShader.INSTANCE.setNoiseTexture(noiseTexP18);
        });

        ClientPlayNetworking.registerGlobalReceiver(VFXSyncS2CPacket.ID, (payload, context) -> {
            String shaderName = payload.shaderName();  // Get shader name from packet
            var pos = payload.pos();

            context.client().execute(() -> {
                // Look up shader by name
                AbstractEventShader shader = ShaderRegistry.get(shaderName);

                if (shader != null) {
                    shader.activate(pos, context.client().world);
                } else {
                    LOGGER.warn("Unknown shader: {}", shaderName);
                }
            });
        });

        // add more vfx here
        ClientTickEvents.END_CLIENT_TICK.register(CubeShader.INSTANCE);
        ClientTickEvents.END_CLIENT_TICK.register(SphereShader.INSTANCE);

        org.ladysnake.satin.api.event.PostWorldRenderCallback.EVENT.register(CubeShader.INSTANCE);
        org.ladysnake.satin.api.event.PostWorldRenderCallback.EVENT.register(SphereShader.INSTANCE);
    }
}