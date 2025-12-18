package com.viscriptshop.gui.view;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.components.MerchantFloatView;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.project.ShopProject;
import com.viscriptshop.util.UIElementUtil;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;

import java.util.Comparator;

public class ShopPreviewView extends View {
    public final ShopEditor editor;
    public final UIElement head = new UIElement();
    public final ScrollerView scrollerView = new ScrollerView();
    private final MerchantFloatView merchantFloatView;
    private CategoryInfo selectedCategory = null;

    public ShopPreviewView(ShopEditor editor) {
        super("viscript_shop.editor.view.shopPreview");
        this.editor = editor;
        this.merchantFloatView = new MerchantFloatView(editor, "viscript_shop.data.category.merchants");
        this.head.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidthPercent(100);
            layout.setHeight(15);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        head.setDisplay(YogaDisplay.NONE);
        UIElement addButton = new Button().setText("viscript_shop.editor.add.merchant").setOnClick(event -> {
            MerchantInfo merchantInfo = new MerchantInfo();
            selectedCategory.getMerchants().add(merchantInfo);
            merchantFloatView.showEdit(merchantInfo, selectedCategory.getShopType());
        }).layout(layout -> {
            layout.setMargin(YogaEdge.RIGHT, 5);
            layout.setHeightPercent(100);
        });
        UIElement sortButton = new Button().setText("viscript_shop.editor.sort.merchant").setOnClick(event -> {
            Dialog.showCheckBox("viscript_shop.editor.sort.merchant", "viscript_shop.dialog.sort_merchant.info", (result) -> {
                if (result) selectedCategory.getMerchants().sort(Comparator.comparingInt(MerchantInfo::getStage));
            }).show(editor);
        }).layout(layout -> {
            layout.setHeightPercent(100);
        });
        head.addChildren(addButton, sortButton);

        this.scrollerView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });
        this.scrollerView.viewContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
            layout.setPadding(YogaEdge.ALL, 5);
            layout.setGap(YogaGutter.ALL, 5);
        });

        this.addChildren(head, scrollerView);
    }

    public void loadView() {
        this.scrollerView.viewContainer.addEventListener(UIEvents.TICK, event -> reloadMerchants());
    }

    public void reloadMerchants() {
        selectedCategory = editor.categoryView.getSelectedCategory();
        if (selectedCategory != null && editor.getCurrentProject() instanceof ShopProject shopProject) {
            head.setDisplay(YogaDisplay.FLEX);
            scrollerView.clearAllScrollViewChildren();

            // 重新添加所有商品
            for (int i = 0; i < selectedCategory.getMerchants().size(); i++) {
                MerchantInfo merchantInfo = selectedCategory.getMerchants().get(i);
                if (merchantInfo.getStage() != shopProject.shop.shopInfo.getStage()) continue;
                int finalI = i;
                scrollerView.addScrollViewChild(createMerchant(merchantInfo, i)
                        .addEventListener(UIEvents.MOUSE_DOWN, event -> {
                            showMerchantMenuTab(event, merchantInfo, finalI);
                        })
                );
            }
        }
    }

    public UIElement createMerchant(MerchantInfo merchantInfo, int i) {
        switch (selectedCategory.getShopType()) {
            case ITEM_FOR_ITEM -> {
                UIElement merchant = new UIElement().layout(layout -> {
                    layout.setWidth(100);
                    layout.setGap(YogaGutter.ALL, 5);
                    layout.setMargin(YogaEdge.LEFT, 5);
                    layout.setJustifyContent(YogaJustify.CENTER);
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setAlignItems(YogaAlign.CENTER);
                });
                ItemSlot itemASlot = (ItemSlot) UIElementUtil.createItemSlot(merchantInfo.getItemA(), false, true).addEventListener(UIEvents.MOUSE_DOWN, event -> showMerchantMenuTab(event, merchantInfo, i));
                ItemSlot itemBSlot = (ItemSlot) UIElementUtil.createItemSlot(merchantInfo.getItemB(), false, true).addEventListener(UIEvents.MOUSE_DOWN, event -> showMerchantMenuTab(event, merchantInfo, i));
                ItemSlot resultItemSlot = (ItemSlot) UIElementUtil.createItemSlot(merchantInfo.getItemResult(), true, true).addEventListener(UIEvents.MOUSE_DOWN, event -> showMerchantMenuTab(event, merchantInfo, i));
                merchant.addChildren(itemASlot, itemBSlot,
                        new UIElement().style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR_S_LIGHT)).layout(layout -> {
                            layout.setWidth(6);
                            layout.setHeight(6);
                            layout.setMargin(YogaEdge.ALL, 5);
                        }),
                        resultItemSlot
                );
                return merchant;
            }
            case CURRENCY -> {
                UIElement merchant = new UIElement().layout(layout -> {
                    layout.setWidth(55);
                    layout.setFlexDirection(YogaFlexDirection.COLUMN);
                    layout.setAlignItems(YogaAlign.CENTER);
                    layout.setJustifyContent(YogaJustify.CENTER);
                    layout.setPadding(YogaEdge.ALL, 5);
                });
                merchant.getStyle().backgroundTexture(Sprites.RECT_SOLID);

                ItemSlot itemSlot = (ItemSlot) UIElementUtil.createItemSlot(merchantInfo.getItemResult(), false, true)
                        .layout(layout -> {
                            layout.setWidth(30);
                            layout.setHeight(30);
                        });

                MerchantInfo.TradeType tradeType = merchantInfo.getTradeType();
                String tradeText = tradeType.getSerializedName();

                Button tradeLabel = (Button) new Button()
                        .setText(tradeText)
                        .textStyle(style -> style
                                .textAlignHorizontal(Horizontal.CENTER)
                        ).layout(layout -> {
                            layout.setWidthPercent(100);
                            layout.setMargin(YogaEdge.TOP, 5);
                            layout.setMargin(YogaEdge.BOTTOM, 10);
                        });

                int money = merchantInfo.getMoney();
                Label priceLabel = (Label) new Label()
                        .setText(Component.literal("◎" + money))
                        .textStyle(style -> style
                                .textColor(0xFFFFAA00)
                                .textAlignHorizontal(Horizontal.CENTER)
                        ).layout(layout -> {
                            layout.setWidthPercent(100);
                        });

                merchant.addChildren(itemSlot, tradeLabel, priceLabel);

                return merchant;
            }
            default -> {
                return new UIElement();
            }
        }
    }

    public void removeMerchant(int index) {
        selectedCategory.getMerchants().remove(index);
    }

    private void showMerchantMenuTab(UIEvent event, MerchantInfo merchantInfo, int index) {
        if (event.button == 1) {
            UIElement clickedElement = event.currentElement;
            float posX = clickedElement.getPositionX();
            float posY = clickedElement.getPositionY() + clickedElement.getSizeHeight();

            TreeBuilder.Menu merchantMenu = TreeBuilder.Menu.start().leaf("viscript_shop.button.update", () -> {
                merchantFloatView.showEdit(merchantInfo, selectedCategory.getShopType());
            }).leaf("viscript_shop.button.delete", () -> {
                Dialog.showCheckBox("viscript_shop.button.delete", "viscript_shop.dialog.delete_merchant.info", (result) -> {
                    if (result) removeMerchant(index);
                }).show(editor);
            });

            UIElementUtil.openMenu(posX, posY, merchantMenu, this);
            event.stopPropagation();
        }
    }
}
