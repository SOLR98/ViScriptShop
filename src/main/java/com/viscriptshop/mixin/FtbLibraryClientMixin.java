package com.viscriptshop.mixin;

import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.viscriptshop.gui.ShopEditor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftblibrary.FTBLibraryClient", remap = false)
public class FtbLibraryClientMixin {

    @Inject(method = "areButtonsVisible", at = @At("HEAD"), cancellable = true, remap = false)
    private static void viscriptShop$hideSidebarForShopEditor(Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (viscript_shop$isShopEditorOpened(screen)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean viscript_shop$isShopEditorOpened(Screen screen) {
        if (screen instanceof ModularUIContainerScreen containerScreen) {
            var rootElement = containerScreen.getMenu().getModularUI().ui.rootElement;
            if (rootElement instanceof ShopEditor) {
                return true;
            }
            if (rootElement instanceof EditorWindow editorWindow) {
                return editorWindow.getCurrentEditor() instanceof ShopEditor;
            }
        }
        return false;
    }
}
