package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.serialization.Codec;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.util.CodecUtil;
import com.viscriptshop.util.ItemUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class GetItemCountC2SPayload {
    public static final String GET_ITEM_COUNT = C2SPayload.MOD_ID + "get_item_count";

    @RPCPacket(GET_ITEM_COUNT)
    public static void getItemCount(RPCSender sender, CategoryInfo categoryInfo) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;

        Map<ItemStack, Integer> itemStacks = new HashMap<>();
        categoryInfo.getMerchants().forEach(merchantInfo -> {
            ItemStack[] items = categoryInfo.getShopType().equals(CategoryInfo.ShopType.ITEM_FOR_ITEM)
                    ? new ItemStack[]{merchantInfo.getItemA(), merchantInfo.getItemB()}
                    : new ItemStack[]{merchantInfo.getItemResult()};

            for (ItemStack stack : items) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                boolean exists = false;
                for (ItemStack existing : itemStacks.keySet()) {
                    if (ItemStack.isSameItemSameComponents(existing, stack)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    itemStacks.put(copy, 0);
                }
            }
        });

        itemStacks.forEach((stack, count) -> itemStacks.put(stack, ItemUtil.getItemForPlayerCount(player, stack)));
        CompoundTag tag = CodecUtil.serializeMap(itemStacks, ItemStack.OPTIONAL_CODEC, Codec.INT, Platform.getFrozenRegistry());
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.GET_ITEM_COUNT, tag);
    }
}