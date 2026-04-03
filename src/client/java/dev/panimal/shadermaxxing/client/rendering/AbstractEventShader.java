package dev.panimal.shadermaxxing.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.panimal.shadermaxxing.Shadermaxxing;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.resource.ResourceLocation;

public class AbstractEventShader implements ClientTickEvents.EndTick {

    private static final Logger LOGGER = LoggerFactory.getLogger("shadermaxxing-shader");
    private static final int LIFETIME_TICKS = 16000;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final String shaderName;

    private final Identifier pipelineId;

    private Vector3f blockPosition = null;
    private RegistryKey<World> dimension = null;
    private AbstractTexture noiseTexP18;
    private int ticks = 0;
    private boolean expired = false;

    private Runnable onExpire;
    private PostPipeline pipeline;

    public AbstractEventShader(String shaderName, Identifier pipelineId) {
        this.shaderName = shaderName;
        this.pipelineId = pipelineId;
        LOGGER.info("[VFX] AbstractEventShader created: name='{}' pipelineId='{}'", shaderName, pipelineId);
    }

    public String getShaderName() {
        return shaderName;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setNoiseTexture(AbstractTexture texture) {
        LOGGER.info("[VFX] setNoiseTexture called for '{}', texture is {}", shaderName, texture == null ? "NULL" : "present");
        this.noiseTexP18 = texture;
    }

    public void setOnExpire(Runnable onExpire) {
        this.onExpire = onExpire;
    }

    public void activate(BlockPos pos, World world) {
        LOGGER.info("[VFX] activate() called for '{}': pos={} world={}", shaderName, pos, world == null ? "NULL" : world.getRegistryKey().getValue());
        this.blockPosition = pos.toCenterPos().toVector3f();
        this.dimension = world.getRegistryKey();
        this.ticks = 0;
        this.pipeline = VeilRenderSystem.renderer().getPostProcessingManager().getPipeline(pipelineId);
        if (this.pipeline != null) {
            VeilRenderSystem.renderer().getPostProcessingManager().runPipeline(this.pipeline);
        }
    }

    private boolean shouldRender() {
        boolean result = blockPosition != null
                && client.world != null
                && client.world.getRegistryKey() == dimension;
        return result;
    }

    public void expire() {
        if (expired) return;
        LOGGER.info("[VFX] expire() called for '{}'", shaderName);
        expired = true;
        blockPosition = null;
        dimension = null;
        pipeline = null;
        if (onExpire != null) onExpire.run();
    }

    @Override
    public void onEndTick(MinecraftClient minecraftClient) {
        if (ticks >= LIFETIME_TICKS
                || minecraftClient.world == null
                || minecraftClient.world.getRegistryKey() != dimension) {
            if (!expired) {
                LOGGER.warn("[VFX] '{}' expiring in onEndTick: ticks={} worldNull={} wrongDim={}",
                        shaderName, ticks,
                        minecraftClient.world == null,
                        minecraftClient.world != null && minecraftClient.world.getRegistryKey() != dimension);
            }
            expire();
            return;
        }
        if (shouldRender()) ticks++;
    }

    public void uploadUniforms(PostPipeline pipeline) {
        LOGGER.info("[VFX] uploadUniforms called for '{}', shouldRender={}", shaderName, shouldRender());

        if (!shouldRender()) {
            LOGGER.warn("[VFX] uploadUniforms skipped for '{}': blockPosition={} world={} dimension={}",
                    shaderName, blockPosition,
                    client.world == null ? "NULL" : "present",
                    dimension);
            return;
        }

        if (noiseTexP18 == null) {
            LOGGER.error("[VFX] NoiseTex is null at render time for '{}'!", shaderName);
            return;
        }

        var bpUniform = pipeline.getUniformSafe("BlockPosition");
        LOGGER.info("[VFX] BlockPosition uniform: {}", bpUniform == null ? "NULL (not found in shader)" : "found");
        if (bpUniform != null) bpUniform.setVector(blockPosition.x(), blockPosition.y(), blockPosition.z());

        Vector3f camPos = client.gameRenderer.getCamera().getPos().toVector3f();
        var cpUniform = pipeline.getUniformSafe("CameraPosition");
        LOGGER.info("[VFX] CameraPosition uniform: {}", cpUniform == null ? "NULL (not found in shader)" : "found");
        if (cpUniform != null) cpUniform.setVector(camPos.x(), camPos.y(), camPos.z());

        var timeUniform = pipeline.getUniformSafe("iTime");
        LOGGER.info("[VFX] iTime uniform: {}", timeUniform == null ? "NULL (not found in shader)" : "found");
        if (timeUniform != null) timeUniform.setFloat(ticks / 20f);

        Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());

        var invUniform = pipeline.getUniformSafe("InverseTransformMatrix");
        LOGGER.info("[VFX] InverseTransformMatrix uniform: {}", invUniform == null ? "NULL (not found in shader)" : "found");
        if (invUniform != null) invUniform.setMatrix(proj.mul(view).invert(new Matrix4f()));

        var mvUniform = pipeline.getUniformSafe("ModelViewMat");
        LOGGER.info("[VFX] ModelViewMat uniform: {}", mvUniform == null ? "NULL (not found in shader)" : "found");
        if (mvUniform != null) mvUniform.setMatrix(view);
    }
}
