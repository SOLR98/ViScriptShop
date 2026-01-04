package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.serialization.Codec;
import com.viscriptshop.command.ShopCommand;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.util.CodecUtil;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class C2SPayload {
    public static final String UPLOAD_SHOP_FILE = "upload_shop_file";
    public static final String GET_SHOP_INFO_C2S = "get_shop_info_c2s";
    public static final String OPEN_SHOP_UI_C2S = "open_shop_ui_c2s";

    @RPCPacket(UPLOAD_SHOP_FILE)
    public static void uploadShopFile(RPCSender sender, CompoundTag tag) {
        if (!sender.isServer()) {
            ServerPlayer player = sender.asPlayer();
            if (player == null) return;

            String fileName = tag.getString("fileName");
            if (fileName.isEmpty()) return;
            tag.remove("fileName");

            File file = new File(LDLib2.getAssetsDir(), ShopHelper.SHOP_PATH + "/" + fileName + Shop.SUFFIX);
            boolean exists = file.exists();
            if (!exists) {
                if (file.getParentFile().mkdirs()) {
                    try {
                        if (!file.createNewFile()) {
                            sendEditorDialog(player, Component.translatable("viscript_shop.message.uploadFile.error").getString(), Component.translatable("viscript_shop.message.uploadFile.error.content").getString());
                            return;
                        }
                    } catch (IOException e) {
                        sendEditorDialog(player, Component.translatable("viscript_shop.message.uploadFile.error").getString(), Component.translatable("viscript_shop.message.uploadFile.error.content").getString() + e.getMessage());
                        return;
                    }
                }
            }
            try {
                NbtIo.writeCompressed(tag, file.toPath());
                sendEditorDialog(player, Component.translatable("viscript_shop.message.uploadFile.success").getString(), exists ? Component.translatable("viscript_shop.message.uploadFile.success.content").getString() : Component.translatable("viscript_shop.message.uploadFile.success.content1").getString());
            } catch (IOException e) {
                sendEditorDialog(player, Component.translatable("viscript_shop.message.uploadFile.error").getString(), Component.translatable("viscript_shop.message.uploadFile.error.content1").getString() + e.getMessage());
            }
        }
    }

    @RPCPacket(GET_SHOP_INFO_C2S)
    public static void getShopInfo(RPCSender sender) {
        Map<String, String> shopInfos = new HashMap<>();
        ShopCommand.getServerShopFiles().forEach(fileName -> {
            ShopInfo shopInfo = ViScriptShopServerUtil.getShopInfo(fileName);
            if (shopInfo.isQuickOpening()) {
                String name = shopInfo.getName();
                shopInfos.put(fileName, name.isEmpty() ? "viscript_shop.ui.title" : name);
            }
        });
        Codec<Map<String, String>> codec = Codec.unboundedMap(Codec.STRING, Codec.STRING);
        RPCPacketDistributor.rpcToPlayer(sender.asPlayer(), S2CPayload.GET_SHOP_INFO_S2C, CodecUtil.serializeNBT(codec, shopInfos, Platform.getFrozenRegistry()));
    }

    @RPCPacket(OPEN_SHOP_UI_C2S)
    public static void openShopUI(RPCSender sender, String shopFileName) {
        ViScriptShopServerUtil.serverOpenShop(sender.asPlayer(), shopFileName);
    }


    public static void sendEditorDialog(ServerPlayer player, String title, String content) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_EDITOR_DIALOG, title, content);
    }
}
