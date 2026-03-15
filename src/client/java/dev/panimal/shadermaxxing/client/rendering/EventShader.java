package dev.panimal.shadermaxxing.client.rendering;

import dev.panimal.shadermaxxing.Shadermaxxing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class EventShader extends AbstractEventShader {
    public static final EventShader INSTANCE = new EventShader();

    public Vector3f blockPosition = null;
    public RegistryKey<World> dimension = null;

    private EventShader() {}

    @Override
    protected Identifier getIdentifier() {
        return Identifier.of(Shadermaxxing.MOD_ID, "shaders/post/cube.json");
    }

    @Override
    protected boolean shouldRender() {
        var client = MinecraftClient.getInstance();
        return blockPosition != null && client.world != null && client.world.getRegistryKey() == dimension;
    }

    @Override
    public void onEndTick(MinecraftClient minecraftClient) {
        if (ticks >= 1600 || minecraftClient.world == null || minecraftClient.world.getRegistryKey() != dimension) {
            blockPosition = null;
            dimension = null;
        }

        super.onEndTick(minecraftClient);
    }

    @Override
    public void onWorldRendered(Camera camera, float tickDelta) {
        if (shouldRender()) {
            uniformBlockPosition.set(blockPosition);
        }

        super.onWorldRendered(camera, tickDelta);
    }
}