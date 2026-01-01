package com.viscriptshop.compat;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.components.DialogSelect;
import com.viscriptshop.util.ViScriptShopClientUtil;
import net.minecraft.client.Minecraft;

public class FtbLibraryComPat {
    public void init() {
        if (ViscriptShop.isFtbLibraryLoaded()) {
            DialogSelect dialogSelect = new DialogSelect(ViScriptShopClientUtil::clientOpenShop);
            Minecraft.getInstance().setScreen(new ModularUI(UI.of(dialogSelect)).getScreen());
        }
    }
}
