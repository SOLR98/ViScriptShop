package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.project.ShopProject;
import com.viscriptshop.gui.view.CategoryView;
import com.viscriptshop.gui.view.ShopPreviewView;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaDisplay;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShopEditor extends Editor {
    public final static ResourceLocation SHOP_ID = ViscriptShop.id("editor");

    public final CategoryView categoryView = new CategoryView(this);
    public final ShopPreviewView shopPreviewView = new ShopPreviewView(this);

    public ShopEditor() {
        fileMenu.addProjectProvider(ShopProject.PROVIDER);
        this.leftWindow.getLeftTop().addView(categoryView);
        this.centerWindow.getLeftTop().addView(shopPreviewView);
        this.bottomWindow.setDisplay(YogaDisplay.NONE);
        this.bottomWindow.getParentWindow().removeSplitWindow(this.bottomWindow);
    }

    @Override
    protected Editor createNewEditorInstance() {
        return new ShopEditor();
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
}
