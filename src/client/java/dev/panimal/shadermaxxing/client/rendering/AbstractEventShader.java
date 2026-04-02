package dev.panimal.shadermaxxing.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.panimal.shadermaxxing.Shadermaxxing;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
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

public class AbstractEventShader implements ClientTickEvents.EndTick {

    private static final int LIFETIME_TICKS = 16000;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final String shaderName;

    private final Identifier pipelineId;
    private PostPipeline pipeline;

    private Vector3f blockPosition = null;
    private RegistryKey<World> dimension = null;
    private AbstractTexture noiseTexP18;
    private int ticks = 0;
    private boolean expired = false;

    private Runnable onExpire;

    public AbstractEventShader(String shaderName, Identifier pipelineId) {
        this.shaderName = shaderName;
        this.pipelineId = pipelineId;
    }

    public String getShaderName() {
        return shaderName;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setNoiseTexture(AbstractTexture texture) {
        this.noiseTexP18 = texture;
    }

    public void setOnExpire(Runnable onExpire) {
        this.onExpire = onExpire;
    }

    public void activate(BlockPos pos, World world) {
        this.blockPosition = pos.toCenterPos().toVector3f();
        this.dimension = world.getRegistryKey();
        this.ticks = 0;
        this.pipeline = null;
    }

    private boolean shouldRender() {
        return blockPosition != null
                && client.world != null
                && client.world.getRegistryKey() == dimension;
    }

    public void expire() {
        if (expired) return;
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
            expire();
            return;
        }
        if (shouldRender()) ticks++;
    }

    public void onWorldRendered(Camera camera, float tickDelta) {
        if (!shouldRender()) return;

        if (noiseTexP18 == null) {
            Shadermaxxing.LOGGER.error("NoiseTex is null at render time!");
            return;
        }

        PostProcessingManager postManager = VeilRenderSystem.renderer().getPostProcessingManager();
        if (pipeline == null) {
            pipeline = postManager.getPipeline(pipelineId);
            if (pipeline == null) {
                Shadermaxxing.LOGGER.error("Veil post pipeline not found: {}", pipelineId);
                return;
            }
        }

        pipeline.getUniformSafe("BlockPosition")
                .setVector(blockPosition.x(), blockPosition.y(), blockPosition.z());

        Vector3f camPos = camera.getPos().toVector3f();
        pipeline.getUniformSafe("CameraPosition")
                .setVector(camPos.x(), camPos.y(), camPos.z());

        pipeline.getUniformSafe("iTime")
                .setFloat((ticks + tickDelta) / 20f);

        Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());
        pipeline.getUniformSafe("InverseTransformMatrix").setMatrix(proj.mul(view).invert(new Matrix4f()));
        pipeline.getUniformSafe("ModelViewMat").setMatrix(view);

        postManager.runPipeline(pipeline);
    }
}