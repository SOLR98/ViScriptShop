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
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;

public class ShopProject implements IProject {
    public static int VERSION = 1;
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
        shop.deserializeNBT(provider, nbt.getCompound("shop"));
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
        this.shop.shopInfo.setStage(0);
        return true;
    }
}
