package dev.panimal.shadermaxxing.registry;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class VFXRegistry {
    private static final Set<String> NAMES = new LinkedHashSet<>();

    public static void register(String name) {
        NAMES.add(name.toLowerCase());
    }

    public static Collection<String> getNames() {
        return NAMES;
    }
}