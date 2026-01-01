package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

//商店信息
@Data
public class ShopInfo implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, ShopInfo> STREAM_CODEC;
    public static final Codec<ShopInfo> CODEC;

    @Configurable(name = "viscript_shop.data.shop.name",tips = "viscript_shop.data.shop.name.tip")
    private String name = "";
    @Configurable(name = "viscript_shop.data.shop.stage", tips = "viscript_shop.data.shop.stage.tip")
    private int stage = 0;
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
        ArrayConfiguratorGroup<CategoryInfo> categoryConfigArrayConfiguratorGroup = new ArrayConfiguratorGroup<>("viscript_shop.data.shop.categoryInfos", false,
                () -> new ArrayList<>(this.getCategoryInfos()),
                (getter, setter) -> {
                    CategoryInfo instance = getter.get();
                    return instance != null ? instance.createDirectConfigurator() : new Configurator();
                }, true);
        categoryConfigArrayConfiguratorGroup.setAddDefault(CategoryInfo::new);
        categoryConfigArrayConfiguratorGroup.setOnUpdate(list -> {
            List<CategoryInfo> origin = this.getCategoryInfos();
            origin.clear();
            origin.addAll(list);
        });
        father.addConfigurators(categoryConfigArrayConfiguratorGroup);
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
}
