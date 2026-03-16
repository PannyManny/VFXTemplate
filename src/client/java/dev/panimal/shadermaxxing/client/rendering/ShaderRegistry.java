package dev.panimal.shadermaxxing.client.rendering;

import java.util.HashMap;
import java.util.Map;

public class ShaderRegistry {
    private static final Map<String, AbstractEventShader> SHADERS = new HashMap<>();

    public static void register(String name, AbstractEventShader shader) {
        SHADERS.put(name.toLowerCase(), shader);
    }

    public static AbstractEventShader get(String name) {
        return SHADERS.get(name.toLowerCase());
    }

    public static boolean has(String name) {
        return SHADERS.containsKey(name.toLowerCase());
    }

    public static void init() {
        // add more vfx here
        register("cube", CubeShader.INSTANCE);
        register("sphere", SphereShader.INSTANCE);
    }
}