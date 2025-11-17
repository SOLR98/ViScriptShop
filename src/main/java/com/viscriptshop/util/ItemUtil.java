package com.viscriptshop.util;

import com.viscriptshop.ViscriptShop;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ItemUtil {
    //删除玩家物品，兼容背包，精妙背包，超越维度
    public static void removeItemForPlayer(ServerPlayer player, ItemStack itemStack, int count) {
        //TODO:删除付出的物品，用于联动库存模组
        int remain = count;
        //从玩家背包里扣除物品
        if (remain > 0) {
            int removed = ItemUtil.removeItem(player, itemStack, remain);
            remain -= removed;
        }

        if (ViscriptShop.isSophisticatedBackpacksLoaded() && remain > 0) {
            remain = removeItemFromSophisticatedBackpacks(player, itemStack, remain);
        }

        if (ViscriptShop.isBeyondDimensionsLoaded() && remain > 0) {
            long removed = removeItemFromBeyondDimension(player, itemStack, remain);
            remain -= (int) removed;
        }
    }

    //获取玩家物品，兼容背包，精妙背包，超越维度
    public static int getItemForPlayerCount(ServerPlayer player, ItemStack item) {
        int count = 0;
        if (player != null) {
            //TODO:修改玩家拥有的物品数量，用于联动库存模组
            //背包该物品数量
            count += ItemUtil.removeItem(player, item, 0);
            //精妙背包
            if (ViscriptShop.isSophisticatedBackpacksLoaded()) {
                for (ItemStack itemStack : ItemUtil.getItemsFromInventoryBackpack(player)) {
                    if (ItemStack.isSameItemSameComponents(itemStack, item)) {
                        count += itemStack.getCount();
                    }
                }
            }
            //超越维度
            if (ViscriptShop.isBeyondDimensionsLoaded()) {
                DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                if (net != null) {
                    UnifiedStorage storage = net.getUnifiedStorage();
                    if (storage != null) {
                        IStackKey<?> key = new ItemStackKey(item);
                        KeyAmount keyAmount = storage.getStackByKey(key);
                        count += (int) keyAmount.amount();
                    }
                }
            }
        }
        return count;
    }

    //删除或获取背包里的物品
    public static int removeItem(ServerPlayer player, ItemStack item, int count) {
        return player.getInventory().clearOrCountMatchingItems(itemStack -> ItemStack.isSameItemSameComponents(itemStack, item), count, player.inventoryMenu.getCraftSlots());
    }

    //从精妙背包中扣除指定物品
    public static int removeItemFromSophisticatedBackpacks(ServerPlayer player, ItemStack target, int needToRemove) {
        if (needToRemove <= 0) return 0;

        final int[] remain = {needToRemove};
        for (ItemStack backpackItem : getAllInventoryBackpack(player)) {
            modifyInventoryBackpack(player, backpackItem, (inventoryHandler) -> {
                for (int i = 0; i < inventoryHandler.getSlots(); i++) {
                    if (remain[0] <= 0) break;
                    ItemStack stackInSlot = inventoryHandler.getStackInSlot(i);
                    if (ItemStack.isSameItemSameComponents(stackInSlot, target)) {
                        int canRemove = Math.min(stackInSlot.getCount(), remain[0]);
                        ItemStack removed = inventoryHandler.extractItem(i, canRemove, false);
                        remain[0] -= removed.getCount();
                    }
                }
            });
        }

        return remain[0];
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


    //获取玩家所有背包中所有的物品，不包括玩家物品栏
    public static List<ItemStack> getItemsFromInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();
        getAllInventoryBackpack(player).forEach(itemStack -> {
            items.addAll(getItemsFromBackpackItem(itemStack));
        });
        return items;
    }


    //获取玩家背包中所有的背包
    public static List<ItemStack> getAllInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            items.add(backpack);
            return false;
        });
        return items;
    }

    //获取背包中所有的物品
    public static List<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        List<ItemStack> items = new ArrayList<>();
        BackpackWrapper backpackWrapper = new BackpackWrapper(itemStack);
        InventoryHandler handler = backpackWrapper.getInventoryHandler();
        Integer size = itemStack.get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS);
        if (size == null) return items;
        for (int i = 0; i < size; i++) {
            ItemStack item = handler.getStackInSlot(i);
            items.add(item);
        }
        return items;
    }

    public static void modifyInventoryBackpack(ServerPlayer player, ItemStack backpackItem, Consumer<IItemHandler> action) {
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            if (!backpack.equals(backpackItem)) return false;
            BackpackContext.Item backpackContext = new BackpackContext.Item(inventoryName, identifier, index);
            modifyBackpack(player, backpackContext, action);
            return false;
        });
    }

    public static void modifyBackpack(ServerPlayer player, BackpackContext backpackContext, Consumer<IItemHandler> action) {
        BackpackContainer container = new BackpackContainer(player.containerMenu.containerId + 1, player, backpackContext);
        int size = container.realInventorySlots.size() - player.getInventory().items.size();
        InventoryHandler inventoryHandler = container.getStorageWrapper().getInventoryHandler();
        action.accept(inventoryHandler);
        for (int i = 0; i < size; i++) {
            container.realInventorySlots.get(i).set(inventoryHandler.getStackInSlot(i));
        }
        UUID uuid = container.getStorageWrapper().getContentsUuid().get();
        CompoundTag backpackContent = BackpackStorage.get().getOrCreateBackpackContents(uuid);
        player.connection.send(new BackpackContentsPayload(uuid, backpackContent));
    }

}
