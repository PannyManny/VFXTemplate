package dev.panimal.shadermaxxing.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import net.minecraft.client.MinecraftClient;
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
    }

    public String getShaderName() {
        return shaderName;
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
        this.pipeline = VeilRenderSystem.renderer().getPostProcessingManager().getPipeline(pipelineId);
        if (this.pipeline != null) {
            VeilRenderSystem.renderer().getPostProcessingManager().add(pipelineId);
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
        expired = true;
        blockPosition = null;
        dimension = null;
        pipeline = null;
        VeilRenderSystem.renderer().getPostProcessingManager().remove(pipelineId);
        if (onExpire != null) onExpire.run();
    }

    @Override
    public void onEndTick(MinecraftClient minecraftClient) {
        if (ticks >= LIFETIME_TICKS
                || minecraftClient.world == null
                || minecraftClient.world.getRegistryKey() != dimension) {
            if (!expired) {
            }
            expire();
            return;
        }
        if (shouldRender()) ticks++;
    }

    public void uploadUniforms(PostPipeline pipeline) {

        if (noiseTexP18 == null) {
            return;
        }

        var bpUniform = pipeline.getUniformSafe("BlockPosition");
        if (bpUniform != null) bpUniform.setVector(blockPosition.x(), blockPosition.y(), blockPosition.z());

        Vector3f camPos = client.gameRenderer.getCamera().getPos().toVector3f();
        var cpUniform = pipeline.getUniformSafe("CameraPosition");
        if (cpUniform != null) cpUniform.setVector(camPos.x(), camPos.y(), camPos.z());

        var timeUniform = pipeline.getUniformSafe("iTime");
        if (timeUniform != null) timeUniform.setFloat(ticks / 20f);

        Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());

        var invUniform = pipeline.getUniformSafe("InverseTransformMatrix");
        if (invUniform != null) invUniform.setMatrix(proj.mul(view).invert(new Matrix4f()));

        var mvUniform = pipeline.getUniformSafe("ModelViewMat");
        if (mvUniform != null) mvUniform.setMatrix(view);
    }
}
