package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.data.ShopSavedData;
import com.viscriptshop.network.s2c.OpenShopUIPayload;
import com.viscriptshop.network.s2c.S2CPayload;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ViScriptShopServerUtil {

    @Info("服务端打开商店编辑器")
    public static void serverOpenShopEditor(ServerPlayer player, String shop) {
        CompoundTag tag = ShopHelper.loadShop(shop);
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_SHOP_EDITOR, tag);
    }

    @Info("服务端打开商店")
    public static void serverOpenShop(ServerPlayer player, String shopLocation, Component title) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        ShopInfo shopInfo = shopSavedData.getShopInfo(shopLocation);
        if (shopInfo == null) {
            CompoundTag shop = ShopHelper.getShop(shopLocation);
            if (shop != null) {
                shopInfo = new ShopInfo();
                shopInfo.deserializeNBT(Platform.getFrozenRegistry(), shop);
                shopSavedData.setShopInfo(shopLocation, shopInfo);
            } else {
                ViscriptShop.LOGGER.error("shop location {} not found", shopLocation);
                return;
            }
        }
        player.connection.send(new OpenShopUIPayload(shopInfo, title.getString()));
    }

    @Info("重置商店信息")
    public static void reloadOpenShop(String shop) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        shopSavedData.resetShopInfo(shop);
    }

    @Info("获取商店的信息")
    public static ShopInfo getShopInfo(String shop) {
        return ViscriptShop.getShopSavedData().getShopInfo(shop);
    }

    @Info("添加商店商品")
    public static void addShopMerchant(String shop, int categoryIndex, MerchantInfo merchantInfo) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        shopSavedData.addShopMerchant(shop, categoryIndex, merchantInfo);
    }

    @Info("设置当前商店的阶段值")
    public static void setStageShop(String shop, int stage) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        ShopInfo shopInfo = shopSavedData.getShopInfo(shop);
        shopInfo.setStage(stage);
        shopSavedData.setShopInfo(shop, shopInfo);
    }

    @Info("获取玩家钱")
    public static int getMoney(ServerPlayer player) {
        return player.getData(ShopRegistries.MONEY).getMoney();
    }

    @Info("设置玩家钱")
    public static void setMoney(ServerPlayer player, int money) {
        ShopRegistries.Money m = new ShopRegistries.Money();
        m.setMoney(money);
        player.setData(ShopRegistries.MONEY, m);
    }

    @Info("给玩家钱")
    public static void addMoney(ServerPlayer player, int money) {
        setMoney(player, getMoney(player) + money);
    }

    @Info("扣除玩家钱")
    public static int removeMoney(ServerPlayer player, int money) {
        int playerMoney = getMoney(player);
        if (money > playerMoney) {
            setMoney(player, 0);
            return playerMoney;
        } else {
            setMoney(player, playerMoney - money);
            return money;
        }
    }
}
