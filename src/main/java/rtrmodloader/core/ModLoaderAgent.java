package rtrmodloader.core;

import rtrmodloader.api.RtRMod;
import rtrmodloader.mods.GameVersionMod;
import rtrmodloader.mods.SaveRedirectMod;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ModLoaderAgent {

    public static void premain(String args, Instrumentation inst) {
        ModLogger.info("[RtRModLoader] Agent loaded v" + ModLoaderVersion.VERSION);

        List<RtRMod> mods = new ArrayList<>();
        List<ClassLoader> modClassLoaders = new ArrayList<>();
        Set<String> loadedIds = new HashSet<>();

        // Built-in mods always load first
        mods.add(new GameVersionMod());
        mods.add(new SaveRedirectMod());

        // External mods — override with -Drtr.mods.dir=..., otherwise <user.dir>/mods
        String modsDirProp = System.getProperty("rtr.mods.dir");
        File modsDir = modsDirProp != null ? new File(modsDirProp) : new File(System.getProperty("user.dir"), "mods");
        if (modsDir.isDirectory()) {
            Set<String> disabled = loadDisabledFromProperties(modsDir);
            File[] jars = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    try (JarFile jarFile = new JarFile(jar)) {
                        // Determines the JAR ID using the same logic as ModManager
                        String jarId = getModIdFromJar(jarFile, jar);
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
                                modClassLoaders.add(cl);
                                System.out.println("[RtRModLoader] Loaded mod: " + mod.getId());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[RtRModLoader] Failed to load " + jar.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        System.out.println("[RtRModLoader] Total mods loaded: " + mods.size());
        for (RtRMod m : mods) {
            System.out.println("  - Mod: " + m.getId() + " patches: " + m.getPatches().size());
        }
        System.out.println("[RtRModLoader] Transformer added.");

        inst.addTransformer(new ModDispatchTransformer(mods, modClassLoaders));
    }

    private static Set<String> loadDisabledFromProperties(File modsDir) {
        Set<String> disabled = new HashSet<>();
        File propsFile = new File(modsDir, "mod_state.properties");
        if (!propsFile.exists()) return disabled;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(propsFile)) {
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                if ("false".equalsIgnoreCase(props.getProperty(key))) {
                    disabled.add(key);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read mod state: " + e.getMessage());
        }
        return disabled;
    }

    private static String getModIdFromJar(JarFile jarFile, File jar) {
        try {
            Manifest mf = jarFile.getManifest();
            if (mf != null && mf.getMainAttributes().getValue("Implementation-Title") != null) {
                return mf.getMainAttributes().getValue("Implementation-Title");
            }
        } catch (IOException ignored) {}
        // Fallback: filename without extension
        return jar.getName().replaceFirst("\\.jar$", "");
    }
}
