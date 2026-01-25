package com.viscriptshop.command;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopSavedData;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.ViScriptShopServerUtil;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = "shop", registry = ICommand.COMMAND_ID)
public class ShopCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal(ViscriptShop.MOD_ID).requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.literal("editor")
                        .executes(context -> openEditor(context, ""))
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    getServerShopFiles().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> openEditor(context, StringArgumentType.getString(context, "shop")))
                        )
                )
                .then(Commands.literal("open")
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    getServerShopFiles().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(this::openShop)
                        )
                )
                .then(Commands.literal("reload")
                        .executes(this::reload)
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    getServerShopFiles().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(this::reloadShop)
                        )
                )
                .then(Commands.literal("setStage")
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    getServerShopFiles().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
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
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        shopSavedData.reset();
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.reload"), true);
        return 1;
    }

    @SneakyThrows
    private int openEditor(CommandContext<CommandSourceStack> context, String shop) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            ViScriptShopServerUtil.serverOpenShopEditor(player, shop);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    @SneakyThrows
    private int openShop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            String shop = StringArgumentType.getString(context, "shop");
            ViScriptShopServerUtil.serverOpenShop(player, shop);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    @SneakyThrows
    private int reloadShop(CommandContext<CommandSourceStack> context) {
        String shop = StringArgumentType.getString(context, "shop");
        ViScriptShopServerUtil.reloadOpenShop(shop);
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.reload.shop"), true);
        return 1;
    }

    @SneakyThrows
    private int setStageShop(CommandContext<CommandSourceStack> context) {
        String shop = StringArgumentType.getString(context, "shop");
        int stage = IntegerArgumentType.getInteger(context, "stage");
        ViScriptShopServerUtil.setStageShop(shop, stage);
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.setStage.shop", stage), true);
        return 1;
    }

    public static List<String> getServerShopFiles() {
        List<String> shopFiles = new ArrayList<>();
        var assets = new File(LDLib2.getAssetsDir(), ShopHelper.SHOP_PATH);
        if (assets.exists() && assets.isDirectory()) {
            try (var stream = Files.walk(assets.toPath())) {
                stream.filter(Files::isRegularFile).forEach(file -> {
                    String string = file.toString();
                    if (string.endsWith(Shop.SUFFIX)) {
                        shopFiles.add("\"" + string.replace(assets.getPath(), "").substring(1).replace("\\", "/").replace(Shop.SUFFIX, "") + "\"");
                    }
                });
            } catch (IOException ignored) {
            }
        }
        return shopFiles;
    }
}
