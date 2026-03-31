package dev.panimal.shadermaxxing.client.rendering;

import dev.panimal.shadermaxxing.Shadermaxxing;
import dev.panimal.shadermaxxing.registry.VFXRegistry;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ShaderRegistry {
    private static final Map<String, Supplier<AbstractEventShader>> FACTORIES = new LinkedHashMap<>();

    public static void register(String name, Supplier<AbstractEventShader> factory) {
        FACTORIES.put(name.toLowerCase(), factory);
        VFXRegistry.register(name);
    }

    public static AbstractEventShader create(String name) {
        Supplier<AbstractEventShader> factory = FACTORIES.get(name.toLowerCase());
        if (factory == null) return null;
        return factory.get();
    }

    public static void init() {
        // add more vfx here
        register("cube",   () -> new AbstractEventShader("cube", Identifier.of(Shadermaxxing.MOD_ID, "shaders/post/cube.json")));
        register("sphere", () -> new AbstractEventShader("sphere", Identifier.of(Shadermaxxing.MOD_ID, "shaders/post/sphere.json")));
    }
}