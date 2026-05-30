package com.viscriptshop.gui.data;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Shop implements INBTSerializable<CompoundTag> {
    public static final String SUFFIX = ".shop";
    public static final int VERSION = 3;
    public static final String VERSION_TAG = "version_num";
    public ShopInfo shopInfo;

    public Shop() {
        shopInfo = new ShopInfo();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return serializeRuntimeNBT(provider, shopInfo);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        deserializeRuntimeNBT(provider, tag, true);
    }

    public void deserializeRuntimeNBT(HolderLookup.Provider provider, CompoundTag tag, boolean migrateLegacy) {
        shopInfo = deserializeRuntimeInfo(provider, tag, migrateLegacy);
    }

    public static CompoundTag serializeRuntimeNBT(HolderLookup.Provider provider, ShopInfo shopInfo) {
        var data = shopInfo.serializeNBT(provider);
        data.putInt(VERSION_TAG, VERSION);
        return data;
    }

    public static ShopInfo deserializeRuntimeInfo(HolderLookup.Provider provider, CompoundTag tag, boolean migrateLegacy) {
        CompoundTag shopTag = unwrapRuntimeTag(tag);
        if (migrateLegacy) {
            shopTag = migrateShopData(shopTag, getRuntimeDataVersion(tag, shopTag));
        }

        ShopInfo shopInfo = new ShopInfo();
        shopInfo.deserializeNBT(provider, shopTag);
        return shopInfo;
    }

    public static CompoundTag unwrapRuntimeTag(CompoundTag tag) {
        if (tag.contains("shop", Tag.TAG_COMPOUND)) {
            return tag.getCompound("shop");
        }
        if (tag.contains("data", Tag.TAG_COMPOUND)) {
            CompoundTag dataTag = tag.getCompound("data");
            if (dataTag.contains("shop", Tag.TAG_COMPOUND)) {
                return dataTag.getCompound("shop");
            }
            return dataTag;
        }
        return tag;
    }

    public static int getRuntimeDataVersion(CompoundTag rootTag, CompoundTag shopTag) {
        if (shopTag.contains(VERSION_TAG, Tag.TAG_INT)) {
            return shopTag.getInt(VERSION_TAG);
        }
        if (rootTag.contains(VERSION_TAG, Tag.TAG_INT)) {
            return rootTag.getInt(VERSION_TAG);
        }
        if (rootTag.contains("meta", Tag.TAG_COMPOUND)) {
            CompoundTag meta = rootTag.getCompound("meta");
            if (meta.contains(VERSION_TAG, Tag.TAG_INT)) {
                return meta.getInt(VERSION_TAG);
            }
            if (meta.contains("version", Tag.TAG_STRING)) {
                return parseMajorVersion(meta.getString("version"));
            }
        }
        return 1;
    }

    private static int parseMajorVersion(String version) {
        int dotIndex = version.indexOf('.');
        String major = dotIndex >= 0 ? version.substring(0, dotIndex) : version;
        try {
            return Integer.parseInt(major);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    /**
     * 将旧版本商店运行时数据迁移到当前格式。
     *
     * @param shopTag 商店运行时数据
     * @param version 数据版本号，如果无法确定版本传入 1
     * @return 迁移后的商店运行时数据
     */
    @NotNull
    public static CompoundTag migrateShopData(@NotNull CompoundTag shopTag, int version) {
        if (version >= VERSION) {
            if (shopTag.contains(VERSION_TAG, Tag.TAG_INT)) {
                return shopTag;
            }
            CompoundTag tagged = shopTag.copy();
            tagged.putInt(VERSION_TAG, VERSION);
            return tagged;
        }

        CompoundTag currentTag = shopTag;
        int currentVersion = version;
        while (currentVersion < VERSION) {
            currentTag = migrateToNextVersion(currentTag, currentVersion);
            currentVersion++;
        }
        currentTag.putInt(VERSION_TAG, VERSION);
        return currentTag;
    }

    @NotNull
    private static CompoundTag migrateToNextVersion(@NotNull CompoundTag shopTag, int fromVersion) {
        return switch (fromVersion) {
            case 1 -> migrateV1ToV2(shopTag);
            case 2 -> migrateV2ToV3(shopTag);
            default -> shopTag;
        };
    }

    private static CompoundTag migrateV1ToV2(CompoundTag shopTag) {
        CompoundTag migratedTag = shopTag.copy();

        if (migratedTag.contains("categoryInfos")) {
            var categoryInfosTag = migratedTag.get("categoryInfos");
            if (categoryInfosTag instanceof CompoundTag oldCategoryFormat) {
                if (oldCategoryFormat.contains("payload")) {
                    var payload = oldCategoryFormat.get("payload");
                    if (payload != null) {
                        migratedTag.put("categoryInfos", payload);
                        if (payload instanceof ListTag categoryList) {
                            for (var category : categoryList) {
                                if (category instanceof CompoundTag categoryCompound) {
                                    migrateCategoryMerchants(categoryCompound);
                                }
                            }
                        }
                    }
                }
            } else if (categoryInfosTag instanceof ListTag categoryList) {
                for (var category : categoryList) {
                    if (category instanceof CompoundTag categoryCompound) {
                        migrateCategoryMerchants(categoryCompound);
                    }
                }
            }
        }

        return migratedTag;
    }

    private static CompoundTag migrateV2ToV3(CompoundTag shopTag) {
        CompoundTag migratedTag = shopTag.copy();
        migratedTag.remove("stage");

        var categoryInfosTag = migratedTag.get("categoryInfos");
        if (categoryInfosTag instanceof ListTag categoryList) {
            for (var category : categoryList) {
                if (category instanceof CompoundTag categoryCompound) {
                    migrateMerchantStageGroups(categoryCompound);
                }
            }
        }

        return migratedTag;
    }

    private static void migrateCategoryMerchants(CompoundTag categoryCompound) {
        if (categoryCompound.contains("merchants")) {
            var merchantsTag = categoryCompound.get("merchants");
            if (merchantsTag instanceof CompoundTag oldMerchantsFormat) {
                if (oldMerchantsFormat.contains("payload")) {
                    var payload = oldMerchantsFormat.get("payload");
                    if (payload != null) {
                        categoryCompound.put("merchants", payload);
                    }
                }
            }
        }
    }

    private static void migrateMerchantStageGroups(CompoundTag categoryCompound) {
        if (!(categoryCompound.get("merchants") instanceof ListTag merchants)) {
            return;
        }

        for (var merchant : merchants) {
            if (!(merchant instanceof CompoundTag merchantCompound)) {
                continue;
            }

            ListTag flags = merchantCompound.contains("flags", Tag.TAG_LIST)
                    ? merchantCompound.getList("flags", Tag.TAG_STRING).copy()
                    : new ListTag();
            if (merchantCompound.contains("stage", Tag.TAG_INT)) {
                int stage = merchantCompound.getInt("stage");
                merchantCompound.remove("stage");
                if (stage > 0) {
                    addFlagIfAbsent(flags, String.valueOf(stage));
                }
            }

            merchantCompound.remove("flags");
            if (merchantCompound.contains("flagGroups", Tag.TAG_LIST) || flags.isEmpty()) {
                continue;
            }

            merchantCompound.put("flagGroups", createAndFlagGroups(flags));
        }
    }

    private static void addFlagIfAbsent(ListTag flags, String flag) {
        for (int i = 0; i < flags.size(); i++) {
            if (flag.equals(flags.getString(i))) {
                return;
            }
        }
        flags.add(StringTag.valueOf(flag));
    }

    private static ListTag createAndFlagGroups(ListTag flags) {
        CompoundTag group = new CompoundTag();
        group.putString("mode", "viscript_shop.data.flag_group.mode.and");
        group.put("flags", flags);
        ListTag flagGroups = new ListTag();
        flagGroups.add(group);
        return flagGroups;
    }
}
