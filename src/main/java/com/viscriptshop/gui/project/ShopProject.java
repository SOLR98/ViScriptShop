package com.viscriptshop.gui.project;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.network.c2s.C2SPayload;
import com.viscriptshop.util.ShopHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;

public class ShopProject implements IProject {
    public static int VERSION = 3;
    public static final ProjectType PROVIDER = ProjectType.of(IGuiTexture.EMPTY, Component.translatable("viscript_shop.editor.shop.add").getString(), ".shopproj", ShopProject::new);

    public Shop shop = new Shop();

    // runtime
    //导出shop数据文本按钮
    @Nullable
    private ISubscription exportMenuSubscription;


    @Override
    public String getVersion() {
        return "%d.0".formatted(VERSION);
    }

    @Override
    public Resources getResources() {
        return Resources.EMPTY;
    }

    @Override
    public ProjectType getProjectType() {
        return PROVIDER;
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        var data = new CompoundTag();
        data.put("shop", shop.serializeNBT(provider));
        return data;
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        CompoundTag shopTag = nbt.getCompound("shop");
        // 获取项目版本
        var version = nbt.contains("version_num") ? nbt.getInt("version_num") : 1;
        // 应用版本兼容
        shopTag = migrateShopData(shopTag, version);
        shop.deserializeNBT(provider, shopTag);
    }

    /**
     * 版本兼容API：将旧版本的商店数据迁移到新版本格式
     * 使用逐步迁移的方式，从当前版本一步步升级到最新版本
     *
     * @param shopTag 商店数据的NBT标签
     * @param version 数据版本号（如果无法确定版本，传入1）
     * @return 迁移后的NBT标签
     */
    @NotNull
    public static CompoundTag migrateShopData(@NotNull CompoundTag shopTag, int version) {
        // 如果已经是最新的版本，不需要迁移
        if (version >= VERSION) {
            return shopTag;
        }

        // 逐步迁移：从当前版本一步步升级到最新版本
        CompoundTag currentTag = shopTag;
        int currentVersion = version;

        // 逐步执行每个版本的迁移逻辑
        while (currentVersion < VERSION) {
            currentTag = migrateToNextVersion(currentTag, currentVersion);
            currentVersion++;
        }

        return currentTag;
    }

    /**
     * 执行从指定版本到下一版本的迁移
     *
     * @param shopTag 商店数据的NBT标签
     * @param fromVersion 当前版本号
     * @return 迁移后的NBT标签
     */
    @NotNull
    private static CompoundTag migrateToNextVersion(@NotNull CompoundTag shopTag, int fromVersion) {
        return switch (fromVersion) {
            case 1 -> migrateV1ToV2(shopTag);
            case 2 -> migrateV2ToV3(shopTag);
            default -> shopTag;
        };
    }

    /**
     * 版本兼容方法：将1.0版本的数据格式迁移到2.0版本
     * 1.0版本：categoryInfos和merchants使用{uid, payload}格式
     * 2.0版本：categoryInfos和merchants直接使用数组格式
     */
    private static CompoundTag migrateV1ToV2(CompoundTag shopTag) {
        // 创建副本避免修改原始tag
        CompoundTag migratedTag = shopTag.copy();

        // 处理categoryInfos格式转换
        if (migratedTag.contains("categoryInfos")) {
            var categoryInfosTag = migratedTag.get("categoryInfos");
            if (categoryInfosTag instanceof CompoundTag oldCategoryFormat) {
                // 旧格式：{uid: X, payload: [...]}
                if (oldCategoryFormat.contains("payload")) {
                    var payload = oldCategoryFormat.get("payload");
                    if (payload != null) {
                        migratedTag.put("categoryInfos", payload);
                        // 递归处理每个category中的merchants
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
                // 如果已经是列表格式，检查每个category的merchants是否需要转换
                for (var category : categoryList) {
                    if (category instanceof CompoundTag categoryCompound) {
                        migrateCategoryMerchants(categoryCompound);
                    }
                }
            }
        }

        return migratedTag;
    }

    /**
     * 版本兼容方法：将2.0版本的数字阶段迁移到3.0版本的条件组阶段
     * 2.0版本：ShopInfo.stage控制商店阶段，MerchantInfo.stage控制商品阶段
     * 3.0版本：商店不再保存阶段，商品使用flagGroups；旧数字阶段大于0时转成同名字符串flag
     */
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

    /**
     * 迁移单个CategoryInfo中的merchants格式
     */
    private static void migrateCategoryMerchants(CompoundTag categoryCompound) {
        if (categoryCompound.contains("merchants")) {
            var merchantsTag = categoryCompound.get("merchants");
            if (merchantsTag instanceof CompoundTag oldMerchantsFormat) {
                // 旧格式：{uid: X, payload: [...]}
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

    @Override
    public CompoundTag getMetadata() {
        var meta = IProject.super.getMetadata();
        meta.putInt("version_num", VERSION);
        return meta;
    }

    @Override
    public void onLoad(Editor editor) {
        IProject.super.onLoad(editor);
        if (exportMenuSubscription != null) {
            exportMenuSubscription.unsubscribe();
        }
        exportMenuSubscription = editor.fileMenu.registerMenuCreator((tab, menu) ->
                menu.branch("viscript_shop.editor.shop.export", m -> {
                            m.leaf("viscript_shop.editor.shop.export", () -> {
                                if (isTrueFormat(editor)) {
                                    Dialog.showFileDialog("viscript_shop.editor.saveAs", new File(LDLib2.getAssetsDir(), "%s/shop/".formatted(ViscriptShop.MOD_ID)), false,
                                            Dialog.suffixFilter(Shop.SUFFIX), file -> {
                                                if (file != null && !file.isDirectory()) {
                                                    if (!file.getName().endsWith(Shop.SUFFIX)) {
                                                        file = new File(file.getParentFile(), file.getName() + Shop.SUFFIX);
                                                    }
                                                    try {
                                                        var fileData = shop.serializeNBT(Platform.getFrozenRegistry());
                                                        NbtIo.writeCompressed(fileData, file.toPath());
                                                        ShopHelper.clearCache();
                                                    } catch (Exception ignored) {
                                                    }
                                                }
                                            }).show(editor);
                                }
                            });
                            m.leaf("viscript_shop.editor.project.upload_shop", () -> {
                                Dialog.stringEditorDialog("viscript_shop.editor.project.upload_shop", "", (result) -> {
                                    return !result.isEmpty();
                                }, (result) -> {
                                    var fileData = shop.serializeNBT(Platform.getFrozenRegistry());
                                    fileData.putString("fileName", result);
                                    RPCPacketDistributor.rpcToServer(C2SPayload.UPLOAD_SHOP_FILE, fileData);
                                }).show(editor);
                            });
                        }
                ));
    }

    @Override
    public void onClosed(Editor editor) {
        IProject.super.onClosed(editor);
        if (exportMenuSubscription != null) {
            exportMenuSubscription.unsubscribe();
            exportMenuSubscription = null;
        }
    }

    public boolean isTrueFormat(Editor editor) {
        for (CategoryInfo categoryInfo : this.shop.shopInfo.getCategoryInfos()) {
            for (MerchantInfo merchant : categoryInfo.getMerchants()) {
                switch (categoryInfo.getShopType()) {
                    case ITEM_FOR_ITEM -> {
                        if (merchant.getItemA().isEmpty() && merchant.getItemB().isEmpty()) {
                            Message.warn("viscript_shop.message.item.empty", editor);
                            return false;
                        } else if (merchant.getItemResult().isEmpty()) {
                            Message.warn("viscript_shop.message.itemResult.empty", editor);
                            return false;
                        }
                    }
                    case CURRENCY -> {
                        if (merchant.getItemResult().isEmpty()) {
                            Message.warn("viscript_shop.message.itemResult.empty", editor);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
