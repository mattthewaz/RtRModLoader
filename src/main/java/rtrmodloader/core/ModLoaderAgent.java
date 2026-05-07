package rtrmodloader.core;

import rtrmodloader.api.RtRMod;
import rtrmodloader.mods.GameVersionMod;
import rtrmodloader.mods.SaveRedirectMod;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.jar.JarFile;

import rtrmodloader.model.ModOptionsStore;
import rtrmodloader.model.ModStateLoader;
import rtrmodloader.util.ModMetadataReader;

public class ModLoaderAgent {

    public static void premain(String args, Instrumentation inst) {
        ModLogger.info("[RtRModLoader] Agent loaded v" + ModLoaderVersion.VERSION);

        List<RtRMod> mods = new ArrayList<>();
        Set<String> loadedIds = new HashSet<>();

        // Built-in mods always load first
        mods.add(new GameVersionMod());
        mods.add(new SaveRedirectMod());

        // External mods — override with -Drtr.mods.dir=..., otherwise <user.dir>/mods
        String modsDirProp = System.getProperty("rtr.mods.dir");
        File modsDir = modsDirProp != null ? new File(modsDirProp) : new File(System.getProperty("user.dir"), "mods");
        if (modsDir.isDirectory()) {
            Set<String> disabled = loadDisabledFromProperties();
            File[] jars = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    try (JarFile jarFile = new JarFile(jar)) {
                        // Determines the JAR ID using the same logic as ModManager
                        String jarId = ModMetadataReader.getModIdFromFile(jar);
                        // Add the JAR to the system classpath
                        inst.appendToSystemClassLoaderSearch(jarFile);
                        // Use the system classloader directly — the JAR is already
                        // appended above, so ServiceLoader will find the mod's
                        // META-INF/services there.  A separate URLClassLoader would
                        // load mod classes twice (once per CL), breaking static
                        // singletons like ArchipelagoMod.instance.
                        ClassLoader cl = ClassLoader.getSystemClassLoader();
                        for (RtRMod mod : ServiceLoader.load(RtRMod.class, cl)) {
                            if (!loadedIds.add(mod.getId())) continue;
                            // Check if the mod is disabled: use both the JAR ID and the ID declared by the mod
                            if (disabled.contains(jarId) || disabled.contains(mod.getId())) {
                                System.out.println("[RtRModLoader] Skipping disabled mod: " + mod.getId() + " (jarId: " + jarId + ")");
                            } else {
                                mods.add(mod);
                                System.out.println("[RtRModLoader] Loaded mod: " + mod.getId());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[RtRModLoader] Failed to load " + jar.getName() + ": " + e.getMessage());
                    }
                }
            }
            ModOptionsStore.load(mods, modsDir);
        }

        System.out.println("[RtRModLoader] Total mods loaded: " + mods.size());
        for (RtRMod m : mods) {
            System.out.println("  - Mod: " + m.getId() + " patches: " + m.getPatches().size());
        }
        System.out.println("[RtRModLoader] Transformer added.");

        inst.addTransformer(new ModDispatchTransformer(mods));
    }

    private static Set<String> loadDisabledFromProperties() {
        Set<String> disabled = new HashSet<>();
        Map<String, Boolean> states = ModStateLoader.loadStates();
        for (Map.Entry<String, Boolean> e : states.entrySet()) {
            if (!e.getValue()) {
                disabled.add(e.getKey());
            }
        }
        return disabled;
    }
}
