package rtrmodloader.model;

import rtrmodloader.api.ModOption;
import rtrmodloader.api.ModOptionProvider;
import rtrmodloader.api.RtRMod;
import rtrmodloader.core.ModLogger;

import java.io.*;
import java.util.List;
import java.util.Properties;

public final class ModOptionsStore {

    private ModOptionsStore() {}

    /**
     * Load saved option values for every mod that implements ModOptionProvider.
     * Missing file = no saved values; missing key = option keeps its default.
     */
    public static void load(List<RtRMod> mods, File modsDir) {
        for (RtRMod mod : mods) {
            if (!(mod instanceof ModOptionProvider)) continue;
            File file = optionsFile(modsDir, mod.getId());
            if (!file.exists()) {
                save(mod, modsDir);
                continue;
            }
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                ModLogger.warn("Could not load options for " + mod.getId() + ": " + e.getMessage());
                continue;
            }
            for (ModOption opt : ((ModOptionProvider) mod).getOptions()) {
                String raw = props.getProperty(opt.getId());
                if (raw != null) opt.fromPropertyString(raw);
            }
        }
    }

    /**
     * Persist the current option values for a single mod.
     * Called after any option is changed so values survive a restart.
     */
    public static void save(RtRMod mod, File modsDir) {
        if (!(mod instanceof ModOptionProvider)) return;
        List<ModOption> options = ((ModOptionProvider) mod).getOptions();
        Properties props = new Properties();
        for (ModOption opt : options) {
            props.setProperty(opt.getId(), opt.toPropertyString());
        }
        File file = optionsFile(modsDir, mod.getId());
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, mod.getId() + " options");
        } catch (IOException e) {
            ModLogger.warn("Could not save options for " + mod.getId() + ": " + e.getMessage());
        }
    }

    private static File optionsFile(File modsDir, String modId) {
        return new File(modsDir, modId + ".options.properties");
    }
}
