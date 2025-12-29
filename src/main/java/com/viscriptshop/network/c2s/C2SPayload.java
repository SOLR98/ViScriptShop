package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.util.ShopHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.IOException;

public class C2SPayload {
    public static final String UPLOAD_NPC_FILE = "upload_npc_file";

    @RPCPacket(UPLOAD_NPC_FILE)
    public static void uploadNpcFile(RPCSender sender, CompoundTag tag) {
        if (!sender.isServer()) {
            ServerPlayer player = sender.asPlayer();
            if (player == null) return;

            String fileName = tag.getString("fileName");
            if (fileName.isEmpty()) return;
            tag.remove("fileName");

            File file = new File(LDLib2.getAssetsDir(), ShopHelper.SHOP_PATH + "/" + fileName + ".npc");
            boolean exists = file.exists();
            if (!exists) {
                if (file.getParentFile().mkdirs()) {
                    try {
                        if (!file.createNewFile()) {
                            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.WARN, "上传文件失败", "无法创建npc文件！");
                            return;
                        }
                    } catch (IOException e) {
                        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.WARN, "上传文件失败", "无法创建npc文件！" + e.getMessage());
                        return;
                    }
                }
            }
            try {
                NbtIo.writeCompressed(tag, file.toPath());
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.WARN, "上传文件成功", exists ? "已覆盖同名npc文件！" : "已创建新npc文件并写入！");
            } catch (IOException e) {
                RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SEND_MESSAGE, Message.Type.WARN, "上传文件失败", "无法写入npc文件！" + e.getMessage());
            }
        }
    }
}
