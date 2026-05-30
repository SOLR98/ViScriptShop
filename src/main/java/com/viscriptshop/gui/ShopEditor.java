package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.viscript_lib.gui.editor.EditorServerUploads;
import com.viscript_lib.gui.editor.EditorUploadAction;
import com.viscript_lib.gui.editor.FunctionFileEditor;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.project.ShopProject;
import com.viscriptshop.gui.view.CategoryView;
import com.viscriptshop.gui.view.ShopPreviewView;
import com.viscriptshop.util.ShopHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShopEditor extends FunctionFileEditor {
    public final static ResourceLocation SHOP_ID = ViscriptShop.id("editor");

    public final CategoryView categoryView = new CategoryView(this);
    public final ShopPreviewView shopPreviewView = new ShopPreviewView(this);

    public ShopEditor() {
        registerFunctionFileType(ShopProject.PROVIDER);
        this.leftWindow.getLeftTop().addView(categoryView);
        this.centerWindow.getLeftTop().addView(shopPreviewView);
        removeBottomWindow();
    }

    @Override
    protected Editor createNewEditorInstance() {
        return new ShopEditor();
    }

    @Override
    protected EditorUploadAction createServerUploadAction() {
        if (getCurrentProject() instanceof ShopProject project) {
            return new ShopServerUploadAction(project, this);
        }
        return null;
    }

    @Override
    protected void loadNewProject(IProject project, @Nullable File projectFile) {
        if (project instanceof ShopProject shopProject) {
            super.loadNewProject(project, projectFile);
            inspectorView.inspect(shopProject.shop.shopInfo);
            categoryView.loadView();
            shopPreviewView.loadView();
        }
    }

    private record ShopServerUploadAction(ShopProject project, ShopEditor editor) implements EditorUploadAction {
        @Override
        public Component getDisplayName() {
            return Component.translatable("viscript_shop.editor.project.upload_shop");
        }

        @Override
        public String getDialogTitleKey() {
            return "viscript_shop.editor.project.upload_shop";
        }

        @Override
        public String getDefaultFileName() {
            File currentFile = editor.getCurrentProjectFile();
            if (currentFile == null) {
                return "test";
            }
            String fileName = currentFile.getName();
            String suffix = getSuffix();
            return fileName.endsWith(suffix) ? fileName.substring(0, fileName.length() - suffix.length()) : fileName;
        }

        @Override
        public String getSuffix() {
            return ShopProject.FORMAT.runtimeSuffix();
        }

        @Override
        public void uploadToServer(String fileName) {
            if (!project.isTrueFormat(editor)) {
                return;
            }
            EditorServerUploads.uploadToServer(
                    ShopProject.FORMAT,
                    fileName,
                    project.serializeRuntimeFile(Platform.getFrozenRegistry())
            );
            ShopHelper.clearCache();
        }
    }
}
