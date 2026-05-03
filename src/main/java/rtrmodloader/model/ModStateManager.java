package rtrmodloader.model;

import rtrmodloader.core.ModLogger;

import java.io.*;
import java.util.List;
import java.util.Properties;

public class ModStateManager {
    private static final File STATE_FILE = new File("mods/mod_state.properties");

    public void saveState(List<ModInfo> mods) {
        Properties props = new Properties();
        for (ModInfo mod : mods) {
            props.setProperty(mod.getId(), String.valueOf(mod.isEnabled()));
        }
        try (FileOutputStream out = new FileOutputStream(STATE_FILE)) {
            props.store(out, "Mod enabled states (modId=enabled)");
        } catch (IOException e) {
            System.err.println("Failed to save mod state: " + e.getMessage());
        }
    }

    public void loadState(List<ModInfo> mods) {
        if (!STATE_FILE.exists()) return;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(STATE_FILE)) {
            props.load(in);
            for (ModInfo mod : mods) {
                String val = props.getProperty(mod.getId());
                if (val != null) {
                    mod.setEnabled(Boolean.parseBoolean(val));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load mod state: " + e.getMessage());
        }
    }

    public void removeEntries(List<String> modIds) {
        if (!STATE_FILE.exists()) return;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(STATE_FILE)) {
            props.load(in);
            boolean changed = false;
            for (String id : modIds) {
                if (props.remove(id) != null) {
                    changed = true;
                    ModLogger.debug("Removed state entry for mod: " + id);
                }
            }
            if (changed) {
                try (FileOutputStream out = new FileOutputStream(STATE_FILE)) {
                    props.store(out, "Mod enabled states (modId=enabled) – cleaned");
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to clean mod state: " + e.getMessage());
        }
    }
}