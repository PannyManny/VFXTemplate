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
        LOGGER.info("[VFX] Client initializing...");

        ShaderRegistry.init();
        LOGGER.info("[VFX] ShaderRegistry initialized with {} shaders", ShaderRegistry.getCount());

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            LOGGER.info("[VFX] Client started, loading noise texture...");
            Identifier noiseId = Identifier.of(Shadermaxxing.MOD_ID, "textures/noise/perlin18.png");
            noiseTexP18 = new ResourceTexture(noiseId);
            client.getTextureManager().registerTexture(
                    Identifier.of(Shadermaxxing.MOD_ID, "noise_tex"),
                    noiseTexP18
            );
            LOGGER.info("[VFX] Noise texture registered: {}", noiseId);
        });

        ClientPlayNetworking.registerGlobalReceiver(VFXSyncS2CPacket.ID, (payload, context) -> {
            String shaderName = payload.shaderName();
            var pos = payload.pos();

            LOGGER.info("[VFX] Received VFXSyncS2CPacket: shader='{}' pos={}", shaderName, pos);

            context.client().execute(() -> {
                LOGGER.info("[VFX] Creating shader instance for '{}'", shaderName);
                AbstractEventShader instance = ShaderRegistry.create(shaderName);

                if (instance == null) {
                    LOGGER.warn("[VFX] ShaderRegistry.create returned null for '{}' - is it registered?", shaderName);
                    return;
                }

                LOGGER.info("[VFX] Shader instance created successfully for '{}'", shaderName);

                instance.setNoiseTexture(noiseTexP18);
                instance.setOnExpire(() -> {
                    LOGGER.info("[VFX] Shader '{}' expired and removed from active list", shaderName);
                    activeShaders.remove(instance);
                });

                instance.activate(pos, context.client().world);
                LOGGER.info("[VFX] Shader '{}' activated at pos={}", shaderName, pos);

                activeShaders.add(instance);
                LOGGER.info("[VFX] Active shaders now: {}", activeShaders.size());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(VFXStopS2CPacket.ID, (payload, context) -> {
            String shaderName = payload.shaderName();
            LOGGER.info("[VFX] Received VFXStopS2CPacket: shader='{}'", shaderName);

            context.client().execute(() -> {
                for (AbstractEventShader shader : activeShaders) {
                    if (shader.getShaderName().equals(shaderName)) {
                        LOGGER.info("[VFX] Found and expiring shader '{}'", shaderName);
                        shader.expire();
                        return;
                    }
                }
                LOGGER.warn("[VFX] Stop received but no active shader found for '{}'", shaderName);
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
                LOGGER.info("[VFX] Post-processing PRE fired: pipelineName='{}' checking against '{}'", pipelineName, expected);
                if (pipelineName.equals(expected)) {
                    LOGGER.info("[VFX] Match found! Uploading uniforms for '{}'", shader.getShaderName());
                    shader.uploadUniforms(pipeline);
                }
            }
        });

        LOGGER.info("[VFX] Client initialization complete.");
    }
}
