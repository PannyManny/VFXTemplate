package dev.panimal.shadermaxxing.client.rendering;

import dev.panimal.shadermaxxing.Shadermaxxing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.ladysnake.satin.api.event.PostWorldRenderCallback;
import org.ladysnake.satin.api.experimental.ReadableDepthFramebuffer;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import org.ladysnake.satin.api.managed.uniform.Uniform1f;
import org.ladysnake.satin.api.managed.uniform.Uniform3f;
import org.ladysnake.satin.api.managed.uniform.UniformMat4;
import org.ladysnake.satin.api.util.GlMatrices;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AbstractEventShader implements PostWorldRenderCallback, ClientTickEvents.EndTick {

    private static final int LIFETIME_TICKS = 16000;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Matrix4f projectionMatrix = new Matrix4f();

    private final String shaderName;
    private final ManagedShaderEffect shader;
    private final UniformMat4 uniformInverseTransformMatrix;
    private final Uniform3f uniformCameraPosition;
    private final Uniform1f uniformiTime;
    private final Uniform3f uniformBlockPosition;

    private Vector3f blockPosition = null;
    private RegistryKey<World> dimension = null;
    private AbstractTexture noiseTexP18;
    private int ticks = 0;
    private boolean expired = false;

    private Runnable onExpire;

    public AbstractEventShader(String shaderName, Identifier shaderIdentifier) {
        this.shaderName = shaderName;
        this.shader = ShaderEffectManager.getInstance().manage(shaderIdentifier, s -> {
            s.setSamplerUniform("DepthSampler", ((ReadableDepthFramebuffer) client.getFramebuffer()).getStillDepthMap());
        });
        this.uniformInverseTransformMatrix = shader.findUniformMat4("InverseTransformMatrix");
        this.uniformCameraPosition = shader.findUniform3f("CameraPosition");
        this.uniformiTime = shader.findUniform1f("iTime");
        this.uniformBlockPosition = shader.findUniform3f("BlockPosition");
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
        shader.release();
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

    @Override
    public void onWorldRendered(Camera camera, float tickDelta) {
        if (!shouldRender()) return;

        if (noiseTexP18 == null) {
            Shadermaxxing.LOGGER.error("NoiseTex is null at render time!");
            return;
        }

        uniformBlockPosition.set(blockPosition);
        uniformInverseTransformMatrix.set(GlMatrices.getInverseTransformMatrix(projectionMatrix));
        uniformCameraPosition.set(camera.getPos().toVector3f());
        uniformiTime.set((ticks + tickDelta) / 20f);

        shader.setSamplerUniform("noiseTexP18", noiseTexP18);
        shader.render(tickDelta);
    }
}