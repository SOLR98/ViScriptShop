package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.BooleanConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptshop.ViscriptShop;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

//商店信息
@Data
public class ShopInfo implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, ShopInfo> STREAM_CODEC;
    public static final Codec<ShopInfo> CODEC;

    @Configurable(name = "viscript_shop.data.shop.name", tips = "viscript_shop.data.shop.name.tip")
    private String name = "";
    @Configurable(name = "viscript_shop.data.shop.stage", tips = "viscript_shop.data.shop.stage.tip")
    private int stage = 0;
    @Persisted
    private boolean isQuickOpening = false;
    @Configurable(name = "viscript_shop.data.shop.lockedMerchantVisibility")
    private LockedMerchantVisibility lockedMerchantVisibility = LockedMerchantVisibility.SHOW_WITH_LOCK;
    @Persisted
    @ReadOnlyManaged(serializeMethod = "writeCategoryInfo", deserializeMethod = "readCategoryInfo")
    private List<CategoryInfo> categoryInfos = new ArrayList<>();

    static {
        CODEC = PersistedParser.createCodec(ShopInfo::new);
        STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        if (ViscriptShop.isFtbLibraryLoaded()) {
            BooleanConfigurator isQuickOpeningConfigurator = new BooleanConfigurator("viscript_shop.data.shop.isQuickOpening", this::isQuickOpening, this::setQuickOpening, isQuickOpening, true);
            isQuickOpeningConfigurator.setTips("viscript_shop.data.shop.isQuickOpening.tip");
            father.addConfigurators(isQuickOpeningConfigurator);
        }
    }

    private Tag writeCategoryInfo(List<CategoryInfo> value) {
        return IntTag.valueOf(value.size());
    }

    private List<CategoryInfo> readCategoryInfo(IntTag tag) {
        List<CategoryInfo> list = new ArrayList<>();
        for (int i = 0; i < tag.getAsInt(); i++) {
            list.add(new CategoryInfo());
        }
        return list;
    }

    @SkipPersistedValue(field = "isQuickOpening")
    public boolean skipIsQuickOpening(boolean value) {
        return !ViscriptShop.isFtbLibraryLoaded();
    }

    @Getter
    @AllArgsConstructor
    public enum LockedMerchantVisibility implements StringRepresentable {
        SHOW_WITH_LOCK("viscript_shop.data.shop.lockedItemVisibility.show_with_lock"),
        HIDDEN("viscript_shop.data.shop.lockedItemVisibility.hidden");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
