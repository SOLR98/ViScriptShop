package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class ShopHelper {
    private final static Map<String, ShopInfo> CACHE = new HashMap<>();
    public static final String SHOP_PATH = "viscript_shop/shop";
    //缓存的商店信息
    public static ShopInfo cacheShopInfo;

    public static int clearCache() {
        var count = CACHE.size();
        CACHE.clear();
        return count;
    }

    @Nullable
    public static ShopInfo getShop(String shopLocation) {
        return getShop(shopLocation, true);
    }


    public static ShopInfo getShop(String shopLocation, boolean useCache) {
        return useCache ? CACHE.getOrDefault(shopLocation, loadShop(shopLocation)) : loadShop(shopLocation);
    }

    private static ShopInfo loadShop(String shopLocation) {
        if (shopLocation.startsWith("\"")) shopLocation = shopLocation.substring(1);
        if (shopLocation.endsWith("\"")) shopLocation = shopLocation.substring(0, shopLocation.length() - 1);
        File file = new File(LDLib2.getAssetsDir(), SHOP_PATH + "/" + shopLocation + Shop.SUFFIX);
        CompoundTag compoundTag;
        if (!file.exists()) return null;
        try (var inputStream = Files.newInputStream(file.toPath())) {
            compoundTag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            compoundTag = new CompoundTag();
        }
        ShopInfo shopInfo = new ShopInfo();
        shopInfo.deserializeNBT(Platform.getFrozenRegistry(), compoundTag);
        CACHE.put(shopLocation, shopInfo);
        return shopInfo;
    }
}
