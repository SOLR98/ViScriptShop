package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.serialization.Codec;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.components.DialogSelect;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.util.CodecUtil;
import com.viscriptshop.util.ViScriptShopClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class S2CPayload {
    public static final String MOD_ID = ViscriptShop.MOD_ID + ":";
    public static final String OPEN_SHOP_EDITOR = MOD_ID + "open_shop_editor";
    public static final String OPEN_SHOP_UI = MOD_ID + "open_shop_ui";
    public static final String SEND_MESSAGE = MOD_ID + "send_message";
    public static final String SEND_EDITOR_DIALOG = MOD_ID + "send_editor_dialog";
    public static final String GET_SHOP_INFO_S2C = MOD_ID + "get_shop_info_s2c";

    @RPCPacket(OPEN_SHOP_EDITOR)
    public static void openShopEditor(RPCSender sender, CompoundTag tag) {
        ViScriptShopClientUtil.clientOpenNpcEditor(tag);
    }

    @RPCPacket(OPEN_SHOP_UI)
    public static void openShopUI(RPCSender sender, ShopInfo shopInfo) {
        ViScriptShopClientUtil.clientOpenShop(shopInfo);
    }

    @RPCPacket(SEND_MESSAGE)
    public static void sendMessage(RPCSender sender, Message.Type messageType, Component message) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen) {
            Message.send(messageType, message.getString(), screen.modularUI.ui.rootElement);
        }
    }

    @RPCPacket(SEND_EDITOR_DIALOG)
    public static void sendEditorDialog(RPCSender sender, Component title, Component content) {
        if (sender.isServer() && Minecraft.getInstance().screen instanceof ModularUIScreen uiScreen) {
            if (uiScreen.modularUI.ui.rootElement instanceof EditorWindow window && window.getCurrentEditor() instanceof ShopEditor editor) {
                Dialog.showNotification(title.getString(), content.getString(), null).show(editor);
            }
        }
    }

    @RPCPacket(GET_SHOP_INFO_S2C)
    public static void getShopInfoS2C(RPCSender sender, CompoundTag compoundTag) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof DialogSelect dialogSelect) {
            Codec<Map<String, String>> codec = Codec.unboundedMap(Codec.STRING, Codec.STRING);
            dialogSelect.reload(CodecUtil.deserializeNBT(codec, compoundTag, Platform.getFrozenRegistry()));
        }
    }
}
