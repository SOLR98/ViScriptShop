package com.viscriptshop.compat;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.viscriptshop.Config;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.components.DialogSelect;
import dev.ftb.mods.ftblibrary.api.sidebar.SidebarButtonCreatedEvent;
import dev.ftb.mods.ftblibrary.sidebar.RegisteredSidebarButton;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class FtbLibraryComPat {
    public static void init() {
        if (ViscriptShop.isFtbLibraryLoaded()) {
            SidebarButtonCreatedEvent.EVENT.register(event -> {
                RegisteredSidebarButton button = event.getButton();
                if (button.getId().equals(ViscriptShop.id("shop"))) {
                    button.addVisibilityCondition(() -> Config.showFtbLibraryButton.get());
                }
            });
        }
    }

    public static void click() {
        if (ViscriptShop.isFtbLibraryLoaded()) {
            DialogSelect dialogSelect = new DialogSelect();
            ModularUI modularUI = new ModularUI(UI.of(dialogSelect));
            Minecraft.getInstance().setScreen(new ModularUIScreen(modularUI, Component.empty()));
        }
    }
}
