package com.viscriptshop.command;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.*;
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
import java.util.concurrent.CompletableFuture;

@LDLRegister(name = "shop", registry = ICommand.COMMAND_ID)
public class ShopCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        var root = Commands.literal(ViscriptShop.MOD_ID).requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("editor")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_OWNERS))
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
                                .then(Commands.argument("categoryId", StringArgumentType.string())
                                        .suggests(ShopCommand::suggestCategories)
                                        .executes(this::openShopWithCategory)
                                        .then(Commands.argument("merchantId", StringArgumentType.string())
                                                .suggests(ShopCommand::suggestMerchants)
                                                .executes(this::openShopWithMerchant)
                                        )
                                )
                        )
                )
                .then(Commands.literal("reload")
                        .executes(this::reload)
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ViscriptShop.getShopSavedData().shopInfoMap.forEach((key, value) -> {
                                        builder.suggest("\"" + key + "\"");
                                    });
                                    return builder.buildFuture();
                                })
                                .executes(this::reloadShop)
                        )
                )
                .then(Commands.literal("setStage")
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ViscriptShop.getShopSavedData().shopInfoMap.forEach((key, value) -> {
                                        builder.suggest("\"" + key + "\"");
                                    });
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("stage", IntegerArgumentType.integer())
                                        .executes(this::setStageShop)
                                )
                        )
                )
                .then(Commands.literal("setStock")
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ViscriptShop.getShopSavedData().shopInfoMap.forEach((key, value) -> {
                                        builder.suggest("\"" + key + "\"");
                                    });
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("categoryId", StringArgumentType.string())
                                        .suggests(ShopCommand::suggestCategories)
                                        .then(Commands.argument("merchantId", StringArgumentType.string())
                                                .suggests(ShopCommand::suggestMerchants)
                                                .then(Commands.argument("stock", IntegerArgumentType.integer())
                                                        .executes(this::setMerchantStock)
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ViscriptShop.getShopSavedData().shopInfoMap.forEach((key, value) -> {
                                        builder.suggest("\"" + key + "\"");
                                    });
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("categoryId", StringArgumentType.string())
                                        .suggests(ShopCommand::suggestCategories)
                                        .then(Commands.argument("merchantId", StringArgumentType.string())
                                                .suggests(ShopCommand::suggestMerchants)
                                                .executes(this::removeMerchant)
                                        )
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
                );

        if (ViscriptShop.isFtbLibraryLoaded()) {
            root.then(Commands.literal("setQuickOpening")
                    .then(Commands.argument("shop", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                ViscriptShop.getShopSavedData().shopInfoMap.forEach((key, value) -> {
                                    builder.suggest("\"" + key + "\"");
                                });
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("quickOpening", BoolArgumentType.bool())
                                    .executes(this::setQuickOpeningShop)
                            )
                    )
            );
        }

        dispatcher.register(root);
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
    private int openShopWithCategory(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            String shop = StringArgumentType.getString(context, "shop");
            String categoryId = StringArgumentType.getString(context, "categoryId");
            ViScriptShopServerUtil.serverOpenShop(player, shop, categoryId, null);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    @SneakyThrows
    private int openShopWithMerchant(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            String shop = StringArgumentType.getString(context, "shop");
            String categoryId = StringArgumentType.getString(context, "categoryId");
            String merchantId = StringArgumentType.getString(context, "merchantId");
            ViScriptShopServerUtil.serverOpenShop(player, shop, categoryId, merchantId);
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

        if (ViScriptShopServerUtil.getShopInfo(shop) == null) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.shop_not_found", shop));
            return 0;
        }

        ViScriptShopServerUtil.setStageShop(shop, stage);
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.setStage.shop", stage), true);
        return 1;
    }

    @SneakyThrows
    private int setQuickOpeningShop(CommandContext<CommandSourceStack> context) {
        String shop = StringArgumentType.getString(context, "shop");
        boolean quickOpening = BoolArgumentType.getBool(context, "quickOpening");

        if (ViScriptShopServerUtil.getShopInfo(shop) == null) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.shop_not_found", shop));
            return 0;
        }

        ViScriptShopServerUtil.setQuickOpening(shop, quickOpening);
        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.setQuickOpening.shop", shop, quickOpening), true);
        return 1;
    }

    @SneakyThrows
    private int setMerchantStock(CommandContext<CommandSourceStack> context) {
        String shop = StringArgumentType.getString(context, "shop");
        String categoryId = StringArgumentType.getString(context, "categoryId");
        String merchantId = StringArgumentType.getString(context, "merchantId");
        int stock = IntegerArgumentType.getInteger(context, "stock");

        boolean success = ViScriptShopServerUtil.setMerchantStock(shop, categoryId, merchantId, stock);

        if (!success) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.shop_not_found", shop));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.setStock.success", merchantId, stock), true);
        return 1;
    }

    @SneakyThrows
    private int removeMerchant(CommandContext<CommandSourceStack> context) {
        String shop = StringArgumentType.getString(context, "shop");
        String categoryId = StringArgumentType.getString(context, "categoryId");
        String merchantId = StringArgumentType.getString(context, "merchantId");

        boolean success = ViScriptShopServerUtil.removeMerchant(shop, categoryId, merchantId);

        if (!success) {
            context.getSource().sendFailure(Component.translatable("command.viscript_shop.error.shop_not_found", shop));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.viscript_shop.remove.success", merchantId), true);
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

    // 补全分类ID
    private static CompletableFuture<Suggestions> suggestCategories(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            String shopId = StringArgumentType.getString(context, "shop");
            ShopInfo shopInfo = ViScriptShopServerUtil.getShopInfo(shopId);
            if (shopInfo != null) {
                for (CategoryInfo category : shopInfo.getCategoryInfos()) {
                    builder.suggest(category.getId());
                }
            }
        } catch (IllegalArgumentException ignored) {
            // shop参数还未填写，不提供建议
        }
        return builder.buildFuture();
    }

    // 补全商品ID
    private static CompletableFuture<Suggestions> suggestMerchants(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            String shopId = StringArgumentType.getString(context, "shop");
            String categoryId = StringArgumentType.getString(context, "categoryId");

            ShopInfo shopInfo = ViScriptShopServerUtil.getShopInfo(shopId);
            if (shopInfo != null) {
                for (CategoryInfo category : shopInfo.getCategoryInfos()) {
                    if (category.getId().equals(categoryId)) {
                        for (MerchantInfo merchant : category.getMerchants()) {
                            builder.suggest(merchant.getId());
                        }
                        break;
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            // 参数还未填写，不提供建议
        }
        return builder.buildFuture();
    }
}
