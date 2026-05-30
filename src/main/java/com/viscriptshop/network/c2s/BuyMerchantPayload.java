package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.viscriptshop.Config;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopServerEvent;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.MerchantFlagGroup;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.util.ItemUtil;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Map;

public class BuyMerchantPayload {
    public static final String BUY_MERCHANT = C2SPayload.MOD_ID + "buy_merchant";

    @RPCPacket(BUY_MERCHANT)
    public static void buyMerchant(RPCSender sender, String shopLocation, AggregatedResources cost, AggregatedResources gain) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;
        ShopInfo shopInfo = ViScriptShopServerUtil.getShopInfo(shopLocation);

        if (NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, shopInfo, cost, gain)).isCanceled()) return;

        // 检查库存是否充足
        for (var purchaseEntry : gain.getPurchaseEntries()) {
            var categoryInfo = shopInfo.getCategoryInfos().stream()
                    .filter(c -> c.getId().equals(purchaseEntry.getCategoryId()))
                    .findFirst()
                    .orElse(null);
            if (categoryInfo == null) continue;

            var merchantInfo = categoryInfo.getMerchants().stream()
                    .filter(m -> m.getId().equals(purchaseEntry.getMerchantId()))
                    .findFirst()
                    .orElse(null);
            if (merchantInfo == null) continue;

            int stock = ViScriptShopServerUtil.getEffectiveMerchantStock(player, shopLocation, purchaseEntry.getCategoryId(), merchantInfo);
            int buyCount = purchaseEntry.getBuyCount();

            if (!MerchantFlagGroup.canAccess(merchantInfo.getFlagGroupMode(), merchantInfo.getFlagGroups(), ViScriptShopServerUtil.getStageFlags(player))) {
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                        Component.translatable("viscript_shop.message.stage_flags.missing"));
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
                return;
            }

            if (stock >= 0 && buyCount > stock) {
                // 发送错误消息
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                        Component.translatable("viscript_shop.message.shoppingCart.out_of_stock"));
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.UPDATE_OUT_OF_STOCK,
                        purchaseEntry.getCategoryId(), purchaseEntry.getMerchantId(), stock);
                // 库存不足
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
                return;
            }
        }

        int maxShopUiGiveItemsPerPurchase = Config.maxShopUiGiveItemsPerPurchase.get();
        long totalGainItemCount = gain.getTotalItemCount();
        if (maxShopUiGiveItemsPerPurchase >= 0 && totalGainItemCount > maxShopUiGiveItemsPerPurchase) {
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR,
                    Component.translatable("viscript_shop.message.buy.too_many_items", maxShopUiGiveItemsPerPurchase));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }

        // 判断数量是否足够
        Map<ItemStack, Integer> costItems = cost.getItems();
        for (ItemStack itemStack : costItems.keySet()) {
            if (!itemStack.isEmpty() && ItemUtil.getItemForPlayerCount(player, itemStack) < costItems.get(itemStack)) {
                // 物品数量不够
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR, Component.translatable("viscript_shop.message.notEnoughItem", itemStack.getItem().getDescription().getString()));
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
                return;
            }
        }

        if (cost.getTotalMoney() > ViScriptShopServerUtil.getMoney(player)) {
            // 钱不够
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR, Component.translatable("viscript_shop.message.noEnoughMoney", cost.getTotalMoney() - ViScriptShopServerUtil.getMoney(player)));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }

        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.SUCCESS, Component.translatable("viscript_shop.message.buySuccess"));
        NeoForge.EVENT_BUS.post(new ShopServerEvent.BuySuccess(player, shopInfo, cost, gain));

        // 扣减库存
        for (var purchaseEntry : gain.getPurchaseEntries()) {
            var categoryInfo = shopInfo.getCategoryInfos().stream()
                    .filter(c -> c.getId().equals(purchaseEntry.getCategoryId()))
                    .findFirst()
                    .orElse(null);
            if (categoryInfo == null) continue;

            var merchantInfo = categoryInfo.getMerchants().stream()
                    .filter(m -> m.getId().equals(purchaseEntry.getMerchantId()))
                    .findFirst()
                    .orElse(null);
            if (merchantInfo == null) continue;

            ViScriptShopServerUtil.reduceMerchantStock(player, shopLocation, purchaseEntry.getCategoryId(),
                    merchantInfo, purchaseEntry.getBuyCount());
        }

        // 保存数据到文件
        if (!shopLocation.isEmpty()) {
            var shopSavedData = ViscriptShop.getShopSavedData();
            if (shopSavedData != null) {
                shopSavedData.setShopInfo(shopLocation, shopInfo);
            }
        }

        // 删除物品
        for (ItemStack itemStack : costItems.keySet()) {
            ItemUtil.removeItemForPlayer(player, itemStack, costItems.get(itemStack));
        }

        // 扣除钱
        if (cost.getTotalMoney() > 0) ViScriptShopServerUtil.removeMoney(player, cost.getTotalMoney());

        // 给予玩家物品
        gain.getItems().forEach((itemStack, count) -> {
            ItemStack copy = itemStack.copy();
            copy.setCount(count);
            ItemHandlerHelper.giveItemToPlayer(player, copy);
        });

        // 给予玩家钱
        if (gain.getTotalMoney() > 0) ViScriptShopServerUtil.addMoney(player, gain.getTotalMoney());

        // 给予玩家经验
        if (gain.getTotalXp() > 0) player.giveExperiencePoints(gain.getTotalXp());

        // 执行指令
        if (!gain.getCommands().isEmpty()) {
            for (String command : gain.getCommands()) {
                executeCommands(player, command);
            }
        }

        // 重新加载 UI
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.RELOAD_SHOP_UI,
                ViScriptShopServerUtil.getPlayerVisibleShopInfo(player, shopLocation, shopInfo), cost);
    }

    public static void executeCommands(ServerPlayer player, String value) {
        var commands = value.split(";");
        for (var command : commands) {
            command = command.trim();
            if (!command.isBlank()) {
                MinecraftServer server = Platform.getMinecraftServer();
                CommandSourceStack commandSource = player.createCommandSourceStack().withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();
                var dispatcher = server.getCommands().getDispatcher();
                try {
                    dispatcher.execute(dispatcher.parse(command, commandSource));
                } catch (UnsupportedOperationException e) {
                    server.getCommands().performPrefixedCommand(commandSource, command);
                } catch (CommandSyntaxException e) {
                    ViscriptShop.LOGGER.error("Error executing command on server: {}", command, e);
                }
            }
        }
    }
}
