package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.configurator.SyncAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ReloadShopUIPayload(ItemStack itemA, ItemStack itemB) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReloadShopUIPayload> TYPE = new CustomPacketPayload.Type<>(ViscriptShop.id("reload_shop_ui"));
    public static final StreamCodec<FriendlyByteBuf, ReloadShopUIPayload> CODEC = StreamCodec.composite(
            SyncAccessor.ITEM_STACK_STREAM_CODEC,
            ReloadShopUIPayload::itemA,
            SyncAccessor.ITEM_STACK_STREAM_CODEC,
            ReloadShopUIPayload::itemB,
            ReloadShopUIPayload::new
    );


    public static void execute(ReloadShopUIPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            shopUI.removeItemCount(payload.itemA);
            shopUI.removeItemCount(payload.itemB);
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
