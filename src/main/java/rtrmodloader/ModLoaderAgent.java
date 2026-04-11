package rtrmodloader;

import rtrmodloader.api.RtRMod;
import rtrmodloader.mods.GameVersionMod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarFile;

public class ModLoaderAgent {

    public static void premain(String args, Instrumentation inst) {
        System.out.println("[RtRModLoader] Agent loaded v" + ModLoaderVersion.VERSION);

        List<RtRMod> mods = new ArrayList<RtRMod>();
        List<ClassLoader> modClassLoaders = new ArrayList<ClassLoader>();

        // Built-in mods always load first
        mods.add(new GameVersionMod());

        // External mods — override with -Drtr.mods.dir=..., otherwise <user.dir>/mods
        String modsDirProp = System.getProperty("rtr.mods.dir");
        File modsDir = modsDirProp != null ? new File(modsDirProp) : new File(System.getProperty("user.dir"), "mods");
        if (modsDir.isDirectory()) {
            Set<String> disabled = loadDisabled(modsDir);
            File[] jars = modsDir.listFiles(new java.io.FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            if (jars != null) {
                for (File jar : jars) {
                    try {
                        inst.appendToSystemClassLoaderSearch(new JarFile(jar));
                        URLClassLoader cl = new URLClassLoader(
                            new URL[]{jar.toURI().toURL()},
                            ModLoaderAgent.class.getClassLoader()
                        );
                        for (RtRMod mod : ServiceLoader.load(RtRMod.class, cl)) {
                            if (disabled.contains(mod.getId())) {
                                System.out.println("[RtRModLoader] Skipping disabled mod: " + mod.getId());
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

        inst.addTransformer(new ModDispatchTransformer(mods, modClassLoaders));
    }

    private static Set<String> loadDisabled(File modsDir) {
        Set<String> disabled = new HashSet<String>();
        File disabledFile = new File(modsDir, "disabled.txt");
        if (!disabledFile.exists()) return disabled;
        try (BufferedReader reader = new BufferedReader(new FileReader(disabledFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    disabled.add(line);
                }
            }
        } catch (Exception e) {
            System.err.println("[RtRModLoader] Failed to read disabled.txt: " + e.getMessage());
        }
        return disabled;
    }
}
