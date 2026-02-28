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
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
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
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShopUI extends UIElement {
    Minecraft minecraft = Minecraft.getInstance();
    //ui
    public ScrollerView categoryView = new ScrollerView();
    public ScrollerView merchantsView = new ScrollerView();
    public ScrollerView shoppingCarView = new ScrollerView();
    public ScrollerView inventoryView = new ScrollerView();
    public SearchComponent<Item> searchComponent;

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
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).addEventListener(UIEvents.TICK, event -> NeoForge.EVENT_BUS.post(new ShopClientEvent.Tick(this)));
        UIElement root = new UIElement();
        root.layout((layout) -> {
            layout.widthPercent(90);
            layout.heightPercent(87);
            layout.gapAll(3);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        });
        this.addChildren(root);
        //左
        UIElement left = new UIElement().layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(22);
            layout.gapAll(3);
            layout.flexDirection(FlexDirection.COLUMN);
        });
        UIElement leftTop = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
        }).style(style -> {
            style.backgroundTexture(DARK_BACKGROUND_RECT);
        }).addChild(new Label().setText("viscript_shop.data.shop.categoryInfos").textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
        }).layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }));

        categoryView.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(80);
        }).addEventListener(UIEvents.TICK, event -> {
            reloadCategoryList();
        });
        categoryView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        categoryView.viewContainer.layout(layout -> {
            layout.gapColumn(5);
            layout.paddingAll(3);
            layout.flexDirection(FlexDirection.COLUMN);
        });
        UIElement leftCenter = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(74);
        }).style(style -> {
            style.backgroundTexture(DARK_BACKGROUND_RECT);
        }).addChild(categoryView);

        UIElement leftBottom = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(13);
            layout.flexDirection(FlexDirection.COLUMN);
        }).style(style -> {
            style.backgroundTexture(DARK_BACKGROUND_RECT);
        }).addChildren(
                new Label().setText("viscript_shop.ui.balance").textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.BOTTOM);
                }).layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(50);
                }),
                new Label().setText("◎ " + minecraft.player.getData(ShopRegistries.MONEY).getMoney()).textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
                }).layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(50);
                }).addEventListener(UIEvents.TICK, event -> {
                    ((Label) event.currentElement).setText("◎ " + minecraft.player.getData(ShopRegistries.MONEY).getMoney());
                })
        );

        left.addChildren(leftTop, leftCenter, leftBottom);
        //中
        UIElement center = new UIElement().layout(layout -> {
            layout.widthPercent(55);
            layout.heightPercent(100);
            layout.paddingHorizontal(5);
            layout.paddingVertical(3);
            layout.flexDirection(FlexDirection.COLUMN);
        });
        center.getStyle().backgroundTexture(DARK_BACKGROUND_RECT);
        UIElement head = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
        });
        Label centerTitle = (Label) new Label().setText("viscript_shop.ui.topTitle").textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
        }).layout(layout -> {
            layout.heightPercent(100);
        });
        searchComponent = UIElementUtil.createItemSearchComponentConfigurator("", this::getSearch, this::setSearch, getCategoryItems()).searchComponent;
        searchComponent.layout(layout -> {
            layout.marginLeft(5);
            layout.width(70);
        }).addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            reloadMerchants();
        });
        Label stageLabel = (Label) new Label().setText(Component.translatable("viscript_shop.ui.stage", this.currentShopInfo.getStage())).textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
        }).layout(layout -> {
            layout.heightPercent(100);
            layout.marginRight(10);
        });
        head.addChildren(new UIElement().layout(layout -> {
            layout.heightPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
        }).addChildren(centerTitle, searchComponent), new UIElement().layout(layout -> {
            layout.heightPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
        }).addChildren(stageLabel, new PlayerHeadElement()));

        merchantsView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });

        merchantsView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);

        merchantsView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(5);
        });

        reloadMerchants();

        center.addChildren(head, merchantsView);
        //右
        UIElement right = new UIElement().layout(layout -> {
            layout.widthPercent(25);
            layout.heightPercent(100);
            layout.gapAll(3);
            layout.flexDirection(FlexDirection.COLUMN);
        });
        UIElement rightTop = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
        }).style(style -> {
            style.backgroundTexture(DARK_BACKGROUND_RECT);
        }).addChild(new Label().setText(title)
                .textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
                })
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                }));

        UIElement rightBottom = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(5);
            layout.gapAll(2);
        });
        rightBottom.getStyle().backgroundTexture(DARK_BACKGROUND_RECT);

        shoppingCarView.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(35);
        });
        shoppingCarView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        shoppingCarView.viewPort.getLayout().paddingAll(3);
        reloadShoppingItem();

        inventoryView.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(35);
        });
        inventoryView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        inventoryView.viewPort.getLayout().paddingAll(3);
        reloadInventoryItem();

        Button clearButton = (Button) new Button().setText("viscript_shop.button.clear").setOnClick(event -> {
            currentShopInfo.getCategoryInfos().forEach(categoryInfo -> {
                categoryInfo.getMerchants().forEach(merchantInfo -> merchantInfo.setBuyCount(0));
            });
            reloadShoppingItem();
            reloadInventoryItem();
        }).layout(layout -> {
            layout.widthPercent(40);
        });

        Button tsButton = (Button) new Button().setText("viscript_shop.button.ts").setOnClick(event -> {
            ShopHelper.cacheShopInfo = this.currentShopInfo;
            if (minecraft.screen != null) minecraft.screen.onClose();
        }).layout(layout -> {
            layout.widthPercent(40);
        });

        Button buyButton = (Button) new Button().setText("viscript_shop.button.buy").setOnClick(event -> {
            AggregatedResources costSummary = AggregatedResources.getCostSummary(this.currentShopInfo);
            AggregatedResources gainSummary = AggregatedResources.getGainSummary(this.currentShopInfo);
            if (costSummary.isEmpty() || gainSummary.isEmpty()) {
                Message.warn("viscript_shop.message.shoppingCar.empty", this);
                return;
            }
            minecraft.player.connection.send(new BuyMerchantPayload(this.currentShopInfo, costSummary, gainSummary));
        }).layout(layout -> {
            layout.widthPercent(100);
        });

        rightBottom.addChildren(new Label().setText("viscript_shop.ui.shoppingCar").textStyle(textStyle -> textStyle.adaptiveHeight(true)), shoppingCarView,
                new Label().setText("viscript_shop.ui.inventory").textStyle(textStyle -> textStyle.adaptiveHeight(true)), inventoryView,
                new UIElement().layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.SPACE_BETWEEN);
                }).addChildren(tsButton, clearButton), buyButton);

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
            if (!this.search.isEmpty() && !this.search.equals(Items.AIR.toString()) &&
                    !merchantInfo.getItemResult().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(this.search))) &&
                    !merchantInfo.getItemA().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(this.search))) &&
                    !merchantInfo.getItemB().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(this.search)))) {
                continue;
            }
            merchantsView.addScrollViewChild(createMerchant(merchantInfo, i));
        }
    }

    public void reloadShoppingItem() {
        shoppingCarView.clearAllScrollViewChildren();

        AggregatedResources gainSummary = AggregatedResources.getGainSummary(currentShopInfo);
        gainSummary.getItems().forEach((itemStack, count) -> {
            Label countLabel = (Label) new Label().setText(getCountText(count))
                    .textStyle(textStyle -> {
                        textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.BOTTOM);
                        textStyle.fontSize(5);
                    })
                    .layout(layout -> {
                        layout.width(10);
                        layout.heightPercent(100);
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
                layout.width(16);
                layout.heightPercent(100);
                layout.marginLeft(2);
            });
            Label money = (Label) new Label().setText(getCountText(gainSummary.getTotalMoney())).textStyle(textStyle -> {
                textStyle.textAlignVertical(Vertical.BOTTOM).adaptiveWidth(true);
                textStyle.fontSize(5);
            }).layout(layout -> {
                layout.heightPercent(100);
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
            Label countLabel = (Label) new Label().setText(color + getCountText(count) + "§f/" + getCountText(itemCount))
                    .textStyle(textStyle -> {
                        textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.BOTTOM);
                        textStyle.fontSize(4);
                    })
                    .layout(layout -> {
                        layout.width(10);
                        layout.heightPercent(100);
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
                layout.heightPercent(100);
                layout.width(16);
                layout.marginLeft(2);
            });
            Label money = (Label) new Label().setText(color + getCountText(costSummary.getTotalMoney())).textStyle(textStyle -> {
                textStyle.textAlignVertical(Vertical.BOTTOM).adaptiveWidth(true);
                textStyle.fontSize(5);
            }).layout(layout -> {
                layout.heightPercent(100);
            }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(color + costSummary.getTotalMoney())), null, null, null);
            });
            inventoryView.addScrollViewChild(createItemInfoBox().addChildren(moneyIcon, money));
        }

    }

    public void reloadSearchComponent() {
        searchComponent.setSearchUI(new SearchComponent.ISearchUI<>() {
            @Override
            public @NotNull String resultText(@NotNull Item value) {
                return BuiltInRegistries.ITEM.getKey(value).toString();
            }

            @Override
            public void onResultSelected(@Nullable Item value) {
                BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                        search != null ? search : Items.AIR.toString()
                ));
            }

            @Override
            public void search(String word, IResultHandler<Item> searchHandler) {
                String lowerWord = word.toLowerCase();
                Set<Item> candidatesItems = getCategoryItems();
                for (Item item : candidatesItems) {
                    ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                    if (Thread.currentThread().isInterrupted()) return;
                    if (key.toString().toLowerCase().contains(lowerWord) || Component.translatable(item.getDescriptionId()).getString().toLowerCase().contains(lowerWord)) {
                        searchHandler.acceptResult(item);
                    }
                }
            }
        });
    }

    public UIElement createMerchant(MerchantInfo merchantInfo, int index) {
        UIElement merchant = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(20);
            layout.gapAll(6);
            layout.flexDirection(FlexDirection.ROW);
            layout.paddingHorizontal(5);
            layout.alignItems(AlignItems.CENTER);
        });
        merchant.getStyle().backgroundTexture(LIGHT_BACKGROUND_RECT);
        Label id = (Label) new Label().setText(String.valueOf(index + 1)).textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
        }).layout(layout -> {
            layout.heightPercent(100);
        });

        UIElement uiElement = new UIElement().layout(layout -> {
            layout.widthPercent(25);
            layout.heightPercent(100);
            layout.gapAll(5);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
        });
        UIElement rightArrowIcon = new UIElement().style(style -> style.backgroundTexture(RIGHT_ARROW)).layout(layout -> {
            layout.width(16);
            layout.height(16);
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
                    layout.heightPercent(100);
                });
                uiElement.getLayout().justifyContent(AlignContent.SPACE_BETWEEN);
                uiElement.getLayout().widthPercent(50);
                UIElement moneyUI = new UIElement().layout(layout -> {
                    layout.widthPercent(40);
                    layout.heightPercent(100);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.CENTER);
                }).addChild(money);

                UIElement itemUI = new UIElement().layout(layout -> {
                    layout.widthPercent(40);
                    layout.heightPercent(100);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.CENTER);
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
                    layout.width(35);
                }
                case CURRENCY -> {
                    layout.width(30);
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
            layout.width(16);
            layout.height(16);
        }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = new HoverTooltips(List.of(Component.translatable("viscript_shop.ui.stage.lock", merchantInfo.getStage())), null, null, null);
        });

        merchant.addChildren(new UIElement().layout(layout -> {
            layout.gapAll(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.heightPercent(100);
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

    private String getCountText(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(java.util.Locale.US, "%.1fk", count / 1000.0);
        return String.format(java.util.Locale.US, "%.1fm", count / 1000000.0);
    }

    private UIElement createItemInfoBox() {
        return new UIElement().layout(layout -> {
            layout.widthPercent(50);
            layout.height(20);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
        });
    }

    public Set<Item> getCategoryItems() {
        Set<Item> items = new HashSet<>();
        List<MerchantInfo> merchants = selectedCategory.getMerchants();
        for (MerchantInfo merchant : merchants) {
            if (merchant.getStage() <= currentShopInfo.getStage()) {
                items.add(merchant.getItemA().getItem());
                items.add(merchant.getItemB().getItem());
                items.add(merchant.getItemResult().getItem());
            }
        }
        return items;
    }
}
