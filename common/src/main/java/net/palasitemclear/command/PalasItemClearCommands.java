package net.palasitemclear.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.palasitemclear.PalasItemClear;
import net.palasitemclear.config.ConfigValidationException;
import net.palasitemclear.config.ConfigPersistenceException;
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

    public static void register() {
        CommandRegistrationEvent.EVENT.register(PalasItemClearCommands::registerCommands);
    }

    private static void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registry,
            Commands.CommandSelection selection
    ) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(PalasItemClear.MOD_ID)
                .requires(PalasItemClearCommands::hasAdminPermission)
                .executes(context -> sendFeedback(context.getSource(), ClearAdminService.help()))
                .then(Commands.literal("help").executes(context -> sendFeedback(context.getSource(), ClearAdminService.help())))
                .then(Commands.literal("status").executes(context -> runWithController(context.getSource(), controller ->
                        ClearAdminService.status(controller))))
                .then(Commands.literal("next").executes(context -> runWithController(context.getSource(), controller ->
                        ClearAdminService.nextClear(controller))))
                .then(Commands.literal("clear").executes(context -> runWithController(context.getSource(), controller ->
                        ClearAdminService.clearNow(controller))))
                .then(Commands.literal("bin")
                        .executes(context -> runWithController(context.getSource(), controller ->
                                ClearAdminService.openRecoveryBin(controller, context.getSource(), null)))
                        .then(Commands.argument("player", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerClearController controller = ServerClearRegistry.get();
                                    for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                        builder.suggest(player.getGameProfile().getName());
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
                .then(Commands.literal("pause").executes(context -> runWithController(context.getSource(), controller ->
                        ClearAdminService.pause(controller))))
                .then(Commands.literal("resume").executes(context -> runWithController(context.getSource(), controller ->
                        ClearAdminService.resume(controller))))
                .then(Commands.literal("reset").executes(context -> runWithController(context.getSource(), controller ->
                        ClearAdminService.resetSchedule(controller))))
                .then(Commands.literal("reload").executes(context -> runWithController(context.getSource(), controller ->
                        ClearAdminService.reload(controller))))
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

        dispatcher.register(root);
    }

    private static boolean hasAdminPermission(CommandSourceStack source) {
        return source.hasPermission(CommandPermissions.ADMIN_LEVEL);
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
