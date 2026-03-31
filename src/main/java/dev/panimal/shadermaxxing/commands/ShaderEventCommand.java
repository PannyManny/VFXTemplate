package dev.panimal.shadermaxxing.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.panimal.shadermaxxing.network.VFXStopS2CPacket;
import dev.panimal.shadermaxxing.network.VFXSyncS2CPacket;
import dev.panimal.shadermaxxing.registry.VFXRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ShaderEventCommand {

    private ShaderEventCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("vfx")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("shader_name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    VFXRegistry.getNames().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                                .executes(ctx -> {
                                                    String shaderName = StringArgumentType.getString(ctx, "shader_name");
                                                    BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                                                    ServerCommandSource source = ctx.getSource();

                                                    Collection<ServerPlayerEntity> targets =
                                                            Optional.ofNullable(source.getEntity())
                                                                    .filter(ServerPlayerEntity.class::isInstance)
                                                                    .map(e -> ((ServerPlayerEntity) e).getWorld()
                                                                            .getPlayers().stream()
                                                                            .map(p -> (ServerPlayerEntity) p)
                                                                            .collect(Collectors.toList()))
                                                                    .orElseGet(() -> source.getServer()
                                                                            .getPlayerManager()
                                                                            .getPlayerList());

                                                    for (ServerPlayerEntity player : targets) {
                                                        ServerPlayNetworking.send(player, new VFXSyncS2CPacket(shaderName, pos));
                                                    }

                                                    return targets.size();
                                                })
                                        )
                                )
                        )
        );

        dispatcher.register(
                CommandManager.literal("vfxstop")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("shader_name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    VFXRegistry.getNames().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .executes(ctx -> {
                                            String shaderName = StringArgumentType.getString(ctx, "shader_name");
                                            Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");

                                            for (ServerPlayerEntity player : targets) {
                                                ServerPlayNetworking.send(player, new VFXStopS2CPacket(shaderName));
                                            }

                                            return targets.size();
                                        })
                                )
                        )
        );
    }
}