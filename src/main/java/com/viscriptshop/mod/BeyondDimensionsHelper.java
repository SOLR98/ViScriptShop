package com.viscriptshop.mod;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class BeyondDimensionsHelper {
    //获取玩家超越维度中的指定物品
    public static int getItemFromBeyondDimension(ServerPlayer player, ItemStack item) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null) {
            UnifiedStorage storage = net.getUnifiedStorage();
            if (storage != null) {
                IStackKey<?> key = new ItemStackKey(item);
                KeyAmount keyAmount = storage.getStackByKey(key);
                return (int) keyAmount.amount();
            }
        }
        return 0;
    }

    //从超越维度中扣除指定物品
    public static long removeItemFromBeyondDimension(ServerPlayer player, ItemStack target, long removeAmount) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null || removeAmount <= 0) return 0;

        var storage = net.getUnifiedStorage();
        if (storage == null) return 0;

        IStackKey<?> key = new ItemStackKey(target);

        KeyAmount extracted = storage.extract(key, removeAmount, false);

        return extracted.amount();
    }
}
