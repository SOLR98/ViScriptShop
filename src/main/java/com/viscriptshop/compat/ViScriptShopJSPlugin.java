package com.viscriptshop.compat;

import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptshop.event.CommonEventsPostJS;
import com.viscriptshop.event.ViScriptShopEventsJS;
import com.viscriptshop.util.ViScriptShopClientUtil;
import com.viscriptshop.util.ViScriptShopServerUtil;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.neoforged.neoforge.common.NeoForge;

public class ViScriptShopJSPlugin implements KubeJSPlugin {

    @Override
    public void init() {
        if (Platform.isClient()) {
            NeoForge.EVENT_BUS.register(CommonEventsPostJS.class);
        }
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(ViScriptShopEventsJS.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        ScriptType type = bindings.type();
        if (type.equals(ScriptType.CLIENT)) {
            bindings.add("ViScriptShopUtil", ViScriptShopClientUtil.class);
        } else if (type.equals(ScriptType.SERVER)) {
            bindings.add("ViScriptShopUtil", ViScriptShopServerUtil.class);
        }
    }
}
