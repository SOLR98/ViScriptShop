package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.util.ShopHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.IOException;

public class C2SPayload {
    public static final String UPLOAD_SHOP_FILE = "upload_shop_file";

    @RPCPacket(UPLOAD_SHOP_FILE)
    public static void uploadNpcFile(RPCSender sender, CompoundTag tag) {
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

    public static void sendEditorDialog(ServerPlayer player, String title, String content) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_EDITOR_DIALOG, title, content);
    }
}
