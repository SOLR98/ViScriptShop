package com.viscriptshop.gui.project;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.viscript_lib.gui.editor.EditorFileFormat;
import com.viscript_lib.gui.editor.FunctionFileProjectType;
import com.viscript_lib.gui.editor.IRuntimeFileProject;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.util.ShopHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

public class ShopProject implements IRuntimeFileProject {
    public static final EditorFileFormat FORMAT = EditorFileFormat.compressed(ViscriptShop.MOD_ID, "shop", Shop.SUFFIX);
    public static final ProjectType PROVIDER = new ShopFunctionFileProjectType();

    public Shop shop = new Shop();


    @Override
    public String getVersion() {
        return "%d.0".formatted(Shop.VERSION);
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
        return serializeRuntimeFile(provider);
    }

    @Override
    public CompoundTag serializeRuntimeFile(HolderLookup.Provider provider) {
        return shop.serializeNBT(provider);
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        shop.deserializeNBT(provider, nbt);
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

    private static class ShopFunctionFileProjectType extends FunctionFileProjectType {
        private ShopFunctionFileProjectType() {
            super(IGuiTexture.EMPTY, "viscript_shop.editor.shop.add", FORMAT, ShopProject::new);
        }

        @Override
        public IProject loadProjectFromFile(File file) throws Exception {
            CompoundTag data;
            if (FORMAT.compressed()) {
                try (var inputStream = Files.newInputStream(file.toPath())) {
                    data = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
                }
            } else {
                data = Objects.requireNonNull(NbtIo.read(file.toPath()));
            }
            var project = getProjectCreator().get();
            project.deserializeProject(Platform.getFrozenRegistry(), data);
            return project;
        }

        @Override
        public void saveProjectToFile(IProject project, File file) throws Exception {
            if (file.getParentFile() != null) {
                Files.createDirectories(file.getParentFile().toPath());
            }
            var fileData = serializeRuntimeFile(project);
            if (FORMAT.compressed()) {
                NbtIo.writeCompressed(fileData, file.toPath());
            } else {
                NbtIo.write(fileData, file.toPath());
            }
            ShopHelper.clearCache();
        }

        @Override
        public boolean isProjectDirty(IProject project, File file) throws Exception {
            CompoundTag fileData;
            if (FORMAT.compressed()) {
                try (var inputStream = Files.newInputStream(file.toPath())) {
                    fileData = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
                }
            } else {
                fileData = Objects.requireNonNull(NbtIo.read(file.toPath()));
            }
            return !serializeRuntimeFile(project).equals(fileData);
        }

        private CompoundTag serializeRuntimeFile(IProject project) {
            if (project instanceof IRuntimeFileProject runtimeFileProject) {
                return runtimeFileProject.serializeRuntimeFile(Platform.getFrozenRegistry());
            }
            return project.serializeProject(Platform.getFrozenRegistry());
        }
    }
}
