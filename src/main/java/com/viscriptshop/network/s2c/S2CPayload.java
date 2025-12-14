package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.util.ViScriptShopClientUtil;
import net.minecraft.client.Minecraft;

public class S2CPayload {
    public static final String OPEN_SHOP_EDITOR = "openShopEditor";
    public static final String SEND_MESSAGE = "sendMessage";

    @RPCPacket(OPEN_SHOP_EDITOR)
    public static void openShopEditor(RPCSender sender) {
        ViScriptShopClientUtil.clientOpenNpcEditor();
    }

    @RPCPacket(SEND_MESSAGE)
    public static void sendMessage(RPCSender sender, Message.Type messageType, String message) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            Message.send(messageType, message, shopUI);
        }
    }
}
