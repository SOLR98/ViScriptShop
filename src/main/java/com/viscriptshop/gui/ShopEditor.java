package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.viscriptshop.gui.project.ShopProject;
import com.viscriptshop.gui.view.CategoryView;
import com.viscriptshop.gui.view.ShopPreviewView;
import com.viscriptshop.util.ShopHelper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShopEditor extends Editor {
//    public final static SpriteTexture ICON = SpriteTexture.of(ViscriptShop.formattedMod("textures/icon.png"));

    public final CategoryView categoryView = new CategoryView(this);
    public final ShopPreviewView shopPreviewView = new ShopPreviewView(this);
    public IProject project;

    public ShopEditor() {
        fileMenu.addProjectProvider(ShopProject.PROVIDER);
//        this.icon.style(style -> style.backgroundTexture(ICON));
        this.leftWindow.getLeftTop().addView(categoryView);
        this.centerWindow.getLeftTop().addView(shopPreviewView);
        if (ShopHelper.cacheShopProject != null) {
            loadNewProject(ShopHelper.cacheShopProject, null);
        }
    }

    @Override
    protected void loadNewProject(IProject project, @Nullable File projectFile) {
        if (project instanceof ShopProject shopProject) {
            super.loadNewProject(project, projectFile);
            ShopHelper.cacheShopProject = shopProject;
            inspectorView.inspect(shopProject.shop.shopInfo);
            this.project = shopProject;
            categoryView.loadView();
            shopPreviewView.loadView();
        }
    }
}
