package com.viscriptshop.network.c2s;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.s2c.GetItemCountS2CPayload;
import com.viscriptshop.util.ItemUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record GetItemCountC2SPayload(ShopInfo shopInfo) implements CustomPacketPayload {
    public static final Type<GetItemCountC2SPayload> TYPE = new Type<>(ViscriptShop.id("get_item_count_c2s"));
    public static final StreamCodec<FriendlyByteBuf, GetItemCountC2SPayload> CODEC = StreamCodec.composite(
            ShopInfo.STREAM_CODEC,
            GetItemCountC2SPayload::shopInfo,
            GetItemCountC2SPayload::new
    );


    public static void execute(GetItemCountC2SPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        List<ItemStack> itemStacks = new ArrayList<>();
        payload.shopInfo().getMerchants().forEach(merchantInfo -> {
            ItemStack[] items = {merchantInfo.getItemA(), merchantInfo.getItemB()};
            for (ItemStack stack : items) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                boolean exists = false;
                for (ItemStack existing : itemStacks) {
                    if (ItemStack.isSameItemSameComponents(existing, stack)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) itemStacks.add(stack.copy());
            }
        });
        itemStacks.forEach(stack -> stack.setCount(ItemUtil.getItemForPlayerCount(player, stack)));
        player.connection.send(new GetItemCountS2CPayload(itemStacks));
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
