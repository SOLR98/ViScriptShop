package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopServerEvent;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.s2c.ReloadShopUIPayload;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.util.ItemUtil;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record BuyMerchantPayload(ShopInfo shopInfo, AggregatedResources cost,
                                 AggregatedResources gain) implements CustomPacketPayload {
    public static final Type<BuyMerchantPayload> TYPE = new Type<>(ViscriptShop.id("buy_merchant"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuyMerchantPayload> CODEC = StreamCodec.composite(
            ShopInfo.STREAM_CODEC,
            BuyMerchantPayload::shopInfo,
            AggregatedResources.STREAM_CODEC,
            BuyMerchantPayload::cost,
            AggregatedResources.STREAM_CODEC,
            BuyMerchantPayload::gain,
            BuyMerchantPayload::new
    );


    public static void execute(BuyMerchantPayload payload, IPayloadContext context) {
        ShopInfo shopInfo = payload.shopInfo();
        AggregatedResources cost = payload.cost();
        AggregatedResources gain = payload.gain();
        ServerPlayer player = (ServerPlayer) context.player();
        if (NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, shopInfo, cost, gain)).isCanceled()) return;
        //判断数量是否足够
        Map<ItemStack, Integer> costItems = cost.getItems();
        for (ItemStack itemStack : costItems.keySet()) {
            if (!itemStack.isEmpty() && ItemUtil.getItemForPlayerCount(player, itemStack) < costItems.get(itemStack)) {
                //物品数量不够
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR, Component.translatable("viscript_shop.message.notEnoughItem", itemStack.getItem().getDescription().getString()).getString());
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, shopInfo, cost, gain));
                return;
            }
        }
        if (cost.getTotalMoney() > player.getData(ShopRegistries.MONEY).getMoney()) {
            //钱不够
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR, Component.translatable("viscript_shop.message.noEnoughMoney", cost.getTotalMoney() - player.getData(ShopRegistries.MONEY).getMoney()).getString());
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, shopInfo, cost, gain));
            return;
        }
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.SUCCESS, Component.translatable("viscript_shop.message.buySuccess").getString());
        NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, shopInfo, cost, gain));
        //删除物品
        for (ItemStack itemStack : costItems.keySet()) {
            ItemUtil.removeItemForPlayer(player, itemStack, costItems.get(itemStack));
        }
        //扣除钱
        if (cost.getTotalMoney() > 0) ViScriptShopServerUtil.removeMoney(player, cost.getTotalMoney());
        //给予玩家物品
        gain.getItems().forEach((itemStack, count) -> {
            ItemStack copy = itemStack.copy();
            copy.setCount(count);
            ItemHandlerHelper.giveItemToPlayer(player, copy);
        });
        //给予玩家钱
        if (gain.getTotalMoney() > 0) ViScriptShopServerUtil.addMoney(player, gain.getTotalMoney());
        //给予玩家经验
        if (gain.getTotalXp() > 0) player.giveExperiencePoints(gain.getTotalXp());
        player.connection.send(new ReloadShopUIPayload(costItems));
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
