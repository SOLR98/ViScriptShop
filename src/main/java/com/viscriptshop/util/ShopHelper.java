package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.command.ShopCommand;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.project.ShopProject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

@ParametersAreNonnullByDefault
public class ShopHelper {
    public static final String SHOP_PATH = "%s/shop/".formatted(ViscriptShop.MOD_ID);
    //缓存的商店项目文件
    public static ShopProject cacheShopProject;
    //缓存的商店信息
    public static ShopInfo cacheShopInfo;

    public static Set<String> scanShopFiles() {
        ShopCommand.shopFilesPath.clear();
        return FileScanner.scanFilesWithSuffix(new File(LDLib2.getAssetsDir(), SHOP_PATH), Shop.SUFFIX);
    }

    public static Shop loadShop(String path) {
        File file = new File(LDLib2.getAssetsDir(), SHOP_PATH + path + Shop.SUFFIX);
        if (!file.exists()) {
            ViscriptShop.LOGGER.error("shop file {} not found", path);
            return null;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(new FileInputStream(file), NbtAccounter.unlimitedHeap());
            var shop = new Shop();
            shop.setPath(path);
            shop.deserializeNBT(Platform.getFrozenRegistry(), tag);
            return shop;
        } catch (Exception ignored) {
            return null;
        }
    }
}
