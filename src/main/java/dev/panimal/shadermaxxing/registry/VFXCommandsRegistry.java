package dev.panimal.shadermaxxing.registry;

import com.mojang.brigadier.CommandDispatcher;
import dev.panimal.shadermaxxing.commands.VFXCommands;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class VFXCommandsRegistry {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        VFXCommands.register(dispatcher);
    }
}