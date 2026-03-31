package dev.panimal.shadermaxxing.registry;

import com.mojang.brigadier.CommandDispatcher;
import dev.panimal.shadermaxxing.commands.ShaderEventCommand;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ShaderCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        ShaderEventCommand.register(dispatcher);
    }
}