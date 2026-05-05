package rtrmodloader.model;

import rtrmodloader.core.ModLogger;
import java.util.List;
import java.util.Map;

public class ModStateManager {

    public void saveState(List<ModInfo> mods) {
        Map<String, Boolean> states = new java.util.HashMap<>();
        for (ModInfo mod : mods) {
            states.put(mod.getId(), mod.isEnabled());
        }
        ModStateLoader.saveStates(states);
    }

    public void loadState(List<ModInfo> mods) {
        Map<String, Boolean> states = ModStateLoader.loadStates();
        for (ModInfo mod : mods) {
            Boolean enabled = states.get(mod.getId());
            if (enabled != null) {
                mod.setEnabled(enabled);
            }
        }
    }

    public void removeEntries(List<String> modIds) {
        Map<String, Boolean> states = ModStateLoader.loadStates();
        boolean changed = false;
        for (String id : modIds) {
            if (states.remove(id) != null) changed = true;
        }
        if (changed) {
            ModStateLoader.saveStates(states);
            ModLogger.debug("Removed state entries for: " + modIds);
        }
    }
}