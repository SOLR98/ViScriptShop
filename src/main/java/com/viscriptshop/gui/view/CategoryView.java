package com.viscriptshop.gui.view;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.components.CategoryFloatView;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.project.ShopProject;
import com.viscriptshop.util.UIElementUtil;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

import java.util.List;

@Getter
public class CategoryView extends View {
    public final ShopEditor editor;
    public final ScrollerView scrollerView = new ScrollerView();
    private CategoryInfo selectedCategory = null;
    private ShopInfo shopInfo;
    private final CategoryFloatView categoryFloatView;

    public CategoryView(ShopEditor editor) {
        super("viscript_shop.editor.view_category");
        this.editor = editor;
        this.categoryFloatView = new CategoryFloatView(editor, "viscript_shop.data.shop.categoryInfos");
        this.scrollerView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        this.scrollerView.viewPort.getStyle().backgroundTexture(null);
        this.addChildren(this.scrollerView);
    }

    public void loadView() {
        if (editor.getCurrentProject() instanceof ShopProject shopProject) {
            this.shopInfo = shopProject.shop.shopInfo;
            scrollerView.viewContainer.layout(layout -> {
                layout.setPadding(YogaEdge.ALL, 5);
                layout.setFlexDirection(YogaFlexDirection.COLUMN);
            }).addEventListener(UIEvents.TICK, event -> {
                reloadCategoryList();
            });
        }
    }

    public void reloadCategoryList() {
        scrollerView.clearAllScrollViewChildren();

        for (int i = 0; i < shopInfo.getCategoryInfos().size(); i++) {
            CategoryInfo categoryInfo = shopInfo.getCategoryInfos().get(i);
            UIElement categoryUI = UIElementUtil.createCategoryUI(
                    categoryInfo,
                    categoryInfo.equals(this.selectedCategory),
                    this::setSelectedCategory,
                    new ColorRectTexture(ColorPattern.T_BLACK.color),
                    new ColorRectTexture(ColorPattern.T_WHITE.color)
            ).layout(layout -> {
                layout.setFlex(11);
                layout.setMargin(YogaEdge.RIGHT, 10);
            });
            int finalI = i;
            UIElement updateButton = new UIElement().style(style -> {
                style.backgroundTexture(Icons.EDIT_ON.copy().setColor(ColorPattern.LIGHT_BLUE.color));
                style.tooltips("viscript_shop.button.update");
            }).layout(layout -> {
                layout.setMargin(YogaEdge.TOP, 5);
                layout.setFlex(1);
                layout.setHeight(15);
            }).addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    categoryFloatView.showEdit(categoryInfo);
                    event.stopPropagation();
                }
            });
            UIElement deleteButton = new UIElement().style(style -> {
                style.backgroundTexture(Icons.DELETE.copy().setColor(ColorPattern.RED.color));
                style.tooltips("viscript_shop.button.delete");
            }).layout(layout -> {
                layout.setMargin(YogaEdge.TOP, 5);
                layout.setFlex(1);
                layout.setHeight(15);
            }).addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    Dialog.showCheckBox("viscript_shop.button.delete", "viscript_shop.dialog.delete_category.info", (result) -> {
                        if (result) removeCategory(finalI);
                    }).show(editor);
                    event.stopPropagation();
                }
            });
            UIElement uiElement = new UIElement().addChildren(categoryUI, updateButton, deleteButton);
            uiElement.getLayout().setFlexDirection(YogaFlexDirection.ROW);
            scrollerView.viewContainer.addChildren(uiElement);
        }

        scrollerView.viewContainer.addChildren(new Button().setText("+").setOnClick(event -> {
                    CategoryInfo categoryInfo = new CategoryInfo();
                    shopInfo.getCategoryInfos().add(categoryInfo);
                    this.categoryFloatView.showEdit(categoryInfo);
                }).layout(layout -> {
                    layout.setMaxWidth(15);
                    layout.setMaxHeight(15);
                }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(Component.translatable("viscript_shop.editor.add.category")), null, null, null);
                })
        );
    }

    private void removeCategory(int i) {
        shopInfo.getCategoryInfos().remove(i);
    }

    private void setSelectedCategory(CategoryInfo newCategory) {
        if (!newCategory.equals(this.selectedCategory)) {
            this.selectedCategory = newCategory;
        }
    }
}
