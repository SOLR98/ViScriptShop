package com.viscriptshop.network.c2s;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.network.s2c.GetItemCountS2CPayload;
import com.viscriptshop.util.ItemUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record GetItemCountC2SPayload(CategoryInfo categoryInfo) implements CustomPacketPayload {
    public static final Type<GetItemCountC2SPayload> TYPE = new Type<>(ViscriptShop.id("get_item_count_c2s"));
    public static final StreamCodec<FriendlyByteBuf, GetItemCountC2SPayload> CODEC = StreamCodec.composite(
            CategoryInfo.STREAM_CODEC,
            GetItemCountC2SPayload::categoryInfo,
            GetItemCountC2SPayload::new
    );


    public static void execute(GetItemCountC2SPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        Map<ItemStack, Integer> itemStacks = new HashMap<>();
        payload.categoryInfo().getMerchants().forEach(merchantInfo -> {
            ItemStack[] items = payload.categoryInfo().getShopType().equals(CategoryInfo.ShopType.ITEM_FOR_ITEM) ? new ItemStack[]{merchantInfo.getItemA(), merchantInfo.getItemB()} : new ItemStack[]{merchantInfo.getItemResult()};
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
        player.connection.send(new GetItemCountS2CPayload(itemStacks));
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
