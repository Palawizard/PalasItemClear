package net.palasitemclear.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.palasitemclear.PalasItemClear;
import net.palasitemclear.compat.ModernServerSupport;
import net.palasitemclear.config.ConfigPersistenceException;
import net.palasitemclear.config.ConfigValidationException;
import net.palasitemclear.server.ServerClearController;
import net.palasitemclear.server.ServerClearRegistry;

public final class PalasItemClearCommands {
    private static final SimpleCommandExceptionType NOT_RUNNING = new SimpleCommandExceptionType(
            Component.literal(ClearAdminService.NOT_RUNNING)
    );
    private static final SimpleCommandExceptionType NO_PERMISSION = new SimpleCommandExceptionType(
            Component.literal("You do not have permission to run this command.")
    );

    private PalasItemClearCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(PalasItemClear.MOD_ID)
                .requires(PalasItemClearCommands::hasAdminPermission)
                .executes(context -> sendFeedback(context.getSource(), ClearAdminService.help()))
                .then(Commands.literal("help").executes(context ->
                        sendFeedback(context.getSource(), ClearAdminService.help())))
                .then(Commands.literal("status").executes(context -> runWithController(context.getSource(),
                        ClearAdminService::status)))
                .then(Commands.literal("next").executes(context -> runWithController(context.getSource(),
                        ClearAdminService::nextClear)))
                .then(Commands.literal("clear").executes(context -> runWithController(context.getSource(),
                        ClearAdminService::clearNow)))
                .then(Commands.literal("bin")
                        .executes(context -> runWithController(context.getSource(), controller ->
                                ClearAdminService.openRecoveryBin(controller, context.getSource(), null)))
                        .then(Commands.argument("player", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerClearController controller = ServerClearRegistry.get();
                                    for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                        builder.suggest(player.getGameProfile().name());
                                    }
                                    if (controller != null) {
                                        controller.recoveryBin().knownOwnerNames().forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> runWithController(context.getSource(), controller ->
                                        ClearAdminService.openRecoveryBin(
                                                controller,
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player")
                                        )))))
                .then(Commands.literal("pause").executes(context -> runWithController(context.getSource(),
                        ClearAdminService::pause)))
                .then(Commands.literal("resume").executes(context -> runWithController(context.getSource(),
                        ClearAdminService::resume)))
                .then(Commands.literal("reset").executes(context -> runWithController(context.getSource(),
                        ClearAdminService::resetSchedule)))
                .then(Commands.literal("reload").executes(context -> runWithController(context.getSource(),
                        ClearAdminService::reload)))
                .then(Commands.literal("set")
                        .then(Commands.literal("interval")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                        .suggests((context, builder) -> {
                                            builder.suggest(60);
                                            builder.suggest(120);
                                            builder.suggest(300);
                                            builder.suggest(600);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            int seconds = IntegerArgumentType.getInteger(context, "seconds");
                                            return runWithController(context.getSource(), controller -> {
                                                try {
                                                    return ClearAdminService.setInterval(controller, seconds);
                                                } catch (ConfigValidationException | ConfigPersistenceException exception) {
                                                    throw new SimpleCommandExceptionType(
                                                            Component.literal(exception.getMessage())
                                                    ).create();
                                                }
                                            });
                                        }))));

        event.getDispatcher().register(root);
    }

    private static boolean hasAdminPermission(CommandSourceStack source) {
        return ModernServerSupport.hasAdminPermission(source);
    }

    @FunctionalInterface
    private interface ControllerAction {
        String run(ServerClearController controller) throws CommandSyntaxException;
    }

    private static int runWithController(CommandSourceStack source, ControllerAction action)
            throws CommandSyntaxException {
        if (!hasAdminPermission(source)) {
            throw NO_PERMISSION.create();
        }

        ServerClearController controller = ServerClearRegistry.get();
        if (!ClearAdminService.isActive(controller)) {
            throw NOT_RUNNING.create();
        }

        return sendFeedback(source, action.run(controller));
    }

    private static int sendFeedback(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }
}
