package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.configurator.SyncAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record GetItemCountS2CPayload(Map<ItemStack, Integer> itemStacks) implements CustomPacketPayload {
    public static final Type<GetItemCountS2CPayload> TYPE = new Type<>(ViscriptShop.id("get_item_count_s2c"));
    public static final StreamCodec<FriendlyByteBuf, GetItemCountS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, SyncAccessor.ITEM_STACK_STREAM_CODEC, ByteBufCodecs.VAR_INT),
            GetItemCountS2CPayload::itemStacks,
            GetItemCountS2CPayload::new
    );


    public static void execute(GetItemCountS2CPayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            payload.itemStacks().forEach(shopUI::setItemCount);
            shopUI.reloadInventoryItem();
            shopUI.reloadShoppingItem();
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
