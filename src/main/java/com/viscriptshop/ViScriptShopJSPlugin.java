package com.viscriptshop;

import com.viscriptshop.event.CommonEventsPostJS;
import com.viscriptshop.event.ViScriptShopEventsJS;
import com.viscriptshop.util.ViScriptShopUtil;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

public class ViScriptShopJSPlugin implements KubeJSPlugin {

    @Override
    public void init() {
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(CommonEventsPostJS.class);
        }
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(ViScriptShopEventsJS.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("ViScriptShopUtil", ViScriptShopUtil.class);
    }
}
