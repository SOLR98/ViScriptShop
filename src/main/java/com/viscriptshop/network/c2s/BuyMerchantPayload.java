package com.viscriptshop.network.c2s;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopServerEvent;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.network.s2c.ReloadShopUIPayload;
import com.viscriptshop.network.s2c.SendMessagePayload;
import com.viscriptshop.util.ItemUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record BuyMerchantPayload(MerchantInfo merchantInfo) implements CustomPacketPayload {
    public static final Type<BuyMerchantPayload> TYPE = new Type<>(ViscriptShop.id("buy_merchant"));
    public static final StreamCodec<FriendlyByteBuf, BuyMerchantPayload> CODEC = StreamCodec.composite(
            MerchantInfo.STREAM_CODEC,
            BuyMerchantPayload::merchantInfo,
            BuyMerchantPayload::new
    );


    public static void execute(BuyMerchantPayload payload, IPayloadContext context) {
        MerchantInfo merchantInfo = payload.merchantInfo();
        ServerPlayer player = (ServerPlayer) context.player();
        if (merchantInfo != null) {
            if (NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, merchantInfo)).isCanceled()) return;
            //判断数量是否足够
            ItemStack itemA = merchantInfo.getItemA();
            if (canBuy(merchantInfo, player, itemA)) return;
            ItemStack itemB = merchantInfo.getItemB();
            if (canBuy(merchantInfo, player, itemB)) return;
            player.connection.send(new SendMessagePayload(Message.Type.SUCCESS, Component.translatable("viscript_shop.message.buySuccess", merchantInfo.getItemResult().getItem().getDescription().getString()).getString()));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, merchantInfo));
            //删除物品
            ItemUtil.removeItemForPlayer(player, itemA, itemA.getCount());
            ItemUtil.removeItemForPlayer(player, itemB, itemB.getCount());
            //给予玩家物品
            ItemHandlerHelper.giveItemToPlayer(player, merchantInfo.getItemResult());
            //给予玩家经验
            if (merchantInfo.getXp() != 0) player.giveExperiencePoints(merchantInfo.getXp());
            //执行指令
            if (!merchantInfo.getCommand().isEmpty())
                Arrays.stream(merchantInfo.getCommand().split(";")).forEach(command -> runCommand(player, command));
            player.connection.send(new ReloadShopUIPayload(itemA, itemB));
        }
    }

    private static boolean canBuy(MerchantInfo merchantInfo, ServerPlayer player, ItemStack itemStackA) {
        if (!itemStackA.isEmpty() && ItemUtil.getItemForPlayerCount(player, itemStackA) < itemStackA.getCount()) {
            player.connection.send(new SendMessagePayload(Message.Type.ERROR, Component.translatable("viscript_shop.message.notEnoughItem", itemStackA.getItem().getDescription().getString()).getString()));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, merchantInfo));
            return true;
        }
        return false;
    }

    private static void runCommand(ServerPlayer player, String command) {
        CommandSourceStack commandSource = player.createCommandSourceStack();
        commandSource = commandSource.withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();
        var dispatcher = player.server.getCommands().getDispatcher();
        try {
            dispatcher.execute(dispatcher.parse(command, commandSource));
        } catch (CommandSyntaxException e) {
            ViscriptShop.LOGGER.error("Error executing command on server: {}", command, e);
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
