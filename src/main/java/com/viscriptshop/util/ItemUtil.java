package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.compat.BeyondDimensionsHelper;
import com.viscriptshop.compat.SophisticatedBackpacksHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ItemUtil {
    public static final Codec<ItemStack> ITEM_STACK_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> {
                CompoundTag tag = (CompoundTag) dynamic.getValue();
                return ItemStack.parseOptional(Platform.getFrozenRegistry(), tag);
            },
            itemStack -> {
                if (itemStack == null || itemStack.isEmpty()) {
                    return new Dynamic<>(NbtOps.INSTANCE, new CompoundTag());
                }
                return new Dynamic<>(NbtOps.INSTANCE, itemStack.saveOptional(Platform.getFrozenRegistry()));
            }
    );

    public static final StreamCodec<ByteBuf, ItemStack> ITEM_STACK_STREAM_CODEC = ByteBufCodecs.fromCodec(ITEM_STACK_CODEC);

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
            remain = SophisticatedBackpacksHelper.removeItemFromSophisticatedBackpacks(player, itemStack, remain);
        }

        if (ViscriptShop.isBeyondDimensionsLoaded() && remain > 0) {
            long removed = BeyondDimensionsHelper.removeItemFromBeyondDimension(player, itemStack, remain);
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
                for (ItemStack itemStack : SophisticatedBackpacksHelper.getItemsFromInventoryBackpack(player)) {
                    if (ItemStack.isSameItemSameComponents(itemStack, item)) {
                        count += itemStack.getCount();
                    }
                }
            }
            //超越维度
            if (ViscriptShop.isBeyondDimensionsLoaded()) {
                count += BeyondDimensionsHelper.getItemFromBeyondDimension(player, item);
            }
        }
        return count;
    }

    //删除或获取背包里的物品
    public static int removeItem(ServerPlayer player, ItemStack item, int count) {
        return player.getInventory().clearOrCountMatchingItems(itemStack -> ItemStack.isSameItemSameComponents(itemStack, item), count, player.inventoryMenu.getCraftSlots());
    }
}
