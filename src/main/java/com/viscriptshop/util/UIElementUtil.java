package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.CategoryInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIElementUtil {
    public static SearchComponentConfigurator<Item> createItemSearchComponentConfigurator(String name, Supplier<String> getter, Consumer<String> setter, TagKey<Item> tag) {
        return new SearchComponentConfigurator<>(name,
                () -> {
                    String id = getter.get();
                    return id != null ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)) : Items.AIR;
                },
                item -> {
                    ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                    setter.accept(key.toString());
                },
                BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                        getter.get() != null ? getter.get() : Items.AIR.toString()
                )),
                false,
                (word, searchHandler) -> {
                    String lowerWord = word.toLowerCase();
                    for (var key : BuiltInRegistries.ITEM.keySet()) {
                        if (Thread.currentThread().isInterrupted()) return;
                        Item item = BuiltInRegistries.ITEM.get(key);
                        if (tag != null && !item.getDefaultInstance().is(tag)) continue;
                        if (key.toString().toLowerCase().contains(lowerWord) || Component.translatable(item.getDescriptionId()).getString().toLowerCase().contains(lowerWord)) {
                            ((IResultHandler<Item>) searchHandler).acceptResult(BuiltInRegistries.ITEM.get(key));
                        }
                    }
                },
                value -> BuiltInRegistries.ITEM.getKey(value).toString(),
                value -> {
                    UIElementProvider<Item> itemUIProvider = UIElementProvider.iconText(
                            ItemStackTexture::new,
                            item -> Component.translatable(item.getDescriptionId())
                    );
                    return itemUIProvider.createUI(value);
                }
        );
    }

    public static SearchComponentConfigurator<Item> createItemSearchComponentConfigurator(String name, Supplier<String> getter, Consumer<String> setter) {
        return createItemSearchComponentConfigurator(name, getter, setter, null);
    }

    public static ItemSlot createItemSlot(ItemStack item, int size, boolean isRenderBackgroundTexture, boolean showItemTooltips) {
        return (ItemSlot) new ItemSlot().setItem(item)
                .slotStyle(slotStyle -> {
                    if (!isRenderBackgroundTexture) slotStyle.hoverOverlay(new ColorRectTexture(0));
                    slotStyle.showItemTooltips(showItemTooltips);
                })
                .layout(layout -> {
                    layout.setWidth(size);
                    layout.setHeight(size);
                })
                .style(style -> {
                    if (!isRenderBackgroundTexture) style.backgroundTexture(null);
                });
    }

    public static ItemSlot createItemSlot(ItemStack item, boolean isRenderBackgroundTexture, boolean showItemTooltips) {
        return createItemSlot(item, 16, isRenderBackgroundTexture, showItemTooltips);
    }

    public static UIElement createCategoryUI(CategoryInfo categoryInfo, boolean isSelected, Consumer<CategoryInfo> onSelectCallback, IGuiTexture defaultBg, IGuiTexture selectedBg) {
        UIElement category = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeight(18);
            layout.setGap(YogaGutter.ALL, 5);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setMargin(YogaEdge.BOTTOM, 5);
        }).addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                onSelectCallback.accept(categoryInfo);
            }
        });
        UIElement icon = new UIElement().layout(layout -> {
            layout.setMinWidth(16);
            layout.setMinHeight(16);
            layout.setWidth(16);
            layout.setHeight(16);
            layout.setMaxWidth(16);
            layout.setMaxHeight(16);
        });
        Label label = (Label) new Label().setText(categoryInfo.getName())
                .textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
                    textStyle.fontSize(8);
                    if (isSelected) {
                        textStyle.textColor(ColorPattern.WHITE.color);
                    }
                }).layout(layout -> {
                    layout.setHeightPercent(100);
                });
        UIElement name = new UIElement().layout(layout -> {
                    layout.setFlex(8);
                    layout.setHeightPercent(100);
                    layout.setPadding(YogaEdge.ALL, 3);
                }).style(style -> {
                    style.backgroundTexture(isSelected ? selectedBg : defaultBg);
                })
                .addChild(label);
        switch (categoryInfo.getIconType()) {
            case ITEM -> icon = createItemSlot(categoryInfo.getIconItem(), false, false);
            case TEXTURE -> {
                String iconTexture = categoryInfo.getIconTexture();
                if (!iconTexture.isEmpty() && ViscriptShop.isPresentResource(ResourceLocation.parse(iconTexture))) {
                    icon.style(style -> style.backgroundTexture(SpriteTexture.of(iconTexture)));
                }
            }
        }
        category.addChildren(icon, name);
        return category;
    }

    public static void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder, @NotNull UIElement parent) {
        if (menuBuilder != null && !menuBuilder.isEmpty()) {
            openMenu(posX, posY, menuBuilder.build(), TreeBuilder.Menu::uiProvider, parent).setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider).setOnNodeClicked(TreeBuilder.Menu::handle);
        }
    }

    private static <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, UIElementProvider<T> uiProvider, @NotNull UIElement parent) {
        Menu<T, C> menu = new Menu<>(menuNode, uiProvider);
        menu.layout((layout) -> {
            layout.setPosition(YogaEdge.LEFT, posX - parent.getContentX());
            layout.setPosition(YogaEdge.TOP, posY - parent.getContentY());
        });
        parent.addChildren(menu);
        return menu;
    }
}
