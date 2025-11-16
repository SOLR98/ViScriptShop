package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.data.ShopInfo;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ViScriptShopClientUtil {
    @Info("客户端打开商店编辑器")
    public static void clientOpenNpcEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        ModularUI ui = UIElementUtil.createUI(new ShopEditor()).shouldCloseOnEsc(false);
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }

    @Info("客户端打开商店")
    public static void clientOpenShop(ShopInfo shopInfo, String title) {
        Minecraft minecraft = Minecraft.getInstance();
        ModularUI ui = UIElementUtil.createUI(new ShopUI(shopInfo, title));
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }
}
