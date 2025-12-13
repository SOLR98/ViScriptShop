package com.viscriptshop.command;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.ViScriptShopServerUtil;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@LDLRegister(name = "npc", registry = "viscript_shop:command")
public class ShopCommand implements ICommand {
    public static final Set<ResourceLocation> shopFilesPath = new HashSet<>();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal(ViscriptShop.MOD_ID).requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.literal("editor")
                        .executes(this::openEditor)
                )
                .then(Commands.literal("open")
                        .then(Commands.argument("shop", ResourceLocationArgument.id())
                                .suggests(this::shopFileSuggestions)
                                .executes(context -> openShop(context, Component.translatable("viscript_shop.ui.title")))
                                .then(Commands.argument("title", ComponentArgument.textComponent(buildContext))
                                        .executes(context -> openShop(context, ComponentArgument.getComponent(context, "title")))
                                )
                        )
                )
                .then(Commands.literal("reload")
                        .executes(this::reload)
                        .then(Commands.argument("shop", ResourceLocationArgument.id())
                                .suggests(this::shopFileSuggestions)
                                .executes(this::reloadShop)
                        )
                )
                .then(Commands.literal("setStage")
                        .then(Commands.argument("shop", ResourceLocationArgument.id())
                                .suggests(this::shopFileSuggestions)
                                .then(Commands.argument("stage", IntegerArgumentType.integer())
                                        .executes(this::setStageShop)
                                )
                        )
                )
                .then(Commands.literal("money")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("money", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                                    int money = IntegerArgumentType.getInteger(ctx, "money");
                                                    ViScriptShopServerUtil.addMoney(player, money);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.money.add", player.getDisplayName(), money, ViScriptShopServerUtil.getMoney(player)), true);
                                                    return money;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("money", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                                    int money = IntegerArgumentType.getInteger(ctx, "money");
                                                    int removeMoney = ViScriptShopServerUtil.removeMoney(player, money);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.money.remove", player.getDisplayName(), removeMoney, ViScriptShopServerUtil.getMoney(player)), true);
                                                    return removeMoney;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            int money = ViScriptShopServerUtil.getMoney(player);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.money.get", player.getDisplayName(), money), true);
                                            return money;
                                        })
                                )
                        )
                        .then(Commands.literal("pay")
                                .then(Commands.argument("player1", EntityArgument.player())
                                        .then(Commands.argument("player2", EntityArgument.player())
                                                .then(Commands.argument("money", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            ServerPlayer player1 = EntityArgument.getPlayer(ctx, "player1");
                                                            ServerPlayer player2 = EntityArgument.getPlayer(ctx, "player2");
                                                            int money = IntegerArgumentType.getInteger(ctx, "money");
                                                            int removeMoney = ViScriptShopServerUtil.removeMoney(player1, money);
                                                            ViScriptShopServerUtil.addMoney(player2, removeMoney);
                                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.money.pay", player1.getDisplayName(), removeMoney, player2.getDisplayName()), true);
                                                            return removeMoney;
                                                        })
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        shopFilesPath.clear();
        for (String path : ShopHelper.scanShopFiles()) {
            shopFilesPath.add(ViscriptShop.id(path));
            ViScriptShopServerUtil.reloadOpenShop(ViscriptShop.id(path));
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.reload"), true);
        return 1;
    }

    @SneakyThrows
    private int openEditor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            ViScriptShopServerUtil.serverOpenShopEditor(player);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    @SneakyThrows
    private int openShop(CommandContext<CommandSourceStack> context, Component title) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            ResourceLocation shop = ResourceLocationArgument.getId(context, "shop");
            ViScriptShopServerUtil.serverOpenShop(player, shop, title);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    @SneakyThrows
    private int reloadShop(CommandContext<CommandSourceStack> context) {
        ResourceLocation shop = ResourceLocationArgument.getId(context, "shop");
        ViScriptShopServerUtil.reloadOpenShop(shop);
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.reload.shop"), true);
        return 1;
    }

    @SneakyThrows
    private int setStageShop(CommandContext<CommandSourceStack> context) {
        ResourceLocation shop = ResourceLocationArgument.getId(context, "shop");
        int stage = IntegerArgumentType.getInteger(context, "stage");
        ViScriptShopServerUtil.setStageShop(shop, stage);
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.setStage.shop", stage), true);
        return 1;
    }

    private CompletableFuture<Suggestions> shopFileSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        SharedSuggestionProvider.suggestResource(shopFilesPath, builder);
        return builder.buildFuture();
    }
}
