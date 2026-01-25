package com.viscriptshop.compat;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@LDLRegister(name = BeyondDimensions.MODID, registry = IContainerHelper.CONTAINER_HELPER_ID, modID = BeyondDimensions.MODID)
public class BeyondDimensionsHelper implements IContainerHelper {
    //获取玩家超越维度中的指定物品
    @Override
    public int getItemStackCount(ServerPlayer player, ItemStack item) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null) {
            UnifiedStorage storage = net.getUnifiedStorage();
            IStackKey<?> key = new ItemStackKey(item);
            KeyAmount keyAmount = storage.getStackByKey(key);
            return (int) keyAmount.amount();
        }
        return 0;
    }

    //从超越维度中扣除指定物品
    @Override
    public int removeItemStackByCount(ServerPlayer player, ItemStack item, int count) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null || count <= 0) return 0;

        var storage = net.getUnifiedStorage();

        IStackKey<?> key = new ItemStackKey(item);

        KeyAmount extracted = storage.extract(key, count, false, false);

        return (int) extracted.amount();
    }
}
