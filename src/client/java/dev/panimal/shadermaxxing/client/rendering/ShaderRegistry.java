package dev.panimal.shadermaxxing.client.rendering;

import dev.panimal.shadermaxxing.Shadermaxxing;
import dev.panimal.shadermaxxing.registry.VFXRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ShaderRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("shadermaxxing-registry");
    private static final Map<String, Supplier<AbstractEventShader>> FACTORIES = new LinkedHashMap<>();

    public static void register(String name, Supplier<AbstractEventShader> factory) {
        FACTORIES.put(name.toLowerCase(), factory);
        VFXRegistry.register(name);
    }

    public static AbstractEventShader create(String name) {
        Supplier<AbstractEventShader> factory = FACTORIES.get(name.toLowerCase());
        if (factory == null) {
            return null;
        }
        return factory.get();
    }

    public static int getCount() {
        return FACTORIES.size();
    }

    public static void init() {
        register("bluetint", () -> new AbstractEventShader("bluetint", Identifier.of(Shadermaxxing.MOD_ID, "bluetint")));
    }
}
