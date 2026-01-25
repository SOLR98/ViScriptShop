package com.viscriptshop.compat;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@LDLRegister(name = "inventory", registry = IContainerHelper.CONTAINER_HELPER_ID, priority = 99)
public class InventoryHelper implements IContainerHelper {
    @Override
    public int getItemStackCount(ServerPlayer player, ItemStack item) {
        return this.removeItemStackByCount(player, item, 0);
    }

    @Override
    public int removeItemStackByCount(ServerPlayer player, ItemStack item, int count) {
        return player.getInventory().clearOrCountMatchingItems(itemStack -> ItemStack.isSameItemSameComponents(itemStack, item), count, player.inventoryMenu.getCraftSlots());
    }
}
