package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.util.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record ReloadShopUIPayload(Map<ItemStack, Integer> costItems) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReloadShopUIPayload> TYPE = new CustomPacketPayload.Type<>(ViscriptShop.id("reload_shop_ui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadShopUIPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ItemUtil.ITEM_STACK_STREAM_CODEC, ByteBufCodecs.VAR_INT),
            ReloadShopUIPayload::costItems,
            ReloadShopUIPayload::new
    );


    public static void execute(ReloadShopUIPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            payload.costItems.forEach(shopUI::removeItemCount);
            shopUI.currentShopInfo.getCategoryInfos().forEach(categoryInfo -> {
                categoryInfo.getMerchants().forEach(merchantInfo -> merchantInfo.setBuyCount(0));
            });
            shopUI.reloadInventoryItem();
            shopUI.reloadShoppingItem();
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
