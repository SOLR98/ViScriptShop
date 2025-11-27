package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.math.Size;
import com.viscriptshop.ViscriptShop;
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
        ModularUI ui = new ModularUI(UI.of(new EditorWindow(ShopEditor::new), size -> size));
        if (!Platform.isDevEnv()) ui.shouldCloseOnEsc(false).shouldCloseOnKeyInventory(false);
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }

    @Info("客户端打开商店")
    public static void clientOpenShop(ShopInfo shopInfo, String title) {
        Minecraft minecraft = Minecraft.getInstance();
        ShopUI shopUI = new ShopUI(shopInfo, title);
        if (shopInfo.getCategoryInfos().isEmpty()) {
            ViscriptShop.LOGGER.error("不合规的商店信息：商店分类栏为空");
            return;
        }
        ModularUI ui = new ModularUI(UI.of(shopUI, size -> {
            int width = size.width;
            int height = size.height;

            float fontSize = Math.max(12, height * 0.04f);
            for (UIElement child : shopUI.getChildren()) {
                if (child instanceof Label label) label.getTextStyle().fontSize(fontSize);
            }
            return Size.of(width, height);
        }));
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }
}
