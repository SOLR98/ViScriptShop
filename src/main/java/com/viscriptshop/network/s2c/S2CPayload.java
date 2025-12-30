package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.util.ViScriptShopClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public class S2CPayload {
    public static final String OPEN_SHOP_EDITOR = "openShopEditor";
    public static final String SEND_MESSAGE = "sendMessage";
    public static final String SEND_EDITOR_DIALOG = "sendEditorDialog";

    @RPCPacket(OPEN_SHOP_EDITOR)
    public static void openShopEditor(RPCSender sender, CompoundTag tag) {
        ViScriptShopClientUtil.clientOpenNpcEditor(tag);
    }

    @RPCPacket(SEND_MESSAGE)
    public static void sendMessage(RPCSender sender, Message.Type messageType, String message) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen) {
            Message.send(messageType, message, screen.modularUI.ui.rootElement);
        }
    }

    @RPCPacket(SEND_EDITOR_DIALOG)
    public static void sendEditorDialog(RPCSender sender, String title, String content) {
        if (sender.isServer() && Minecraft.getInstance().screen instanceof ModularUIScreen uiScreen) {
            if (uiScreen.modularUI.ui.rootElement instanceof EditorWindow window && window.getCurrentEditor() instanceof ShopEditor editor) {
                Dialog.showNotification(title, content, null).show(editor);
            }
        }
    }
}
