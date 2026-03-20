package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopClientEvent;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.components.PlayerHeadElement;
import com.viscriptshop.gui.components.SceneToggleBuilder;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.c2s.BuyMerchantPayload;
import com.viscriptshop.network.c2s.GetItemCountC2SPayload;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.SimpleItemStackFilter;
import com.viscriptshop.util.UIElementUtil;
import com.viscriptshop.util.ViScriptShopClientUtil;
import dev.vfyjxf.taffy.style.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
    private final UIElement centerPanel;
    private final UIElement headPanel;
    public SearchComponent<ItemStack> searchComponent;
    private final Toggle currencyLayoutToggle;

    private final IGuiTexture DARK_BACKGROUND_RECT = Sprites.BORDER_RT0;
    private final IGuiTexture LIGHT_BACKGROUND_RECT = Sprites.RECT_RD_SOLID;
    private final SpriteTexture RIGHT_ARROW = SpriteTexture.of(ViscriptShop.formattedMod("textures/right_arrow.png"));
    private final SpriteTexture LOCK = SpriteTexture.of(ViscriptShop.formattedMod("textures/lock.png"));
    private final SpriteTexture GRID = SpriteTexture.of(ViscriptShop.formattedMod("textures/grid.png"));
    private final SpriteTexture LIST = SpriteTexture.of(ViscriptShop.formattedMod("textures/list.png"));
    private static final float CURRENCY_GRID_CARD_WIDTH = 50f;
    private static final float CURRENCY_GRID_GAP = 3f;

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
    private ItemStack searchItem = ItemStack.EMPTY;
    @Getter
    @Setter
    private String searchId = "";
    //当前模式 true为物品查询 false为序号查询
    @Getter
    @Setter
    private boolean searchMode = true;

    @Getter
    @Setter
    private boolean currencyGridLayout = false;
    private int currencyGridColumns = -1;

    public ShopUI(ShopInfo shopInfo, String title) {
        this(shopInfo, title, null, null);
    }

    public ShopUI(ShopInfo shopInfo, String title, String categoryId, String merchantId) {
        this.playerItems.clear();
        this.currentShopInfo = ShopHelper.cacheShopInfo == null ? shopInfo : ShopHelper.cacheShopInfo;
        this.currentShopInfo.setStage(shopInfo.getStage());
        if (minecraft.player != null) {
            // 根据 categoryId 查找对应分类
            if (categoryId != null && !categoryId.isEmpty()) {
                for (CategoryInfo category : this.currentShopInfo.getCategoryInfos()) {
                    if (categoryId.equals(category.getId())) {
                        selectedCategory = category;
                        break;
                    }
                }
            }
            // 如果没找到指定分类，使用第一个分类
            if (selectedCategory == null) {
                selectedCategory = this.currentShopInfo.getCategoryInfos().getFirst();
            }

            // 根据 merchantId 查找对应商品的索引
            if (merchantId != null && !merchantId.isEmpty()) {
                for (int i = 0; i < selectedCategory.getMerchants().size(); i++) {
                    MerchantInfo merchant = selectedCategory.getMerchants().get(i);
                    if (merchantId.equals(merchant.getId())) {
                        this.searchId = String.valueOf(i + 1);
                        this.searchMode = false;
                        break;
                    }
                }
            }

            RPCPacketDistributor.rpcToServer(GetItemCountC2SPayload.GET_ITEM_COUNT, selectedCategory);
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
                new Label().setText("◎ " + ViScriptShopClientUtil.getMoney(minecraft.player)).textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
                }).layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(50);
                }).addEventListener(UIEvents.TICK, event -> {
                    ((Label) event.currentElement).setText("◎ " + ViScriptShopClientUtil.getMoney(minecraft.player));
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
        this.centerPanel = center;
        center.getStyle().backgroundTexture(DARK_BACKGROUND_RECT);
        UIElement head = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(10);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
        });
        this.headPanel = head;
        Label centerTitle = (Label) new Label().setText("viscript_shop.ui.topTitle").textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
        }).layout(layout -> {
            layout.heightPercent(100);
        });
        //物品输入框
        searchComponent = UIElementUtil.createItemStackSearchComponentConfigurator("", this::getSearchItem, search -> {
            this.searchItem = search;
            reloadMerchants();
        }, getCategoryItems()).searchComponent;
        searchComponent.layout(layout -> {
            layout.width(70);
        });
        //序号输入框
        StringConfigurator idInput = (StringConfigurator) new StringConfigurator("", this::getSearchId, search -> {
            if (search.chars().allMatch(Character::isDigit)) {
                this.searchId = search;
                reloadMerchants();
            }
        }, searchId, true)
                .layout(layout -> layout.width(20)).setDisplay(TaffyDisplay.NONE);
        idInput.textField.textFieldStyle(textStyle -> textStyle.placeholder(Component.empty()));
        Toggle toggle = (Toggle) new SceneToggleBuilder(this::isSearchMode, this::setSearchMode)
                .icon(new ItemStackTexture(Items.GRASS_BLOCK), SpriteTexture.of(ViscriptShop.formattedMod("textures/id.png")))
                .build()
                .setOnToggleChanged(isOn -> {
                    reloadMerchants();
                    searchComponent.setDisplay(isOn ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
                    idInput.setDisplay(isOn ? TaffyDisplay.NONE : TaffyDisplay.FLEX);
                })
                .addEventListener(UIEvents.TICK, event -> {
                    event.target.getStyle().tooltips(Component.translatable(searchMode ? "viscript_shop.ui.searchMode.item" : "viscript_shop.ui.searchMode.id"));
                })
                .layout(layout -> {
                    layout.width(16);
                    layout.height(16);
                });

        Toggle layoutToggle = (Toggle) new SceneToggleBuilder(this::isCurrencyGridLayout, this::setCurrencyGridLayout)
                .icon(GRID, LIST)
                .build()
                .setOnToggleChanged(isOn -> {
                    setCurrencyGridLayout(isOn);
                    reloadMerchants();
                })
                .layout(layout -> {
                    layout.width(16);
                    layout.height(16);
                });
        this.currencyLayoutToggle = layoutToggle;
        updateCurrencyLayoutToggleState();

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
            layout.gapAll(5);
        }).addChildren(centerTitle, searchComponent, idInput, toggle, layoutToggle), new UIElement().layout(layout -> {
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
        merchantsView.viewPort.addEventListener(UIEvents.LAYOUT_CHANGED, event -> updateCurrencyGridColumns());

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
            RPCPacketDistributor.rpcToServer(BuyMerchantPayload.BUY_MERCHANT, this.currentShopInfo, costSummary, gainSummary);
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
                            RPCPacketDistributor.rpcToServer(GetItemCountC2SPayload.GET_ITEM_COUNT, selectedCategory);
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
        updateCurrencyLayoutToggleState();
        configureMerchantsContainerLayout();

        // 重新添加所有商品
        for (int i = 0; i < selectedCategory.getMerchants().size(); i++) {
            MerchantInfo merchantInfo = selectedCategory.getMerchants().get(i);
            //商品上锁样式：隐藏
            if (currentShopInfo.getLockedMerchantVisibility().equals(ShopInfo.LockedMerchantVisibility.HIDDEN) && getMerchantLockReason(merchantInfo) != null) {
                continue;
            }
            //搜索筛选 物品筛选和序号筛选
            if (this.searchMode) {
                if (!this.searchItem.isEmpty()) {
                    boolean isMatch = ItemStack.isSameItemSameComponents(merchantInfo.getItemResult(), this.searchItem) ||
                            ItemStack.isSameItemSameComponents(merchantInfo.getItemA(), this.searchItem) ||
                            ItemStack.isSameItemSameComponents(merchantInfo.getItemB(), this.searchItem);
                    if (!isMatch) {
                        continue;
                    }
                }
            } else {
                if (!this.searchId.isEmpty()) {
                    try {
                        int targetIndex = Integer.parseInt(this.searchId);
                        if ((i + 1) != targetIndex) {
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
            if (isCurrencyGridActive()) {
                merchantsView.addScrollViewChild(createCurrencyMerchantGrid(merchantInfo, i));
            } else {
                merchantsView.addScrollViewChild(createMerchant(merchantInfo, i));
            }
        }
    }

    private boolean isCurrencyGridActive() {
        return selectedCategory != null
                && selectedCategory.getShopType() == CategoryInfo.ShopType.CURRENCY
                && currencyGridLayout;
    }

    private void updateCurrencyLayoutToggleState() {
        if (currencyLayoutToggle == null) return;

        boolean show = selectedCategory != null && selectedCategory.getShopType() == CategoryInfo.ShopType.CURRENCY;
        currencyLayoutToggle.setDisplay(show ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
        currencyLayoutToggle.getStyle().tooltips(Component.translatable(currencyGridLayout ? "viscript_shop.ui.layout.grid" : "viscript_shop.ui.layout.list"));
        currencyLayoutToggle.setValue(currencyGridLayout, false);
    }

    private void configureMerchantsContainerLayout() {
        configureCenterPanelPaddingForLayout();
        if (isCurrencyGridActive()) {
            merchantsView.viewContainer.layout(layout -> {
                layout.display(TaffyDisplay.GRID);
                layout.gridAutoFlow(GridAutoFlow.ROW);
                layout.justifyItems(AlignItems.CENTER);
                layout.alignItems(AlignItems.FLEX_START);
                layout.justifyContent(AlignContent.CENTER);
                layout.alignContent(AlignContent.FLEX_START);
                layout.gapAll(CURRENCY_GRID_GAP);
            });
            updateCurrencyGridColumns();
        } else {
            merchantsView.viewContainer.layout(layout -> {
                layout.display(TaffyDisplay.FLEX);
                layout.flexDirection(FlexDirection.COLUMN);
                layout.wrap(FlexWrap.NO_WRAP);
                layout.gapAll(5);
            });
            currencyGridColumns = -1;
        }
    }

    private void configureCenterPanelPaddingForLayout() {
        if (centerPanel == null) return;

        boolean grid = isCurrencyGridActive();
        centerPanel.layout(layout -> {
            layout.paddingHorizontal(grid ? 0 : 5);
            layout.paddingVertical(3);
        });
        if (headPanel != null) {
            headPanel.layout(layout -> layout.paddingHorizontal(grid ? 5 : 0));
        }
    }

    private void updateCurrencyGridColumns() {
        if (!isCurrencyGridActive()) return;
        if (merchantsView == null || merchantsView.viewPort == null) return;

        float available = merchantsView.viewPort.getContentWidth();
        if (available <= 1f) return;

        int cols = Math.max(1, (int) Math.floor((available + CURRENCY_GRID_GAP) / (CURRENCY_GRID_CARD_WIDTH + CURRENCY_GRID_GAP)));
        while (cols > 1) {
            float required = cols * CURRENCY_GRID_CARD_WIDTH + (cols - 1) * CURRENCY_GRID_GAP;
            if (required <= available + 0.01f) break;
            cols--;
        }
        if (cols == currencyGridColumns) return;
        currencyGridColumns = cols;

        List<TrackSizingFunction> tracks = new ArrayList<>(cols);
        for (int i = 0; i < cols; i++) {
            tracks.add(TrackSizingFunction.fixed(CURRENCY_GRID_CARD_WIDTH));
        }
        merchantsView.viewContainer.getLayout().gridTemplateColumns(new GridTemplate(tracks, List.of(), List.of()));
        merchantsView.viewContainer.markTaffyStyleDirty();
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
            String color = costSummary.getTotalMoney() <= ViScriptShopClientUtil.getMoney(minecraft.player) ? "§a" : "§c";
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
        Set<ItemStack> items = getCategoryItems();
        searchComponent.setSearchUI(new SearchComponent.ISearchUI<>() {
            @Override
            public @NotNull String resultText(@NotNull ItemStack value) {
                return value.isEmpty() ? "" : value.getHoverName().getString();
            }

            @Override
            public void onResultSelected(@Nullable ItemStack value) {
                searchItem = value;
                reloadMerchants();
            }

            @Override
            public void search(String word, IResultHandler<ItemStack> handler) {
                Collection<ItemStack> candidatesItems = items;

                if (candidatesItems == null) {
                    candidatesItems = BuiltInRegistries.ITEM.stream()
                            .map(ItemStack::new)
                            .toList();
                }

                for (ItemStack stack : candidatesItems) {
                    if (Thread.currentThread().isInterrupted()) return;

                    if (stack.isEmpty()) {
                        handler.acceptResult(stack);
                        continue;
                    }

                    if (SimpleItemStackFilter.matchItemSearch(stack, word)) {
                        handler.acceptResult(stack);
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
            layout.paddingHorizontal(4);
            layout.alignItems(AlignItems.CENTER);
        });
        merchant.getStyle().backgroundTexture(LIGHT_BACKGROUND_RECT);
        Label id = (Label) new Label().setText(String.valueOf(index + 1)).textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER);
            textStyle.fontSize(6);
        }).layout(layout -> {
            layout.width(20);
            layout.heightPercent(100);
        });

        UIElement uiElement = new UIElement().layout(layout -> {
            layout.widthPercent(20);
            layout.heightPercent(100);
            layout.gapAll(5);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
        });
        UIElement rightArrowIcon = new UIElement().style(style -> style.backgroundTexture(RIGHT_ARROW)).layout(layout -> {
            layout.width(12);
            layout.height(12);
        });
        ItemSlot resultItemSlot = (ItemSlot) UIElementUtil.createItemSlot(merchantInfo.getItemResult(), false, true).setId("itemResult" + index);
        resultItemSlot.getLayout().marginRight(2);

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
                Label money = (Label) new Label().setText("◎" + getCountText(merchantInfo.getMoney())).textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
                    textStyle.fontSize(8);
                }).layout(layout -> {
                    layout.heightPercent(100);
                }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(String.valueOf(merchantInfo.getMoney()))), null, null, null);
                });
                uiElement.getLayout().justifyContent(AlignContent.SPACE_BETWEEN);
                uiElement.getLayout().widthPercent(45);
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
            if (getMerchantLockReason(merchantInfo) == null) {
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
            if (getMerchantLockReason(merchantInfo) == null) {
                merchantInfo.setBuyCount(Math.min(Integer.MAX_VALUE, (int) merchantInfo.getBuyCount() + 1));
                reloadShoppingItem();
                reloadInventoryItem();
            }
        });
        if (getMerchantLockReason(merchantInfo) != null) {
            countConfigurator.textField.setWheelDur(0);
            countConfigurator.textField.setActive(false);
        }
        UIElement LockIcon = new UIElement().style(style -> style.backgroundTexture(LOCK)).layout(layout -> {
            layout.width(16);
            layout.height(16);
        }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            Component lockReason = getMerchantLockReason(merchantInfo);
            if (lockReason != null) {
                event.hoverTooltips = new HoverTooltips(List.of(lockReason), null, null, null);
            }
        });

        merchant.addChildren(new UIElement().layout(layout -> {
            layout.gapAll(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.heightPercent(100);
        }).addChildren(redButton, countConfigurator, addButton));

        if (getMerchantLockReason(merchantInfo) != null) merchant.addChildren(LockIcon);

        return merchant;
    }

    public UIElement createCurrencyMerchantGrid(MerchantInfo merchantInfo, int index) {
        UIElement merchant = new UIElement().layout(layout -> {
            layout.width(CURRENCY_GRID_CARD_WIDTH);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.paddingAll(3);
            layout.gapAll(2);
            layout.positionType(TaffyPosition.RELATIVE);
        });
        merchant.getStyle().backgroundTexture(LIGHT_BACKGROUND_RECT);

        Label id = (Label) new Label().setText(String.valueOf(index + 1)).textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER);
            textStyle.fontSize(8);
        }).layout(layout -> {
            layout.widthPercent(100);
            layout.height(6);
            layout.alignSelf(AlignItems.FLEX_START);
        });

        ItemSlot resultItemSlot = (ItemSlot) UIElementUtil.createItemSlot(merchantInfo.getItemResult(), false, true)
                .setId("itemResult" + index)
                .layout(layout -> {
                    layout.width(20);
                    layout.height(20);
                });

        String tradeText = merchantInfo.getTradeType().getSerializedName();
        Label tradeLabel = (Label) new Label()
                .setText(Component.translatable(tradeText))
                .textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER).fontSize(6))
                .layout(layout -> layout.widthPercent(100));

        Label priceLabel = (Label) new Label()
                .setText(Component.literal("◎" + getCountText(merchantInfo.getMoney())))
                .textStyle(textStyle -> textStyle
                        .textColor(0xFFFFAA00)
                        .textAlignHorizontal(Horizontal.CENTER)
                        .textAlignVertical(Vertical.CENTER)
                        .fontSize(8)
                )
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.marginTop(1);
                    layout.marginBottom(2);
                })
                .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(String.valueOf(merchantInfo.getMoney()))), null, null, null);
                });

        NumberConfigurator countConfigurator = new NumberConfigurator("", merchantInfo::getBuyCount, count -> {
            merchantInfo.setBuyCount(count);
            reloadShoppingItem();
            reloadInventoryItem();
        }, 0, true);
        countConfigurator.setRange(0, Integer.MAX_VALUE);
        countConfigurator.layout(layout -> layout.width(28));
        countConfigurator.inlineContainer.getStyle().backgroundTexture(LIGHT_BACKGROUND_RECT);

        if (getMerchantLockReason(merchantInfo) != null) {
            countConfigurator.textField.setWheelDur(0);
            countConfigurator.textField.setActive(false);
        }

        UIElement lockIcon = new UIElement().style(style -> style.backgroundTexture(LOCK)).layout(layout -> {
            layout.width(12);
            layout.height(12);
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.top(2);
            layout.right(2);
        }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            Component lockReason = getMerchantLockReason(merchantInfo);
            if (lockReason != null) {
                event.hoverTooltips = new HoverTooltips(List.of(lockReason), null, null, null);
            }
        });
        lockIcon.setDisplay(getMerchantLockReason(merchantInfo) == null ? TaffyDisplay.NONE : TaffyDisplay.FLEX);

        UIElement body = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(3);
        }).addChildren(resultItemSlot, tradeLabel, priceLabel);

        UIElement controls = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });

        UIElement qty = new UIElement().layout(layout -> {
            layout.gapAll(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        }).addChildren( countConfigurator);

        controls.addChildren(qty);

        merchant.addChildren(id, lockIcon, body, controls);
        return merchant;
    }

    /**
     * 判断商品是否解锁
     *
     * @param merchantInfo 商品信息
     * @return null表示已解锁，非null返回锁定原因的Component
     */
    private Component getMerchantLockReason(MerchantInfo merchantInfo) {
        // 阶段判断
        if (merchantInfo.getStage() > currentShopInfo.getStage()) {
            return Component.translatable("viscript_shop.ui.stage.lock", merchantInfo.getStage());
        }

        // 所有条件都满足，返回null表示已解锁
        return null;
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

    public Set<ItemStack> getCategoryItems() {
        Set<ItemStack> items = new HashSet<>();
        items.add(ItemStack.EMPTY);
        List<MerchantInfo> merchants = selectedCategory.getMerchants();

        for (MerchantInfo merchant : merchants) {
            if (getMerchantLockReason(merchant) == null) {
                if (selectedCategory.getShopType() == CategoryInfo.ShopType.ITEM_FOR_ITEM) {
                    addItemStackIfUnique(items, merchant.getItemA());
                    addItemStackIfUnique(items, merchant.getItemB());
                }
                addItemStackIfUnique(items, merchant.getItemResult());
            }
        }
        return items;
    }

    private void addItemStackIfUnique(Set<ItemStack> list, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        for (ItemStack existing : list) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                return;
            }
        }
        ItemStack displayStack = stack.copy();
        displayStack.setCount(1);

        list.add(displayStack);
    }
}
