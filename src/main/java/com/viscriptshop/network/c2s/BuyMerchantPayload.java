package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopServerEvent;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.util.CodecUtil;
import com.viscriptshop.util.ItemUtil;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Map;

public class BuyMerchantPayload {
    public static final String BUY_MERCHANT = C2SPayload.MOD_ID + "buy_merchant";

    @RPCPacket(BUY_MERCHANT)
    public static void buyMerchant(RPCSender sender, ShopInfo shopInfo, AggregatedResources cost, AggregatedResources gain) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;

        if (NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyPre(player, shopInfo, cost, gain)).isCanceled()) return;

        // 判断数量是否足够
        Map<ItemStack, Integer> costItems = cost.getItems();
        for (ItemStack itemStack : costItems.keySet()) {
            if (!itemStack.isEmpty() && ItemUtil.getItemForPlayerCount(player, itemStack) < costItems.get(itemStack)) {
                // 物品数量不够
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR, Component.translatable("viscript_shop.message.notEnoughItem", itemStack.getItem().getDescription().getString()));
                NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
                return;
            }
        }

        if (cost.getTotalMoney() > ViScriptShopServerUtil.getMoney(player)) {
            // 钱不够
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.ERROR, Component.translatable("viscript_shop.message.noEnoughMoney", cost.getTotalMoney() - ViScriptShopServerUtil.getMoney(player)));
            NeoForge.EVENT_BUS.post(new ShopServerEvent.BuyFail(player, shopInfo, cost, gain));
            return;
        }

        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.SUCCESS, Component.translatable("viscript_shop.message.buySuccess"));
        NeoForge.EVENT_BUS.post(new ShopServerEvent.BuySuccess(player, shopInfo, cost, gain));

        // 删除物品
        for (ItemStack itemStack : costItems.keySet()) {
            ItemUtil.removeItemForPlayer(player, itemStack, costItems.get(itemStack));
        }

        // 扣除钱
        if (cost.getTotalMoney() > 0) ViScriptShopServerUtil.removeMoney(player, cost.getTotalMoney());

        // 给予玩家物品
        gain.getItems().forEach((itemStack, count) -> {
            ItemStack copy = itemStack.copy();
            copy.setCount(count);
            ItemHandlerHelper.giveItemToPlayer(player, copy);
        });

        // 给予玩家钱
        if (gain.getTotalMoney() > 0) ViScriptShopServerUtil.addMoney(player, gain.getTotalMoney());

        // 给予玩家经验
        if (gain.getTotalXp() > 0) player.giveExperiencePoints(gain.getTotalXp());

        // 执行指令
        if (!gain.getCommands().isEmpty()) {
            for (String command : gain.getCommands()) {
                executeCommands(player, command);
            }
        }

        // 重新加载 UI
        CompoundTag tag = CodecUtil.serializeMap(costItems, ItemStack.OPTIONAL_CODEC, Codec.INT, Platform.getFrozenRegistry());
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.RELOAD_SHOP_UI, tag);
    }

    public static void executeCommands(ServerPlayer player, String value) {
        var commands = value.split(";");
        for (var command : commands) {
            command = command.trim();
            if (!command.isBlank()) {
                MinecraftServer server = Platform.getMinecraftServer();
                CommandSourceStack commandSource = player.createCommandSourceStack().withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();
                var dispatcher = server.getCommands().getDispatcher();
                try {
                    dispatcher.execute(dispatcher.parse(command, commandSource));
                } catch (UnsupportedOperationException e) {
                    server.getCommands().performPrefixedCommand(commandSource, command);
                } catch (CommandSyntaxException e) {
                    ViscriptShop.LOGGER.error("Error executing command on server: {}", command, e);
                }
            }
        }
    }
}