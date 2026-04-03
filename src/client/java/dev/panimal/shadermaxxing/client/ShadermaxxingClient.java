package dev.panimal.shadermaxxing.client;

import dev.panimal.shadermaxxing.Shadermaxxing;
import dev.panimal.shadermaxxing.client.rendering.AbstractEventShader;
import dev.panimal.shadermaxxing.client.rendering.ShaderRegistry;
import dev.panimal.shadermaxxing.network.VFXStopS2CPacket;
import dev.panimal.shadermaxxing.network.VFXSyncS2CPacket;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.fabric.event.FabricVeilPostProcessingEvent;
import foundry.veil.fabric.event.FabricVeilRenderLevelStageEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ShadermaxxingClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("shadermaxxing-client");

    private static final List<AbstractEventShader> activeShaders = new ArrayList<>();

    private static AbstractTexture noiseTexP18;

    @Override
    public void onInitializeClient() {
        ShaderRegistry.init();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            Identifier noiseId = Identifier.of(Shadermaxxing.MOD_ID, "textures/noise/perlin18.png");
            noiseTexP18 = new ResourceTexture(noiseId);
            client.getTextureManager().registerTexture(
                    Identifier.of(Shadermaxxing.MOD_ID, "noise_tex"),
                    noiseTexP18
            );
        });

        ClientPlayNetworking.registerGlobalReceiver(VFXSyncS2CPacket.ID, (payload, context) -> {
            String shaderName = payload.shaderName();
            var pos = payload.pos();

            context.client().execute(() -> {
                AbstractEventShader instance = ShaderRegistry.create(shaderName);

                if (instance == null) {
                    return;
                }

                instance.setNoiseTexture(noiseTexP18);
                instance.setOnExpire(() -> {
                    activeShaders.remove(instance);
                });

                instance.activate(pos, context.client().world);

                activeShaders.add(instance);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(VFXStopS2CPacket.ID, (payload, context) -> {
            String shaderName = payload.shaderName();

            context.client().execute(() -> {
                for (AbstractEventShader shader : activeShaders) {
                    if (shader.getShaderName().equals(shaderName)) {
                        shader.expire();
                        return;
                    }
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (AbstractEventShader shader : List.copyOf(activeShaders)) {
                shader.onEndTick(client);
            }
        });

        FabricVeilPostProcessingEvent.PRE.register((pipelineName, pipeline, context) -> {
            for (AbstractEventShader shader : List.copyOf(activeShaders)) {
                Identifier expected = Identifier.of(Shadermaxxing.MOD_ID, shader.getShaderName());
                if (pipelineName.equals(expected)) {
                    shader.uploadUniforms(pipeline);
                }
            }
        });
    }
}
