package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.viscriptshop.gui.data.ShopInfo;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class DialogSelect extends Dialog {
    public ShopInfo selectedShopInfo;
    public Selector<ShopInfo> selector;

    public DialogSelect(Consumer<ShopInfo> onSelect) {
        super();
        this.setTitle("sidebar_button.viscript_shop.shop.tooltip");
        selector = new Selector<>();
        selector.setOnValueChanged(newValue -> {
            selector.setValue(newValue);
            selectedShopInfo = newValue;
        });
        selector.setCandidateUIProvider(UIElementProvider.text(value -> Component.translatable(value.getName())));
        this.addButton(new Button()
                .setOnClick(event -> {
                    onSelect.accept(selectedShopInfo);
                })
                .setText("ldlib.gui.tips.confirm"));
        this.addButton(new Button()
                .setOnClick(event -> this.close())
                .setText("ldlib.gui.tips.cancel"));
        this.contentContainer.addChildren(selector);
    }
}
