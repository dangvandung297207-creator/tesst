package com.animeki.command;

import com.animeki.ability.AbilityService;
import com.animeki.flight.FlightService;
import com.animeki.ki.KiService;
import com.animeki.registry.ModAttachments;
import com.animeki.transformation.Transformation;
import com.animeki.transformation.TransformationService;
import com.animeki.transformation.Transformations;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The {@code /ki} developer command tree.
 *
 * <pre>
 * /ki set &lt;player&gt; &lt;amount&gt;      - set the current Ki
 * /ki give &lt;player&gt; &lt;amount&gt;     - add Ki
 * /ki transform &lt;player&gt; &lt;id&gt;    - force a transformation (or "none" to cancel)
 * /ki cooldown reset &lt;player&gt;    - clear ability cooldowns
 * /ki flight &lt;player&gt; &lt;on|off&gt;   - toggle flight
 * /ki debug [player]             - print the full Ki / combat state
 * </pre>
 *
 * <p>Everything runs on the server and requires permission level 2.</p>
 */
public final class KiCommand {
    private static final SuggestionProvider<CommandSourceStack> TRANSFORMATION_IDS = (context, builder) -> {
        List<String> ids = new ArrayList<>();
        for (Transformation transformation : Transformations.all()) {
            ids.add(transformation.id());
        }
        ids.add("none");
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    private KiCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("ki")
                .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D, 1_000_000.0D))
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                    KiService.set(target, amount);
                                    return success(context, Component.translatable("commands.animeki.ki.set",
                                            target.getDisplayName(), String.format("%.1f", amount)));
                                }))));

        root.then(Commands.literal("give")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D, 1_000_000.0D))
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                    KiService.give(target, amount);
                                    return success(context, Component.translatable("commands.animeki.ki.give",
                                            String.format("%.1f", amount), target.getDisplayName()));
                                }))));

        root.then(Commands.literal("transform")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TRANSFORMATION_IDS)
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    String id = StringArgumentType.getString(context, "id");
                                    if ("none".equalsIgnoreCase(id) || "off".equalsIgnoreCase(id)) {
                                        TransformationService.cancel(target, false);
                                        return success(context, Component.translatable("commands.animeki.ki.transform.none",
                                                target.getDisplayName()));
                                    }
                                    Transformation transformation = Transformations.byId(id);
                                    if (transformation == null) {
                                        return failure(context, Component.translatable(
                                                "commands.animeki.ki.transform.unknown", id));
                                    }
                                    boolean started = TransformationService.start(target, transformation.id());
                                    return started
                                            ? success(context, Component.translatable("commands.animeki.ki.transform",
                                                    target.getDisplayName(), transformation.id()))
                                            : failure(context, Component.translatable(
                                                    "commands.animeki.ki.transform.refused"));
                                }))));

        root.then(Commands.literal("cooldown")
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    AbilityService.resetCooldowns(target);
                                    ModAttachments.flight(target).setLastDashTick(Long.MIN_VALUE);
                                    AbilityService.setCooldown(target, com.animeki.flight.DashService.COOLDOWN_ID, 0);
                                    ModAttachments.abilities(target).clear();
                                    return success(context, Component.translatable("commands.animeki.ki.cooldown.reset",
                                            target.getDisplayName()));
                                }))));

        root.then(Commands.literal("flight")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    if (enabled) {
                                        if (!FlightService.canFly(target)) {
                                            return failure(context, Component.translatable(
                                                    "commands.animeki.ki.flight.unavailable"));
                                        }
                                        FlightService.setActive(target, true, true);
                                    } else {
                                        FlightService.setActive(target, false, true);
                                    }
                                    return success(context, Component.translatable("commands.animeki.ki.flight",
                                            target.getDisplayName(), enabled ? "on" : "off"));
                                }))));

        root.then(Commands.literal("debug")
                .executes(context -> debug(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> debug(context, EntityArgument.getPlayer(context, "player")))));

        dispatcher.register(root);
    }

    private static int debug(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        StringBuilder builder = new StringBuilder();
        builder.append("[animeki] ").append(player.getScoreboardName()).append('\n');
        builder.append("  ").append(KiService.debug(player)).append('\n');
        builder.append("  flight=").append(ModAttachments.flight(player).isActive())
                .append(" boosting=").append(ModAttachments.flight(player).isBoosting())
                .append(" dashing=").append(ModAttachments.flight(player).isDashing())
                .append(" airDashCharges=").append(ModAttachments.flight(player).airDashCharges()).append('\n');
        builder.append("  comboStep=").append(ModAttachments.combat(player).step())
                .append(" hits=").append(ModAttachments.combat(player).hits())
                .append(" activeAbility=").append(ModAttachments.abilities(player).activeAbilityId())
                .append(" beamQueue=").append(com.animeki.beam.BeamService.beams(player.serverLevel()).size())
                .append(" destructionQueue=")
                .append(com.animeki.destruction.DestructionService.queued(player.serverLevel()));
        context.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static int success(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendSuccess(() -> message, true);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static int failure(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendFailure(message);
        return 0;
    }
}
