package com.viscriptshop.gui.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ShopSavedData extends SavedData {
    private final Map<String, ShopInfo> shopInfoMap = new HashMap<>();

    public static SavedData.Factory<ShopSavedData> factory() {
        return new SavedData.Factory<>(
                ShopSavedData::new,
                ShopSavedData::fromNbt
        );
    }

    public void addShopMerchant(String shop, int categoryIndex, MerchantInfo merchantInfo) {
        ShopInfo shopInfo = shopInfoMap.get(shop);
        shopInfo.getCategoryInfos().get(categoryIndex).getMerchants().add(merchantInfo);
        setDirty();
    }

    public ShopInfo getShopInfo(String shop) {
        setDirty();
        return shopInfoMap.get(shop);
    }

    public void setShopInfo(String shop, ShopInfo shopInfo) {
        shopInfoMap.put(shop, shopInfo);
        setDirty();
    }

    public void resetShopInfo(String shop) {
        shopInfoMap.remove(shop);
        setDirty();
    }

    public void reset() {
        shopInfoMap.clear();
        setDirty();
    }

    public static ShopSavedData fromNbt(CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        ShopSavedData shopSavedData = new ShopSavedData();
        for (String shop : nbt.getAllKeys()) {
            ShopInfo shopInfo = new ShopInfo();
            shopInfo.deserializeNBT(provider, nbt.getCompound(shop));
            shopSavedData.shopInfoMap.put(shop, shopInfo);
        }
        return shopSavedData;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        for (Map.Entry<String, ShopInfo> entry : shopInfoMap.entrySet()) {
            compoundTag.put(entry.getKey(), entry.getValue().serializeNBT(provider));
        }
        return compoundTag;
    }
}
