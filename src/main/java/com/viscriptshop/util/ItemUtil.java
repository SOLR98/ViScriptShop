package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptshop.ViScriptShopRegistries;
import com.viscriptshop.compat.IContainerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ItemUtil {
    //删除玩家物品，兼容背包，精妙背包，超越维度
    public static void removeItemForPlayer(ServerPlayer player, ItemStack itemStack, int count) {
        int remain = count;
        for (AutoRegistry.Holder<LDLRegister, IContainerHelper, Supplier<IContainerHelper>> containerHelperSupplierHolder : ViScriptShopRegistries.ContainerHelper) {
            IContainerHelper iContainerHelper = containerHelperSupplierHolder.value().get();
            if (remain > 0) {
                int removed = iContainerHelper.removeItemStackByCount(player, itemStack, remain);
                remain -= removed;
            }

        }
    }

    //获取玩家物品，兼容背包，精妙背包，超越维度
    public static int getItemForPlayerCount(ServerPlayer player, ItemStack item) {
        int count = 0;
        if (player != null) {
            for (AutoRegistry.Holder<LDLRegister, IContainerHelper, Supplier<IContainerHelper>> containerHelperSupplierHolder : ViScriptShopRegistries.ContainerHelper) {
                IContainerHelper iContainerHelper = containerHelperSupplierHolder.value().get();
                count += iContainerHelper.getItemStackCount(player, item);
            }
        }
        return count;
    }
}
