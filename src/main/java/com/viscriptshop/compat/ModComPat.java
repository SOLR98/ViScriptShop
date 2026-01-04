package com.viscriptshop.compat;

import com.viscriptshop.ViscriptShop;
import net.neoforged.api.distmarker.Dist;

public class ModComPat {
    public static void init(Dist dist) {
        if (dist == Dist.CLIENT) {
            if (ViscriptShop.isFtbLibraryLoaded()) {
                FtbLibraryComPat.init();
            }
        } else {

        }
    }
}
