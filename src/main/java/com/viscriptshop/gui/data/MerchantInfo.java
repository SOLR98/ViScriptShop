package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantInfo implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, MerchantInfo> STREAM_CODEC;
    public static final Codec<MerchantInfo> CODEC;

    //以物换物商店
    @Configurable(name = "viscript_shop.data.merchant.itemA")
    private ItemStack itemA = ItemStack.EMPTY;
    @Configurable(name = "viscript_shop.data.merchant.itemB")
    private ItemStack itemB = ItemStack.EMPTY;
    //通用货币商店
    @Configurable(name = "viscript_shop.data.merchant.money")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int money = 0;
    @Configurable(name = "viscript_shop.data.merchant.tradeType")
    private TradeType tradeType = TradeType.BUY;
    //通用参数
    @Configurable(name = "viscript_shop.data.merchant.itemResult")
    private ItemStack itemResult = ItemStack.EMPTY;
    @Configurable(name = "viscript_shop.data.merchant.xp")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int xp = 0;
    @Configurable(name = "viscript_shop.data.merchant.stage", tips = "viscript_shop.data.merchant.stage.tip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int stage = 0;
    //ui用参数
    private Number buyCount = 0;

    static {
        CODEC = PersistedParser.createCodec(MerchantInfo::new);
        STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    }

    public Configurator createConfigurator(CategoryInfo.ShopType shopType) {
        ConfiguratorGroup group = new ConfiguratorGroup();
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(YogaDisplay.NONE);
        buildConfigurator(group);
        List<Configurator> configurators = new ArrayList<>(group.getConfigurators());
        group.removeAllConfigurators();
        //以物换物商店
        List<Configurator> itemForItemConfigurators = configurators.subList(0, 2);
        //通用货币商店
        List<Configurator> currencyConfigurators = configurators.subList(2, 4);
        //通用参数
        List<Configurator> commonConfigurators = configurators.subList(4, configurators.size());
        switch (shopType) {
            case ITEM_FOR_ITEM -> itemForItemConfigurators.forEach(group::addConfigurator);
            case CURRENCY -> currencyConfigurators.forEach(group::addConfigurator);
        }
        commonConfigurators.forEach(group::addConfigurator);
        return group;
    }

    public MerchantInfo itemA(ItemStack itemA) {
        this.itemA = itemA;
        return this;
    }

    public MerchantInfo itemB(ItemStack itemB) {
        this.itemB = itemB;
        return this;
    }

    public MerchantInfo itemResult(ItemStack itemResult) {
        this.itemResult = itemResult;
        return this;
    }

    public MerchantInfo money(int money) {
        this.money = money;
        return this;
    }

    public MerchantInfo tradeType(TradeType tradeType) {
        this.tradeType = tradeType;
        return this;
    }

    public MerchantInfo xp(int xp) {
        this.xp = xp;
        return this;
    }

    public MerchantInfo stage(int stage) {
        this.stage = stage;
        return this;
    }

    @Getter
    @AllArgsConstructor
    public enum TradeType implements StringRepresentable {
        BUY("viscript_shop.data.merchant.tradeType.buy"),
        SELL("viscript_shop.data.merchant.tradeType.sell");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
