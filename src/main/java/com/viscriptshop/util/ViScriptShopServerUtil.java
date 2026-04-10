package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.Config;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.data.ShopSavedData;
import com.viscriptshop.network.s2c.S2CPayload;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerPlayer;
import net.sirgrantd.sg_economy.api.SGEconomyApi;

public class ViScriptShopServerUtil {

    @Info("服务端打开商店编辑器")
    public static void serverOpenShopEditor(ServerPlayer player, String shop) {
        ShopInfo shopInfo = ShopHelper.getShop(shop);
        PlayerUIMenuType.openUI(player, ShopEditor.SHOP_ID);
        if (shopInfo != null) RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SHOP_INFO_PROJECT, shopInfo);
    }

    @Info("服务端打开商店")
    public static void serverOpenShop(ServerPlayer player, String shopLocation) {
        serverOpenShop(player, shopLocation, null, null);
    }

    @Info("服务端打开商店（带分类和商品参数）")
    public static void serverOpenShop(ServerPlayer player, String shopLocation, String categoryId, String merchantId) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        ShopInfo shopInfo = shopSavedData.getShopInfo(shopLocation);
        if (shopInfo == null) {
            shopInfo = ShopHelper.getShop(shopLocation);
            if (shopInfo != null) {
                shopSavedData.setShopInfo(shopLocation, shopInfo);
            } else {
                ViscriptShop.LOGGER.error("shop location {} not found", shopLocation);
                return;
            }
        }
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_SHOP_UI, shopLocation, shopInfo,
                categoryId != null ? categoryId : "",
                merchantId != null ? merchantId : "");
    }

    @Info("重置商店信息")
    public static void reloadOpenShop(String shop) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        shopSavedData.resetShopInfo(shop);
    }

    @Info("获取商店的信息，先从savedData里获取，如果不存在再从服务端文件中获取")
    public static ShopInfo getShopInfo(String shop) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        ShopInfo shopInfo = shopSavedData.getShopInfo(shop);
        if (shopInfo == null) shopInfo = ShopHelper.getShop(shop);
        return shopInfo;
    }

    @Info("设置商品信息")
    public static void setShopInfo(String shop, ShopInfo shopInfo) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        shopSavedData.setShopInfo(shop, shopInfo);
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

    @Info("设置商店商品库存")
    public static boolean setMerchantStock(String shopLocation, String categoryId, String merchantId, int stock) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        ShopInfo shopInfo = shopSavedData.getShopInfo(shopLocation);

        if (shopInfo == null) {
            shopInfo = ShopHelper.getShop(shopLocation);
            if (shopInfo != null) {
                shopSavedData.setShopInfo(shopLocation, shopInfo);
            }
        }

        if (shopInfo == null) {
            return false;
        }

        // 查找并设置库存
        for (var category : shopInfo.getCategoryInfos()) {
            if (category.getId().equals(categoryId)) {
                for (var merchant : category.getMerchants()) {
                    if (merchant.getId().equals(merchantId)) {
                        merchant.setStock(stock);
                        // 保存到文件和内存
                        shopSavedData.setShopInfo(shopLocation, shopInfo);
                        ShopHelper.clearCache();
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    @Info("删除商店商品")
    public static boolean removeMerchant(String shopLocation, String categoryId, String merchantId) {
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        ShopInfo shopInfo = shopSavedData.getShopInfo(shopLocation);

        if (shopInfo == null) {
            shopInfo = ShopHelper.getShop(shopLocation);
            if (shopInfo != null) {
                shopSavedData.setShopInfo(shopLocation, shopInfo);
            }
        }

        if (shopInfo == null) {
            return false;
        }

        // 查找并删除商品
        for (var category : shopInfo.getCategoryInfos()) {
            if (category.getId().equals(categoryId)) {
                boolean removed = category.getMerchants().removeIf(merchant -> merchant.getId().equals(merchantId));
                if (removed) {
                    // 保存到文件和内存
                    shopSavedData.setShopInfo(shopLocation, shopInfo);
                    ShopHelper.clearCache();
                    return true;
                }
                break;
            }
        }
        return false;
    }

    @Info("获取玩家钱")
    public static int getMoney(ServerPlayer player) {
        if (ViscriptShop.isMagicCoinsLoaded() && Config.isReplaceMoneyToMagicCoin.get()) {
            return SGEconomyApi.get().getBalanceAsInt(player);
        }
        return player.getData(ShopRegistries.MONEY).getMoney();
    }

    @Info("设置玩家钱")
    public static void setMoney(ServerPlayer player, int money) {
        if (ViscriptShop.isMagicCoinsLoaded() && Config.isReplaceMoneyToMagicCoin.get()) {
            SGEconomyApi.get().setBalanceAsInt(player, money);
        }
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
