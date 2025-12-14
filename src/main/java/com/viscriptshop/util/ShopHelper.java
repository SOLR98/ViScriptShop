package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.project.ShopProject;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class ShopHelper {
    private final static Map<ResourceLocation, Shop> CACHE = new HashMap<>();
    public static final String SHOP_PATH = "shop/";
    //缓存的商店项目文件
    public static ShopProject cacheShopProject;
    //缓存的商店信息
    public static ShopInfo cacheShopInfo;

    public static int clearCache() {
        var count = CACHE.size();
        CACHE.clear();
        return count;
    }

    @Nullable
    public static Shop getShop(ResourceLocation shopLocation) {
        return getShop(shopLocation, true);
    }


    @Nullable
    public static Shop getShop(ResourceLocation shopLocation, boolean useCache) {
        return useCache ? CACHE.computeIfAbsent(shopLocation, location -> loadShop(shopLocation)) : loadShop(shopLocation);
    }

    public static Shop loadShop(ResourceLocation shopLocation) {
        ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(shopLocation.getNamespace(), SHOP_PATH + shopLocation.getPath() + Shop.SUFFIX);
        try (var inputStream = Minecraft.getInstance().getResourceManager().open(resourceLocation)) {
            var tag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            var shop = new Shop();
            shop.setShopLocation(shopLocation);
            shop.deserializeNBT(Platform.getFrozenRegistry(), tag);
            return shop;
        } catch (Exception ignored) {
            return null;
        }
    }
}
