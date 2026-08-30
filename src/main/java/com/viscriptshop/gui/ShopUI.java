package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.viscriptshop.Config;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopClientEvent;
import com.viscriptshop.gui.components.CustomCountElement;
import com.viscriptshop.gui.components.DiscountInfoElement;
import com.viscriptshop.gui.components.GiftTagElement;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.components.PlayerHeadElement;
import com.viscriptshop.gui.components.SceneToggleBuilder;
import com.viscriptshop.gui.components.theme.ShopButton;
import com.viscriptshop.gui.components.theme.ShopScrollerView;
import com.viscriptshop.gui.components.theme.ShopTheme;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.DiscountResult;
import com.viscriptshop.gui.data.MerchantFlagGroup;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.MerchantItemInfo;
import com.viscriptshop.gui.data.PromotionRule;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.layout.GlassDarkShopUiLayout;
import com.viscriptshop.gui.layout.GrayCatShopUiLayout;
import com.viscriptshop.gui.layout.ShopUiElements;
import com.viscriptshop.gui.layout.ShopUiLayout;
import com.viscriptshop.network.c2s.BuyMerchantPayload;
import com.viscriptshop.network.c2s.GetItemCountC2SPayload;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.TradePriceCalculator;
import com.viscript_lib.util.CountTextUtil;
import com.viscript_lib.util.item.SimpleItemStackFilter;
import com.viscriptshop.util.UIElementUtil;
import com.viscriptshop.util.ViScriptShopClientUtil;
import dev.vfyjxf.taffy.style.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShopUI extends UIElement {
    Minecraft minecraft = Minecraft.getInstance();
    //主题样式
    private final ShopTheme theme = ShopTheme.current();
    //ui
    public ScrollerView categoryView = new ShopScrollerView(theme);
    public ScrollerView merchantsView = new ShopScrollerView(theme);
    public ScrollerView shoppingCarView = new ShopScrollerView(theme);
    public ScrollerView inventoryView = new ShopScrollerView(theme);
    public SearchComponent<ItemStack> searchComponent;
    private final Toggle currencyLayoutToggle;
    private final UIElement shopUiShell;
    private final ShopUiLayout shopUiLayout;

    private final IGuiTexture LIST_BACKGROUND = theme.merchantList();
    private final IGuiTexture GRID_BACKGROUND = theme.merchantGrid();
    private final SpriteTexture RIGHT_ARROW = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/right_arrow.png"));
    private final SpriteTexture LOCK = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/lock.png"));
    private final SpriteTexture COIN = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/coin.png"));
    private static final float CURRENCY_GRID_GAP = 3f;

    //data
    //玩家身上对应物品的数量
    public List<AggregatedResources.ItemEntry> playerItems = new ArrayList<>();
    //打开的商店信息
    public ShopInfo currentShopInfo;
    //商店文件位置（用于购买后保存数据）
    private String shopLocation;
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
    private int discountRefreshTick = 0;

    public ShopUI(String shopLocation, ShopInfo shopInfo, String title) {
        this(shopLocation, shopInfo, title, null, null);
    }

    public ShopUI(String shopLocation, ShopInfo shopInfo, String title, String categoryId, String merchantId) {
        this.shopLocation = shopLocation;
        this.playerItems.clear();
        this.currentShopInfo = initCurrentShopInfo(shopInfo);
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
        }).addEventListener(UIEvents.TICK, event -> {
            NeoForge.EVENT_BUS.post(new ShopClientEvent.Tick(this));
            // 折扣周期刷新:每 40 tick 重算折后价并重建商品行(折扣现算,原价存商店数据)
            if (++discountRefreshTick >= 40) {
                discountRefreshTick = 0;
                reloadMerchants();
            }
        });

        ShopUiElements elements = createUiElements(title);
        this.searchComponent = elements.itemSearch();
        this.currencyLayoutToggle = elements.currencyLayoutToggle();
        this.shopUiLayout = theme.isGrayCatWorkshop()
                ? new GrayCatShopUiLayout(elements.itemSearch())
                : GlassDarkShopUiLayout.INSTANCE;
        this.shopUiShell = shopUiLayout.build(theme, elements);
        this.addChild(shopUiShell);

        updateCurrencyLayoutToggleState();
        reloadMerchants();
        reloadShoppingItem();
        reloadInventoryItem();
    }

    private ShopUiElements createUiElements(String title) {
        categoryView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        }).addEventListener(UIEvents.TICK, event -> reloadCategoryList());
        categoryView.verticalScroller.layout(layout -> layout.marginRight(3));
        categoryView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        categoryView.viewContainer.layout(layout -> {
            layout.gapColumn(5);
            layout.paddingAll(3);
            layout.flexDirection(FlexDirection.COLUMN);
        });

        merchantsView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        merchantsView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(5);
        });
        merchantsView.viewPort.addEventListener(UIEvents.LAYOUT_CHANGED, event -> updateCurrencyGridColumns());

        shoppingCarView.viewContainer.layout(layout -> {
            layout.paddingLeft(6);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        shoppingCarView.viewPort.getLayout().paddingAll(3);
        shoppingCarView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);

        inventoryView.viewContainer.layout(layout -> {
            layout.paddingLeft(6);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        inventoryView.viewPort.getLayout().paddingAll(3);
        inventoryView.viewPort.setId("shop_consumption_panel");
        inventoryView.viewPort.getStyle().backgroundTexture(theme.consumptionPanel());

        Label categoryTitle = (Label) new Label().setText("viscript_shop.data.shop.categoryInfos");
        Label shopTitle = (Label) new Label().setText(title);
        UIElement balanceIcon = new UIElement().style(style -> style.backgroundTexture(GuiTextureGroup.of(
                theme.balanceIconBackground(),
                COIN.copy().scale(theme.balanceIconScale())
        )));
        Label balanceValue = (Label) new Label()
                .addEventListener(UIEvents.TICK, event ->
                        ((Label) event.currentElement).setText(String.valueOf(
                                ViScriptShopClientUtil.getMoney(minecraft.player)
                        )));

        UIElement searchIcon = new UIElement().setId("shop_search_icon").style(style ->
                style.backgroundTexture(GuiTextureGroup.of(
                        theme.searchIconBackground(),
                        theme.searchIcon().copy().scale(theme.searchIconScale())
                ))
        );
        SearchComponent<ItemStack> itemSearch = UIElementUtil.createItemStackSearchComponentConfigurator(
                "",
                this::getSearchItem,
                search -> {
                    this.searchItem = search;
                    reloadMerchants();
                },
                getCategoryItems()
        ).searchComponent;
        itemSearch.setId("shop_item_search");
        itemSearch.getStyle().backgroundTexture(theme.searchField());
        itemSearch.searchStyle(style -> style.focusOverlay(IGuiTexture.EMPTY));
        itemSearch.setDisplay(searchMode ? TaffyDisplay.FLEX : TaffyDisplay.NONE);

        StringConfigurator idSearch = (StringConfigurator) new StringConfigurator(
                "",
                this::getSearchId,
                search -> {
                    if (search.chars().allMatch(Character::isDigit)) {
                        this.searchId = search;
                        reloadMerchants();
                    }
                },
                searchId,
                true
        ).setId("shop_id_search");
        idSearch.setDisplay(searchMode ? TaffyDisplay.NONE : TaffyDisplay.FLEX);
        idSearch.getStyle().backgroundTexture(theme.searchField());
        idSearch.textField.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        idSearch.textField.textFieldStyle(textStyle -> {
            textStyle.placeholder(Component.empty());
            textStyle.focusOverlay(IGuiTexture.EMPTY);
        });

        Toggle searchModeToggle = new SceneToggleBuilder(this::isSearchMode, this::setSearchMode)
                .icon(
                        new ItemStackTexture(Items.GRASS_BLOCK),
                        SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/id.png"))
                )
                .baseTexture(theme.toggleBase())
                .hoverTexture(theme.toggleHover())
                .build();
        searchModeToggle.setId("shop_search_mode_toggle");
        searchModeToggle.setOnToggleChanged(isOn -> {
            reloadMerchants();
            itemSearch.setDisplay(isOn ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
            idSearch.setDisplay(isOn ? TaffyDisplay.NONE : TaffyDisplay.FLEX);
        });
        searchModeToggle.addEventListener(UIEvents.TICK, event -> event.target.getStyle().tooltips(
                        Component.translatable(searchMode
                                ? "viscript_shop.ui.searchMode.item"
                                : "viscript_shop.ui.searchMode.id")
                ));

        SpriteTexture gridIcon = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/grid.png"));
        SpriteTexture listIcon = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/list.png"));
        Toggle layoutToggle = new SceneToggleBuilder(
                this::isCurrencyGridLayout,
                this::setCurrencyGridLayout
        )
                .icon(gridIcon, listIcon)
                .baseTexture(theme.toggleBase())
                .hoverTexture(theme.toggleHover())
                .build();
        layoutToggle.setId("shop_currency_layout_toggle");
        layoutToggle.setOnToggleChanged(isOn -> {
            setCurrencyGridLayout(isOn);
            reloadMerchants();
        });

        UIElement playerHead = new UIElement().addChild(
                new PlayerHeadElement().layout(layout -> layout.marginRight(5))
        );

        ShopButton clearButton = ShopButton.other(theme);
        clearButton.setId("shop_clear_button");
        clearButton.setText("viscript_shop.button.clear").setOnClick(event -> {
                    currentShopInfo.getCategoryInfos().forEach(categoryInfo ->
                            categoryInfo.getMerchants().forEach(merchantInfo ->
                                    merchantInfo.setBuyCount(0)
                            )
                    );
                    reloadShoppingItem();
                    reloadInventoryItem();
                });
        ShopButton stashButton = ShopButton.other(theme);
        stashButton.setId("shop_stash_button");
        stashButton.setText("viscript_shop.button.ts").setOnClick(event -> {
                    ShopHelper.cacheShopInfo = this.currentShopInfo;
                    if (minecraft.screen != null) {
                        minecraft.screen.onClose();
                    }
                });
        ShopButton buyButton = ShopButton.buying(theme);
        buyButton.setId("shop_buy_button");
        buyButton.setText("viscript_shop.button.buy").setOnClick(event -> buy());

        Label shoppingCartTitle = (Label) new Label().setText("viscript_shop.ui.shoppingCar");
        Label consumptionTitle = (Label) new Label().setText("viscript_shop.ui.inventory");

        return new ShopUiElements(
                categoryView,
                merchantsView,
                shoppingCarView,
                inventoryView,
                categoryTitle,
                shopTitle,
                balanceIcon,
                balanceValue,
                searchIcon,
                itemSearch,
                idSearch,
                searchModeToggle,
                layoutToggle,
                playerHead,
                shoppingCartTitle,
                consumptionTitle,
                stashButton,
                clearButton,
                buyButton
        );
    }

    private void buy() {
        AggregatedResources costSummary = AggregatedResources.getCostSummary(this.currentShopInfo);
        AggregatedResources gainSummary = AggregatedResources.getGainSummary(this.currentShopInfo);
        if (costSummary.isEmpty() || gainSummary.isEmpty()) {
            Message.warn("viscript_shop.message.shoppingCar.empty", this);
            return;
        }
        int maxItems = Config.maxShopUiGiveItemsPerPurchase.get();
        if (maxItems >= 0 && gainSummary.getTotalItemCount() > maxItems) {
            Message.error(Component.translatable(
                    "viscript_shop.message.buy.too_many_items",
                    maxItems
            ).getString(), this);
            return;
        }
        RPCPacketDistributor.rpcToServer(
                BuyMerchantPayload.BUY_MERCHANT,
                this.shopLocation,
                costSummary,
                gainSummary
        );
    }
    private ShopInfo initCurrentShopInfo(ShopInfo shopInfo) {
        if (ShopHelper.cacheShopInfo == null) {
            return shopInfo;
        }
        copyCachedBuyCounts(ShopHelper.cacheShopInfo, shopInfo);
        return shopInfo;
    }

    private void copyCachedBuyCounts(ShopInfo cachedShopInfo, ShopInfo freshShopInfo) {
        for (CategoryInfo freshCategory : freshShopInfo.getCategoryInfos()) {
            CategoryInfo cachedCategory = cachedShopInfo.getCategoryInfos().stream()
                    .filter(category -> category.getId().equals(freshCategory.getId()))
                    .findFirst()
                    .orElse(null);
            if (cachedCategory == null) continue;

            for (MerchantInfo freshMerchant : freshCategory.getMerchants()) {
                cachedCategory.getMerchants().stream()
                        .filter(merchant -> merchant.getId().equals(freshMerchant.getId()))
                        .findFirst()
                        .ifPresent(cachedMerchant -> {
                            int buyCount = cachedMerchant.getBuyCount().intValue();
                            int stock = freshMerchant.getStock();
                            freshMerchant.setBuyCount(stock >= 0 ? Math.min(buyCount, stock) : buyCount);
                        });
            }
        }
    }

    @Override
    public void initScreen(int screenWidth, int screenHeight) {
        super.initScreen(screenWidth, screenHeight);
        Size layoutSize = getAutoGuiScaledSize(Size.of(screenWidth, screenHeight));
        shopUiLayout.initScreen(shopUiShell, layoutSize);
        applyAutoGuiScaleTransform();
    }

    public static Size getAutoGuiScaledSize(Size screenSize) {
        float scale = getAutoGuiScaleFactor();
        if (scale <= 0f) return screenSize;

        return Size.of(
                Math.max(1, Math.round(screenSize.getWidth() / scale)),
                Math.max(1, Math.round(screenSize.getHeight() / scale))
        );
    }

    private void applyAutoGuiScaleTransform() {
        float scale = getAutoGuiScaleFactor();
        // 让固定尺寸控件在任意 GUI Scale 下都保持 Auto 缩放时的视觉大小。
        transform(transform -> transform.pivot(0.5f, 0.5f).scale(scale));
    }

    private static float getAutoGuiScaleFactor() {
        Minecraft minecraft = Minecraft.getInstance();

        var window = minecraft.getWindow();
        double currentScale = window.getGuiScale();
        if (currentScale <= 0d) return 1f;

        int autoScale = window.calculateScale(0, minecraft.isEnforceUnicode());
        return Math.max(1f, (float) (autoScale / currentScale));
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
                    theme.categoryDefault(),
                    theme.categorySelected(),
                    theme.categoryEntryHeight()
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
            if (currentShopInfo.getLockedMerchantVisibility().equals(ShopInfo.LockedMerchantVisibility.HIDDEN) && isMerchantLocked(merchantInfo)) {
                continue;
            }
            //搜索筛选 物品筛选和序号筛选
            if (this.searchMode) {
                if (!this.searchItem.isEmpty()) {
                    boolean isMatch = ItemStack.isSameItemSameComponents(merchantInfo.getItemResult(), this.searchItem) ||
                            merchantInfo.getItemAMatchRule().matches(merchantInfo.getItemA(), this.searchItem) ||
                            merchantInfo.getItemBMatchRule().matches(merchantInfo.getItemB(), this.searchItem);
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

    private void updateCurrencyGridColumns() {
        if (!isCurrencyGridActive()) return;
        if (merchantsView == null || merchantsView.viewPort == null) return;

        float available = merchantsView.viewPort.getContentWidth();
        if (available <= 1f) return;

        float cardWidth = theme.merchantGridWidth();
        int cols = Math.max(1, (int) Math.floor((available + CURRENCY_GRID_GAP) / (cardWidth + CURRENCY_GRID_GAP)));
        while (cols > 1) {
            float required = cols * cardWidth + (cols - 1) * CURRENCY_GRID_GAP;
            if (required <= available + 0.01f) break;
            cols--;
        }
        if (cols == currencyGridColumns) return;
        currencyGridColumns = cols;

        List<TrackSizingFunction> tracks = new ArrayList<>(cols);
        for (int i = 0; i < cols; i++) {
            tracks.add(TrackSizingFunction.fixed(cardWidth));
        }
        merchantsView.viewContainer.getLayout().gridTemplateColumns(new GridTemplate(tracks, List.of(), List.of()));
        merchantsView.viewContainer.markTaffyStyleDirty();
    }

    public void reloadShoppingItem() {
        shoppingCarView.clearAllScrollViewChildren();

        AggregatedResources gainSummary = AggregatedResources.getGainSummary(currentShopInfo);
        gainSummary.getItems().forEach((itemStack, count) -> {
            Label countLabel = (Label) new Label().setText(CountTextUtil.formatCount(count))
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
            UIElement moneyIcon = new UIElement().layout(layout -> {
                layout.width(16);
                layout.height(16);
                layout.marginLeft(2);
            }).style(style -> style.backgroundTexture(COIN));
            Label money = (Label) new Label().setText(CountTextUtil.formatCount(gainSummary.getTotalMoney())).textStyle(textStyle -> {
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
        costSummary.getItemEntries().forEach(itemEntry -> {
            ItemStack itemStack = itemEntry.getItemStack();
            int count = itemEntry.getCount();
            int itemCount = getItemCount(itemEntry);
            String color = itemCount >= count ? "§a" : "§c";
            Label countLabel = (Label) new Label().setText(color + CountTextUtil.formatCount(count) + "§f/" + CountTextUtil.formatCount(itemCount))
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
            UIElement moneyIcon = new UIElement().layout(layout -> {
                layout.width(16);
                layout.height(16);
                layout.marginLeft(2);
            }).style(style -> style.backgroundTexture(COIN));
            Label money = (Label) new Label().setText(color + CountTextUtil.formatCount(costSummary.getTotalMoney())).textStyle(textStyle -> {
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

    private static final float ROW_HEIGHT = 18;
    private static final float ROW_INPUT_RIGHT = 167;
    private static final float ROW_MINUS_X = 153;
    private static final float ROW_PLUS_X = 167;
    private static final int INPUT_MIN_WIDTH = 24;
    private static final int INPUT_MAX_WIDTH = 46;

    /**
     * 交易条目行构建(单一渲染路径:全部商品槽经 {@link MerchantSlotElement} 自绘)。
     * 布局按比例动态:序号/成本区/箭头/结果/买赠靠左,数量组/锁靠右(flex 自适应列宽)。
     */
    public UIElement createMerchant(MerchantInfo merchantInfo, int index) {
        UIElement merchant = createRowContainer();

        UIElement leftGroup = new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.flex(1);
        });
        leftGroup.addChild(createRowIndex(index));
        switch (selectedCategory.getShopType()) {
            case ITEM_FOR_ITEM -> createItemForItemRow(leftGroup, merchantInfo, index);
            case CURRENCY -> createCurrencyRow(leftGroup, merchantInfo, index);
        }
        merchant.addChild(leftGroup);

        // 右侧组:数量组 + 锁(始终靠右,距行最右 3px,左侧 3px 为动态间隔右缘)
        BuyCountControls controls = createBuyCountControls(merchantInfo);
        UIElement rightGroup = new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(1);
            layout.marginLeft(3);
            layout.marginRight(3);
        });
        rightGroup.addChildren(controls.minusButton(), controls.countInput(), controls.plusButton());

        // 锁定:数量组隐藏 + 锁图标显示(互斥)
        if (isMerchantLocked(merchantInfo)) {
            controls.minusButton().setVisible(false);
            controls.countInput().setVisible(false);
            controls.plusButton().setVisible(false);
            rightGroup.addChild(createLockIcon(merchantInfo));
        }
        merchant.addChild(rightGroup);

        // 库存悬浮提示或遮罩
        int stock = merchantInfo.getStock();
        if (stock > 0) {
            addStockTooltip(merchant, stock);
        } else if (stock == 0) {
            merchant.addChild(createStockOverlay());
        }

        return merchant;
    }

    /** 以物换物行:成本槽(自绘,含折扣/买赠) + 箭头 + 结果槽 + 买赠槽(靠左流式) */
    private void createItemForItemRow(UIElement leftGroup, MerchantInfo merchantInfo, int index) {
        DiscountResult resultA = TradePriceCalculator.calculate(minecraft.player, currentShopInfo, selectedCategory,
                merchantInfo, PromotionRule.CostSlot.ITEM_A, merchantInfo.getItemA());
        DiscountResult resultB = TradePriceCalculator.calculate(minecraft.player, currentShopInfo, selectedCategory,
                merchantInfo, PromotionRule.CostSlot.ITEM_B, merchantInfo.getItemB());
        UIElement slotA = createDiscountedSlot(merchantInfo.getItemAInfo(), resultA, "itemA", index, 14);
        slotA.getLayout().marginRight(14);
        // A/B 槽间距动态(flex 吸收剩余空间),范围 14~28px
        UIElement spacer = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.minWidth(14);
            layout.maxWidth(28);
            layout.height(1);
        });
        UIElement slotB = createDiscountedSlot(merchantInfo.getItemBInfo(), resultB, "itemB", index, 14);
        // 箭头与 B 槽间距不小于 16px
        slotB.getLayout().marginRight(16);
        UIElement arrow = createRightArrow();
        // 箭头右侧(对结果槽)间距不小于 2px
        arrow.getLayout().marginRight(2);
        UIElement resultSlot = createResultSlot(merchantInfo, index);
        resultSlot.getLayout().marginRight(3);
        leftGroup.addChildren(slotA, spacer, slotB, arrow, resultSlot);
        // 买赠槽始终占位(无买赠时为空槽)
        leftGroup.addChild(createBonusSlot(merchantInfo, index));
    }

    /** 货币行:货币金额与结果槽按交易类型排布(flex) */
    private void createCurrencyRow(UIElement leftGroup, MerchantInfo merchantInfo, int index) {
        Label money = (Label) new Label().setText("◎" + CountTextUtil.formatCount(merchantInfo.getMoney()))
                .textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
                    textStyle.fontSize(8);
                    textStyle.textColor(0xFFFFAA00);
                })
                .layout(layout -> {
                    layout.heightPercent(100);
                })
                .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(String.valueOf(merchantInfo.getMoney()))), null, null, null);
                });
        UIElement resultItemSlot = createResultSlot(merchantInfo, index);
        UIElement arrow = createRightArrow();
        arrow.getLayout().marginRight(3);
        switch (merchantInfo.getTradeType()) {
            case BUY -> {
                money.getLayout().marginRight(3);
                leftGroup.addChildren(money, arrow, resultItemSlot);
            }
            case SELL -> {
                resultItemSlot.getLayout().marginRight(3);
                leftGroup.addChildren(resultItemSlot, arrow, money);
            }
        }
    }

    private record BuyCountControls(Button minusButton, NumberConfigurator countInput, Button plusButton) {
    }

    private UIElement createRowContainer() {
        return new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(ROW_HEIGHT);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
        }).style(style -> style.backgroundTexture(LIST_BACKGROUND));
    }

    private UIElement createRowIndex(int index) {
        return (Label) new Label().setText(String.valueOf(index + 1))
                .textStyle(textStyle -> textStyle
                        .textAlignHorizontal(Horizontal.CENTER)
                        .textAlignVertical(Vertical.CENTER)
                        .fontSize(6))
                .layout(layout -> {
                    layout.width(16);
                    layout.height(16);
                    layout.marginLeft(1);
                    layout.marginRight(6);
                });
    }

    private UIElement createRightArrow() {
        return new UIElement().style(style -> style.backgroundTexture(RIGHT_ARROW))
                .layout(layout -> {
                    layout.width(12);
                    layout.height(10);
                });
    }

    private UIElement createResultSlot(MerchantInfo merchantInfo, int index) {
        return UIElementUtil.createMerchantSlotDisplay(merchantInfo.getItemResultInfo(),
                        merchantInfo.getItemResultCount(), true, null, null, false, 14)
                .setId("itemResult" + index)
                .layout(layout -> {
                    layout.width(14);layout.height(14);
                });
    }

    /**
     * 买赠展示槽:始终占位 14×14(无买赠时为空槽,不渲染内容),命中 BUY_GET 规则时显示赠品图标与"赠"字。
     */
    private UIElement createBonusSlot(MerchantInfo merchantInfo, int index) {
        if ((int) merchantInfo.getBuyCount() <= 0) {
            return emptyBonusSlot(index);
        }
        var bonusList = TradePriceCalculator.calculateBonus(minecraft.player, currentShopInfo,
                selectedCategory, merchantInfo, (int) merchantInfo.getBuyCount());
        if (bonusList == null || bonusList.isEmpty()) {
            return emptyBonusSlot(index);
        }

        var first = bonusList.getFirst();
        int giftCount = bonusList.stream().mapToInt(DiscountResult.BonusDetail::getCount).sum();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("viscript_shop.ui.bonus.tag", String.valueOf(giftCount)));
        for (DiscountResult.BonusDetail bonus : bonusList) {
            lines.add(Component.translatable("viscript_shop.ui.discount.detail",
                    Component.translatable(bonus.getSource().isEmpty()
                            ? "viscript_shop.discount.source.external" : bonus.getSource()),
                    String.valueOf(bonus.getCount())));
        }
        HoverTooltips bonusTooltips = new HoverTooltips(lines, null, null, null);
        return UIElementUtil.createMerchantSlotDisplay(first.getItem(), giftCount, false, bonusTooltips, true, 14)
                .setId("itemBonus" + index)
                .layout(layout -> {
                    layout.width(14);layout.height(14);
                });
    }

    /** 空买赠位:14×14 占位,不渲染内容 */
    private UIElement emptyBonusSlot(int index) {
        return UIElementUtil.createMerchantSlotDisplay(ItemStack.EMPTY, 0, false, null, false, 14)
                .setId("itemBonus" + index)
                .layout(layout -> {
                    layout.width(14);layout.height(14);
                });
    }

    private BuyCountControls createBuyCountControls(MerchantInfo merchantInfo) {
        final Button[] minusHolder = new Button[1];
        final NumberConfigurator[] inputHolder = new NumberConfigurator[1];

        minusHolder[0] = ShopButton.other(theme).setText("-");
        minusHolder[0].layout(layout -> {
            layout.width(14);layout.height(14);
        });
        Button plusButton = ShopButton.other(theme).setText("+");
        plusButton.layout(layout -> {
            layout.width(14);layout.height(14);
        });
        inputHolder[0] = new NumberConfigurator("", merchantInfo::getBuyCount, count -> {
            merchantInfo.setBuyCount(count);
            reloadShoppingItem();
            reloadInventoryItem();
            updateInputLayout(inputHolder[0], minusHolder[0]);
            updateStockButtons(merchantInfo, minusHolder[0], plusButton);
        }, 0, true);
        inputHolder[0].layout(layout -> {
            layout.height(12);
        });
        inputHolder[0].inlineContainer.getStyle().backgroundTexture(LIST_BACKGROUND);

        minusHolder[0].setOnClick(event -> {
            if ((int) merchantInfo.getBuyCount() > 0) {
                merchantInfo.setBuyCount((int) merchantInfo.getBuyCount() - 1);
                reloadShoppingItem();
                reloadInventoryItem();
                updateInputLayout(inputHolder[0], minusHolder[0]);
                updateStockButtons(merchantInfo, minusHolder[0], plusButton);
            }
        });
        plusButton.setOnClick(event -> {
            int stock = merchantInfo.getStock();
            int maxCount = stock >= 0 ? stock : Integer.MAX_VALUE;
            if ((int) merchantInfo.getBuyCount() < maxCount) {
                merchantInfo.setBuyCount((int) merchantInfo.getBuyCount() + 1);
                reloadShoppingItem();
                reloadInventoryItem();
                updateInputLayout(inputHolder[0], minusHolder[0]);
                updateStockButtons(merchantInfo, minusHolder[0], plusButton);
            }
        });

        // 输入框变宽:组靠右,flex 自动将 - 按钮向左推(顺序从左到右)
        inputHolder[0].textField.setTextResponder(text -> updateInputLayout(inputHolder[0], minusHolder[0]));

        applyStockRestrictions(merchantInfo, inputHolder[0], minusHolder[0], plusButton);
        updateInputLayout(inputHolder[0], minusHolder[0]);
        return new BuyCountControls(minusHolder[0], inputHolder[0], plusButton);
    }

    private void updateInputLayout(NumberConfigurator countInput, Button minusButton) {
        String text = countInput.textField.getValue();
        int len = text == null ? 0 : text.length();
        int width = Mth.clamp(len * 6 + 12, INPUT_MIN_WIDTH, INPUT_MAX_WIDTH);
        countInput.getLayout().width(width);
    }

    private UIElement createLockIcon(MerchantInfo merchantInfo) {
        return new UIElement().style(style -> style.backgroundTexture(LOCK))
                .layout(layout -> {
                    layout.width(16);
                    layout.height(16);
                })
                .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    List<Component> lockReasons = getMerchantLockReasons(merchantInfo);
                    if (!lockReasons.isEmpty()) {
                        event.hoverTooltips = new HoverTooltips(lockReasons, null, null, null);
                    }
                });
    }

    /**
     * 创建成本槽:槽内显示原价数量(有折扣时画删除线),折扣信息(折后价+折率)由
     * {@link DiscountInfoElement} 渲染在槽右侧;无折扣时保持原样。
     */
    private UIElement createDiscountedSlot(MerchantItemInfo itemInfo, DiscountResult result, String id, int index,
                                           float size) {
        boolean hasDiscount = result != null && result.hasDiscount();
        long displayCount = hasDiscount ? result.getBaseCount() : itemInfo.getCount();
        HoverTooltips tooltips = hasDiscount ? buildDiscountTooltips(itemInfo, result) : null;
        return UIElementUtil.createMerchantSlotDisplay(itemInfo, displayCount, !hasDiscount, tooltips, result, false, size)
                .setId(id + index)
                .layout(layout -> {
                    layout.width(size);
                    layout.height(size);
                });
    }

    /**
     * 组装折扣槽悬浮框:原版物品行(数量限制在堆叠上限内)+ 原价→折后价(缩写)+ 总折率 + 明细(规则/事件来源)。
     */
    private HoverTooltips buildDiscountTooltips(MerchantItemInfo itemInfo, DiscountResult result) {
        List<Component> lines = new ArrayList<>();
        ItemStack stack = itemInfo.getItem().copy();
        stack.setCount((int) Math.min(result.getFinalCount(), stack.getMaxStackSize()));
        TooltipFlag flag = minecraft.options.advancedItemTooltips
                ? TooltipFlag.ADVANCED
                : TooltipFlag.NORMAL;
        lines.addAll(stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, flag));
        lines.add(Component.empty());
        lines.add(Component.translatable("viscript_shop.ui.discount.compare",
                String.valueOf(result.getBaseCount()),
                String.valueOf(result.getFinalCount())));
        lines.add(Component.translatable("viscript_shop.ui.discount.rate",
                DiscountInfoElement.formatRate(result.getRate())));
        for (DiscountResult.DiscountDetail detail : result.getDetails()) {
            lines.add(Component.translatable("viscript_shop.ui.discount.detail",
                    Component.translatable(detail.getSource()),
                    DiscountInfoElement.formatRate(detail.getRate())));
        }
        return new HoverTooltips(lines, null, null, null);
    }

    public UIElement createCurrencyMerchantGrid(MerchantInfo merchantInfo, int index) {        UIElement merchant = new UIElement()
                .setId("shop_merchant_grid_" + index)
                .addClass("shop-merchant-grid-card")
                .layout(layout -> {
                    layout.width(theme.merchantGridWidth());
                    if (theme.merchantGridHeight() > 0) {
                        layout.height(theme.merchantGridHeight());
                    }
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.alignItems(AlignItems.CENTER);
                    layout.justifyContent(AlignContent.FLEX_START);
                    layout.paddingAll(5);
                    layout.gapAll(2);
                    layout.positionType(TaffyPosition.RELATIVE);
                });
        merchant.getStyle().backgroundTexture(GRID_BACKGROUND);

        Label id = (Label) new Label().setText(String.valueOf(index + 1)).textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER);
            textStyle.fontSize(8);
        }).layout(layout -> {
            layout.widthPercent(100);
            layout.height(6);
            layout.alignSelf(AlignItems.FLEX_START);
        });

        UIElement resultItemSlot = UIElementUtil.createMerchantSlotDisplay(
                        merchantInfo.getItemResultInfo(),
                        merchantInfo.getItemResultCount(),
                        true, null, null, false, 20
                )
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
                .setText(Component.literal("◎" + CountTextUtil.formatCount(merchantInfo.getMoney())))
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
        countConfigurator.setId("shop_merchant_grid_count_" + index);
        countConfigurator.layout(layout -> layout.width(28));
        countConfigurator.inlineContainer.getStyle().backgroundTexture(
                theme.isGrayCatWorkshop() ? theme.searchField() : GRID_BACKGROUND
        );

        // 应用库存限制
        int stock = merchantInfo.getStock();
        if (stock >= 0) {
            // 有限库存
            countConfigurator.setRange(0, stock);
            if (stock == 0) {
                countConfigurator.textField.setWheelDur(0);
                countConfigurator.textField.setActive(false);
            }
        } else {
            // 无限库存
            countConfigurator.setRange(0, Integer.MAX_VALUE);
        }

        if (isMerchantLocked(merchantInfo)) {
            countConfigurator.textField.setWheelDur(0);
            countConfigurator.textField.setActive(false);
        }

        // 添加库存悬浮提示或遮罩
        if (stock > 0) {
            // 库存 > 0：添加悬浮提示显示库存
            addStockTooltip(merchant, stock);
        } else if (stock == 0) {
            // 库存 = 0：添加半透明遮罩
            merchant.addChildren(createStockOverlay());
        }

        UIElement lockIcon = new UIElement().style(style -> style.backgroundTexture(LOCK)).layout(layout -> {
            layout.width(12);
            layout.height(12);
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.top(2);
            layout.right(2);
        }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            List<Component> lockReasons = getMerchantLockReasons(merchantInfo);
            if (!lockReasons.isEmpty()) {
                event.hoverTooltips = new HoverTooltips(lockReasons, null, null, null);
            }
        });
        lockIcon.setDisplay(isMerchantLocked(merchantInfo) ? TaffyDisplay.FLEX : TaffyDisplay.NONE);

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
            if (theme.isGrayCatWorkshop()) {
                layout.top(-3);
            }
        });

        UIElement qty = new UIElement().layout(layout -> {
            layout.gapAll(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        }).addChildren(countConfigurator);

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
    private boolean isMerchantLocked(MerchantInfo merchantInfo) {
        return !getMerchantLockReasons(merchantInfo).isEmpty();
    }

    private List<Component> getMerchantLockReasons(MerchantInfo merchantInfo) {
        if (minecraft.player == null) {
            return List.of();
        }

        return MerchantFlagGroup.getLockTooltips(merchantInfo.getFlagGroupMode(), merchantInfo.getFlagGroups(), ViScriptShopClientUtil.getStageFlags(minecraft.player));
    }

    /**
     * 创建库存遮罩层（当库存为0时显示）
     */
    private UIElement createStockOverlay() {
        UIElement overlay = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.top(0);
            layout.left(0);
        });
        overlay.getStyle().backgroundTexture(new ColorRectTexture(0x80000000)); // 半透明黑色
        overlay.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = new HoverTooltips(
                    List.of(Component.translatable("viscript_shop.message.stock.out").withStyle(ChatFormatting.RED)),
                    null, null, null
            );
        });
        return overlay;
    }

    /**
     * 创建库存悬浮提示（当库存>0时显示）
     */
    private void addStockTooltip(UIElement element, int stock) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = new HoverTooltips(
                    List.of(Component.translatable("viscript_shop.message.stock.available", stock).withStyle(ChatFormatting.YELLOW)),
                    null, null, null
            );
        });
    }

    /**
     * 应用库存限制到输入框和按钮
     */
    private void applyStockRestrictions(MerchantInfo merchantInfo, NumberConfigurator countConfigurator, Button removeButton, Button addButton) {
        int stock = merchantInfo.getStock();

        // 库存 < 0：无限库存，不限制
        if (stock < 0) {
            countConfigurator.setRange(0, Integer.MAX_VALUE);
            return;
        }

        // 库存 = 0：禁用所有控件
        if (stock == 0) {
            countConfigurator.setRange(0, 0);
            countConfigurator.textField.setWheelDur(0);
            countConfigurator.textField.setActive(false);
            if (removeButton != null) removeButton.setActive(false);
            if (addButton != null) addButton.setActive(false);
            return;
        }

        // 库存 > 0：设置范围并控制按钮状态
        countConfigurator.setRange(0, stock);

        // 根据当前购买数量更新按钮状态
        updateStockButtons(merchantInfo, removeButton, addButton);
    }

    /**
     * 更新按钮状态（根据库存和当前购买数量）
     */
    private void updateStockButtons(MerchantInfo merchantInfo, Button removeButton, Button addButton) {
        int stock = merchantInfo.getStock();
        int currentCount = (int) merchantInfo.getBuyCount();

        if (stock < 0) {
            // 无限库存，按钮始终可用（除非其他锁定原因）
            if (removeButton != null) removeButton.setActive(true);
            if (addButton != null) addButton.setActive(true);
            return;
        }

        if (removeButton != null) {
            removeButton.setActive(currentCount > 0);
        }

        if (addButton != null) {
            addButton.setActive(currentCount < stock);
        }
    }

    public void setItemCount(AggregatedResources.ItemEntry itemEntry) {
        AggregatedResources.ItemEntry copy = itemEntry.copyWithCount(itemEntry.getCount());
        for (int i = 0; i < this.playerItems.size(); i++) {
            AggregatedResources.ItemEntry existing = this.playerItems.get(i);
            if (existing.canMerge(copy.getItemStack(), copy.getMatchRule())) {
                this.playerItems.set(i, copy);
                return;
            }
        }
        this.playerItems.add(copy);
    }

    public int getItemCount(AggregatedResources.ItemEntry itemEntry) {
        for (AggregatedResources.ItemEntry item : this.playerItems) {
            if (item.canMerge(itemEntry.getItemStack(), itemEntry.getMatchRule())) {
                return item.getCount();
            }
        }
        return 0;
    }

    public void removeItemCount(AggregatedResources.ItemEntry itemEntry) {
        for (AggregatedResources.ItemEntry item : this.playerItems) {
            if (item.canMerge(itemEntry.getItemStack(), itemEntry.getMatchRule())) {
                item.setCount(item.getCount() - itemEntry.getCount());
                return;
            }
        }
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
            if (!isMerchantLocked(merchant)) {
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
