package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopClientEvent;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.components.PlayerHeadElement;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.c2s.BuyMerchantPayload;
import com.viscriptshop.network.c2s.GetItemCountC2SPayload;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.UIElementUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import org.appliedenergistics.yoga.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopUI extends UIElement {
    Minecraft minecraft = Minecraft.getInstance();
    //ui
    public ScrollerView categoryView = new ScrollerView();
    public ScrollerView merchantsView = new ScrollerView();
    public ScrollerView shoppingCarView = new ScrollerView();
    public ScrollerView inventoryView = new ScrollerView();

    private final IGuiTexture DARK_BACKGROUND_RECT = Sprites.BORDER_RT0;
    private final IGuiTexture LIGHT_BACKGROUND_RECT = Sprites.RECT_RD_SOLID;
    private final SpriteTexture RIGHT_ARROW = SpriteTexture.of(ViscriptShop.formattedMod("textures/right_arrow.png"));
    private final SpriteTexture LOCK = SpriteTexture.of(ViscriptShop.formattedMod("textures/lock.png"));

    //data
    //玩家身上对应物品的数量
    public Map<ItemStack, Integer> playerItems = new HashMap<>();
    //打开的商店信息
    public ShopInfo currentShopInfo;
    //玩家选择的商店信息
    @Getter
    @Setter
    private CategoryInfo selectedCategory;
    @Getter
    @Setter
    private String search = "";


    public ShopUI(ShopInfo shopInfo, String title) {
        this.playerItems.clear();
        this.currentShopInfo = ShopHelper.cacheShopInfo == null ? shopInfo : ShopHelper.cacheShopInfo;
        this.currentShopInfo.setStage(shopInfo.getStage());
        if (minecraft.player != null) {
            selectedCategory = this.currentShopInfo.getCategoryInfos().getFirst();
            minecraft.player.connection.send(new GetItemCountC2SPayload(selectedCategory));
        }
        this.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
        }).addEventListener(UIEvents.TICK, event -> NeoForge.EVENT_BUS.post(new ShopClientEvent.Tick(this)));
        UIElement root = new UIElement();
        root.layout((layout) -> {
            layout.setWidthPercent(90);
            layout.setHeightPercent(80);
            layout.setGap(YogaGutter.ALL, 3);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        this.addChildren(root);
        //左
        UIElement left = new UIElement().layout(layout -> {
            layout.setWidthPercent(22);
            layout.setGap(YogaGutter.ALL, 3);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        });
        UIElement leftTop = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(10);
        }).style(style -> {
            style.backgroundTexture(DARK_BACKGROUND_RECT);
        }).addChild(new Label().setText("viscript_shop.data.shop.categoryInfos").textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
        }).layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        }));

        categoryView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(80);
        }).addEventListener(UIEvents.TICK, event -> {
            reloadCategoryList();
        });
        categoryView.viewPort.getStyle().backgroundTexture(null);
        categoryView.viewContainer.layout(layout -> {
            layout.setGap(YogaGutter.COLUMN, 5);
            layout.setPadding(YogaEdge.ALL, 3);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        });
        UIElement leftCenter = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(74);
        }).style(style -> {
            style.backgroundTexture(DARK_BACKGROUND_RECT);
        }).addChild(categoryView);

        UIElement leftBottom = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(13);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        }).style(style -> {
            style.backgroundTexture(DARK_BACKGROUND_RECT);
        }).addChildren(
                new Label().setText("viscript_shop.ui.balance").textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.BOTTOM);
                }).layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(50);
                }),
                new Label().setText("◎ " + minecraft.player.getData(ShopRegistries.MONEY).getMoney()).textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
                }).layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(50);
                }).addEventListener(UIEvents.TICK, event -> {
                    ((Label) event.currentElement).setText("◎ " + minecraft.player.getData(ShopRegistries.MONEY).getMoney());
                })
        );

        left.addChildren(leftTop, leftCenter, leftBottom);
        //中
        UIElement center = new UIElement().layout(layout -> {
            layout.setWidthPercent(55);
            layout.setHeightPercent(100);
            layout.setPadding(YogaEdge.HORIZONTAL, 5);
            layout.setPadding(YogaEdge.VERTICAL, 3);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        });
        center.getStyle().backgroundTexture(DARK_BACKGROUND_RECT);
        UIElement head = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(10);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setJustifyContent(YogaJustify.SPACE_BETWEEN);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        Label centerTitle = (Label) new Label().setText("viscript_shop.ui.topTitle").textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
        }).layout(layout -> {
            layout.setHeightPercent(100);
        });
        SearchComponent<Item> searchComponent = UIElementUtil.createItemSearchComponentConfigurator("", this::getSearch, this::setSearch).searchComponent;
        searchComponent.layout(layout -> {
            layout.setMargin(YogaEdge.LEFT, 5);
            layout.setWidth(70);
        }).addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            reloadMerchants();
        });
        Label stageLabel = (Label) new Label().setText(Component.translatable("viscript_shop.ui.stage", this.currentShopInfo.getStage())).textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
        }).layout(layout -> {
            layout.setHeightPercent(100);
            layout.setMargin(YogaEdge.RIGHT, 10);
        });
        head.addChildren(new UIElement().layout(layout -> {
            layout.setHeightPercent(100);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
        }).addChildren(centerTitle, searchComponent), new UIElement().layout(layout -> {
            layout.setHeightPercent(100);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
        }).addChildren(stageLabel, new PlayerHeadElement()));

        merchantsView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });

        merchantsView.viewPort.getStyle().backgroundTexture(null);

        merchantsView.viewContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
            layout.setGap(YogaGutter.ALL, 5);
        });

        reloadMerchants();

        center.addChildren(head, merchantsView);
        //右
        UIElement right = new UIElement().layout(layout -> {
            layout.setWidthPercent(25);
            layout.setHeightPercent(100);
            layout.setGap(YogaGutter.ALL, 3);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        });
        UIElement rightTop = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(10);
        }).style(style -> {
            style.backgroundTexture(DARK_BACKGROUND_RECT);
        }).addChild(new Label().setText(title)
                .textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
                })
                .layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }));

        UIElement rightBottom = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
            layout.setPadding(YogaEdge.ALL, 5);
            layout.setGap(YogaGutter.ALL, 4);
        });
        rightBottom.getStyle().backgroundTexture(DARK_BACKGROUND_RECT);

        shoppingCarView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(35);
        });
        shoppingCarView.viewContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
        });
        shoppingCarView.viewPort.getLayout().setPadding(YogaEdge.ALL, 3);
        reloadShoppingItem();

        inventoryView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(35);
        });
        inventoryView.viewContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
        });
        inventoryView.viewPort.getLayout().setPadding(YogaEdge.ALL, 3);
        reloadInventoryItem();

        Button buyButton = (Button) new Button().setText("viscript_shop.button.buy").setOnClick(event -> {
            AggregatedResources costSummary = AggregatedResources.getCostSummary(this.currentShopInfo);
            AggregatedResources gainSummary = AggregatedResources.getGainSummary(this.currentShopInfo);
            if (costSummary.isEmpty() || gainSummary.isEmpty()) {
                Message.warn("viscript_shop.message.shoppingCar.empty", this);
                return;
            }
            minecraft.player.connection.send(new BuyMerchantPayload(this.currentShopInfo, costSummary, gainSummary));
        }).layout(layout -> {
            layout.setWidthPercent(40);
        });

        Button tsButton = (Button) new Button().setText("viscript_shop.button.ts").setOnClick(event -> {
            ShopHelper.cacheShopInfo = this.currentShopInfo;
            if (minecraft.screen != null) minecraft.screen.onClose();
        }).layout(layout -> {
            layout.setWidthPercent(40);
        });

        rightBottom.addChildren(new Label().setText("viscript_shop.ui.shoppingCar").textStyle(textStyle -> textStyle.adaptiveHeight(true)), shoppingCarView,
                new Label().setText("viscript_shop.ui.inventory").textStyle(textStyle -> textStyle.adaptiveHeight(true)), inventoryView
                , new UIElement().layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setJustifyContent(YogaJustify.SPACE_BETWEEN);
                    layout.setHeightPercent(20);
                }).addChildren(buyButton, tsButton));

        right.addChildren(rightTop, rightBottom);

        root.addChildren(left, center, right);
    }

    public void reloadCategoryList() {
        categoryView.clearAllScrollViewChildren();

        for (int i = 0; i < currentShopInfo.getCategoryInfos().size(); i++) {
            CategoryInfo categoryInfo = currentShopInfo.getCategoryInfos().get(i);
            UIElement categoryUI = UIElementUtil.createCategoryUI(
                    categoryInfo,
                    categoryInfo.equals(this.selectedCategory),
                    value -> {
                        setSelectedCategory(value);
                        if (minecraft.player != null) {
                            minecraft.player.connection.send(new GetItemCountC2SPayload(selectedCategory));
                        }
                        reloadMerchants();
                    },
                    new ColorRectTexture(ColorPattern.T_BLACK.color),
                    new ColorRectTexture(ColorPattern.T_WHITE.color)
            );
            categoryView.viewContainer.addChildren(categoryUI);
        }
    }

    public void reloadMerchants() {
        merchantsView.clearAllScrollViewChildren();

        // 重新添加所有商品
        for (int i = 0; i < selectedCategory.getMerchants().size(); i++) {
            MerchantInfo merchantInfo = selectedCategory.getMerchants().get(i);
            //商品上锁样式：隐藏
            if (currentShopInfo.getLockedMerchantVisibility().equals(ShopInfo.LockedMerchantVisibility.HIDDEN) && merchantInfo.getStage() > currentShopInfo.getStage()) {
                continue;
            }
            //搜索购买物品筛选
            if (!merchantInfo.getItemResult().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(this.search))) && !this.search.isEmpty() && !this.search.equals(Items.AIR.toString())) {
                continue;
            }
            merchantsView.addScrollViewChild(createMerchant(merchantInfo, i));
        }
    }

    public void reloadShoppingItem() {
        shoppingCarView.clearAllScrollViewChildren();

        AggregatedResources gainSummary = AggregatedResources.getGainSummary(currentShopInfo);
        gainSummary.getItems().forEach((itemStack, count) -> {
            Label countLabel = (Label) new Label().setText(getCountText(count, 9999))
                    .textStyle(textStyle -> {
                        textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.BOTTOM);
                        textStyle.fontSize(5);
                    })
                    .layout(layout -> {
                        layout.setWidth(10);
                        layout.setHeightPercent(100);
                    })
                    .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                        event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(String.valueOf(count))), null, null, null);
                    });
            shoppingCarView.addScrollViewChild(createItemInfoBox().addChildren(UIElementUtil.createItemSlot(itemStack, false, true), countLabel));
        });
        if (gainSummary.getTotalMoney() > 0) {
            Label moneyIcon = (Label) new Label().setText("◎ ").textStyle(textStyle -> {
                textStyle.textAlignVertical(Vertical.CENTER);
                textStyle.fontSize(16);
            }).layout(layout -> {
                layout.setWidth(16);
                layout.setHeightPercent(100);
                layout.setMargin(YogaEdge.LEFT, 2);
            });
            Label money = (Label) new Label().setText(getCountText(gainSummary.getTotalMoney(), 99999)).textStyle(textStyle -> {
                textStyle.textAlignVertical(Vertical.BOTTOM).adaptiveWidth(true);
                textStyle.fontSize(5);
            }).layout(layout -> {
                layout.setHeightPercent(100);
            }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(String.valueOf(gainSummary.getTotalMoney()))), null, null, null);
            });
            shoppingCarView.addScrollViewChild(createItemInfoBox().addChildren(moneyIcon, money));
        }
    }

    public void reloadInventoryItem() {
        inventoryView.clearAllScrollViewChildren();
        AggregatedResources costSummary = AggregatedResources.getCostSummary(currentShopInfo);
        costSummary.getItems().forEach((itemStack, count) -> {
            int itemCount = getItemCount(itemStack);
            String color = itemCount >= count ? "§a" : "§c";
            Label countLabel = (Label) new Label().setText(color + getCountText(count, 999) + "§f/" + getCountText(itemCount, 999))
                    .textStyle(textStyle -> {
                        textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.BOTTOM);
                        textStyle.fontSize(4);
                    })
                    .layout(layout -> {
                        layout.setWidth(10);
                        layout.setHeightPercent(100);
                    })
                    .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                        event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(color + count + "§f/" + itemCount)), null, null, null);
                    });
            inventoryView.addScrollViewChild(createItemInfoBox().addChildren(UIElementUtil.createItemSlot(itemStack, false, true), countLabel));
        });
        if (costSummary.getTotalMoney() > 0 && minecraft.player != null) {
            String color = costSummary.getTotalMoney() <= minecraft.player.getData(ShopRegistries.MONEY).getMoney() ? "§a" : "§c";
            Label moneyIcon = (Label) new Label().setText("◎ ").textStyle(textStyle -> {
                textStyle.textAlignVertical(Vertical.CENTER);
                textStyle.fontSize(16);
            }).layout(layout -> {
                layout.setHeightPercent(100);
                layout.setWidth(16);
                layout.setMargin(YogaEdge.LEFT, 2);
            });
            Label money = (Label) new Label().setText(color + getCountText(costSummary.getTotalMoney(), 99999)).textStyle(textStyle -> {
                textStyle.textAlignVertical(Vertical.BOTTOM).adaptiveWidth(true);
                textStyle.fontSize(5);
            }).layout(layout -> {
                layout.setHeightPercent(100);
            }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(color + costSummary.getTotalMoney())), null, null, null);
            });
            inventoryView.addScrollViewChild(createItemInfoBox().addChildren(moneyIcon, money));
        }

    }

    public UIElement createMerchant(MerchantInfo merchantInfo, int index) {
        UIElement merchant = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeight(20);
            layout.setGap(YogaGutter.ALL, 6);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setPadding(YogaEdge.HORIZONTAL, 5);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        merchant.getStyle().backgroundTexture(LIGHT_BACKGROUND_RECT);
        Label id = (Label) new Label().setText(String.valueOf(index + 1)).textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
        }).layout(layout -> {
            layout.setHeightPercent(100);
        });

        UIElement uiElement = new UIElement().layout(layout -> {
            layout.setWidthPercent(25);
            layout.setHeightPercent(100);
            layout.setGap(YogaGutter.ALL, 5);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        UIElement rightArrowIcon = new UIElement().style(style -> style.backgroundTexture(RIGHT_ARROW)).layout(layout -> {
            layout.setWidth(16);
            layout.setHeight(16);
        });
        ItemSlot resultItemSlot = (ItemSlot) UIElementUtil.createItemSlot(merchantInfo.getItemResult(), false, true).setId("itemResult" + index);

        merchant.addChildren(id);

        switch (selectedCategory.getShopType()) {
            case ITEM_FOR_ITEM -> {
                ItemStack itemA = merchantInfo.getItemA();
                ItemSlot itemASlot = (ItemSlot) UIElementUtil.createItemSlot(itemA, false, true).setId("itemA" + index);
                ItemStack itemB = merchantInfo.getItemB();
                ItemSlot itemBSlot = (ItemSlot) UIElementUtil.createItemSlot(itemB, false, true).setId("itemB" + index);
                uiElement.addChildren(itemASlot, itemBSlot);
                merchant.addChildren(uiElement, rightArrowIcon, resultItemSlot);
            }
            case CURRENCY -> {
                Label money = (Label) new Label().setText("◎" + merchantInfo.getMoney()).textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
                    textStyle.fontSize(8);
                }).layout(layout -> {
                    layout.setHeightPercent(100);
                });
                uiElement.getLayout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
                uiElement.getLayout().setWidthPercent(50);
                UIElement moneyUI = new UIElement().layout(layout -> {
                    layout.setWidthPercent(40);
                    layout.setHeightPercent(100);
                    layout.setJustifyContent(YogaJustify.CENTER);
                    layout.setAlignItems(YogaAlign.CENTER);
                }).addChild(money);

                UIElement itemUI = new UIElement().layout(layout -> {
                    layout.setWidthPercent(40);
                    layout.setHeightPercent(100);
                    layout.setJustifyContent(YogaJustify.CENTER);
                    layout.setAlignItems(YogaAlign.CENTER);
                }).addChild(resultItemSlot);
                switch (merchantInfo.getTradeType()) {
                    case BUY -> uiElement.addChildren(moneyUI, rightArrowIcon, itemUI);
                    case SELL -> uiElement.addChildren(itemUI, rightArrowIcon, moneyUI);
                }
                merchant.addChildren(uiElement);
            }
        }
        Button redButton = new Button().setText("-").setOnClick(event -> {
            if (merchantInfo.getStage() <= currentShopInfo.getStage()) {
                merchantInfo.setBuyCount(Math.max(0, (int) merchantInfo.getBuyCount() - 1));
                reloadShoppingItem();
                reloadInventoryItem();
            }
        });
        NumberConfigurator countConfigurator = new NumberConfigurator("", merchantInfo::getBuyCount, count -> {
            merchantInfo.setBuyCount(count);
            reloadShoppingItem();
            reloadInventoryItem();
        }, 0, true);
        countConfigurator.setRange(0, Integer.MAX_VALUE);
        countConfigurator.layout(layout -> {
            switch (selectedCategory.getShopType()) {
                case ITEM_FOR_ITEM -> {
                    layout.setWidth(35);
                }
                case CURRENCY -> {
                    layout.setWidth(30);
                }
            }
        });
        countConfigurator.inlineContainer.getStyle().backgroundTexture(LIGHT_BACKGROUND_RECT);
        Button addButton = new Button().setText("+").setOnClick(event -> {
            if (merchantInfo.getStage() <= currentShopInfo.getStage()) {
                merchantInfo.setBuyCount(Math.min(Integer.MAX_VALUE, (int) merchantInfo.getBuyCount() + 1));
                reloadShoppingItem();
                reloadInventoryItem();
            }
        });
        if (merchantInfo.getStage() > currentShopInfo.getStage()) {
            countConfigurator.textField.setWheelDur(0);
            countConfigurator.textField.setActive(false);
        }
        UIElement LockIcon = new UIElement().style(style -> style.backgroundTexture(LOCK)).layout(layout -> {
            layout.setWidth(16);
            layout.setHeight(16);
        }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = new HoverTooltips(List.of(Component.translatable("viscript_shop.ui.stage.lock", merchantInfo.getStage())), null, null, null);
        });

        merchant.addChildren(new UIElement().layout(layout -> {
            layout.setGap(YogaGutter.ALL, 2);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setHeightPercent(100);
        }).addChildren(redButton, countConfigurator, addButton));

        if (merchantInfo.getStage() > currentShopInfo.getStage()) merchant.addChildren(LockIcon);

        return merchant;
    }

    public void setItemCount(ItemStack itemStack, int count) {
        ItemStack copy = itemStack.copy();
        copy.setCount(1);
        this.playerItems.put(copy, count);
    }

    public int getItemCount(ItemStack itemStack) {
        for (ItemStack item : this.playerItems.keySet()) {
            if (ItemStack.isSameItemSameComponents(itemStack, item)) {
                return this.playerItems.get(item);
            }
        }
        return 0;
    }

    public void removeItemCount(ItemStack itemStack, int count) {
        for (ItemStack item : this.playerItems.keySet()) {
            if (ItemStack.isSameItemSameComponents(itemStack, item)) {
                this.playerItems.put(item, this.playerItems.get(item) - count);
                return;
            }
        }
    }

    private String getCountText(int count, int max) {
        return count <= max ? count + "" : max + "+";
    }

    private UIElement createItemInfoBox() {
        return new UIElement().layout(layout -> {
            layout.setWidthPercent(50);
            layout.setHeight(20);
            layout.setJustifyContent(YogaJustify.FLEX_START);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
        });
    }
}
