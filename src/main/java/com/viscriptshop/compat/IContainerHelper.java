package com.viscriptshop.compat;

import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public interface IContainerHelper extends ILDLRegister<IContainerHelper, Supplier<IContainerHelper>> {
    String CONTAINER_HELPER_ID = "viscript_shop:container_helper";

    int getItemStackCount(ServerPlayer player, ItemStack item);

    int removeItemStackByCount(ServerPlayer player, ItemStack item, int count);
}
