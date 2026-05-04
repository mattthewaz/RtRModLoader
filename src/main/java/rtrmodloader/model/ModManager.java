package rtrmodloader.model;

import rtrmodloader.core.ModLogger;

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class ModManager {
    private final List<ModInfo> mods = new ArrayList<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final ModStateManager stateManager = new ModStateManager();
    private final ModInstaller installer = new ModInstaller();
    private final SaveFolderManager saveFolderManager = new SaveFolderManager();
    public SaveFolderManager getSaveFolderManager() { return saveFolderManager; }

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public List<ModInfo> getMods() {
        return Collections.unmodifiableList(mods);
    }

    public List<ModInfo> getEnabledMods() {
        return mods.stream().filter(ModInfo::isEnabled).collect(Collectors.toList());
    }

    public void loadMods() {
        List<ModInfo> newMods = new ArrayList<>();
        File modsDir = new File("mods");

        if (!modsDir.exists() && !modsDir.mkdirs()) {
            ModLogger.error("Cannot create mods directory: " + modsDir.getAbsolutePath());
            return;
        }

        File[] jars = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                try (JarFile jf = new JarFile(jar)) {
                    ModInfo info = readModMetadata(jf, jar);
                    newMods.add(info);
                }
                catch (IOException e) {
                    ModLogger.error("Failed to read JAR " + jar.getName() + ": " + e.getMessage());
                }
            }
        }

        // Load the status (enable/disable)
        stateManager.loadState(newMods);

        // Change notification
        List<ModInfo> old = new ArrayList<>(mods);
        mods.clear();
        mods.addAll(newMods);
        pcs.firePropertyChange("mods", old, mods);
    }

    private ModInfo readModMetadata(JarFile jarFile, File jar) {
        String id = null;
        String name = null;
        String version = null;
        String author = null;
        String description = null;

        Properties props = new Properties();
        // 1. Prova a leggere mod.properties
        try {
            java.util.zip.ZipEntry entry = jarFile.getEntry("mod.properties");
            if (entry != null) {
                props.load(jarFile.getInputStream(entry));
                id = props.getProperty("id");
                name = props.getProperty("name");
                version = props.getProperty("version");
                author = props.getProperty("author");
                description = props.getProperty("description");
            }
        } catch (IOException ignored) {}

        // 2. Se mancano, leggi dal MANIFEST.MF
        if (id == null || id.trim().isEmpty() || name == null) {
            try {
                Manifest mf = jarFile.getManifest();
                if (mf != null) {
                    if (id == null) id = mf.getMainAttributes().getValue("Implementation-Title");
                    if (name == null) name = mf.getMainAttributes().getValue("Implementation-Title");
                    if (version == null) version = mf.getMainAttributes().getValue("Implementation-Version");
                    if (author == null) author = mf.getMainAttributes().getValue("Implementation-Vendor");
                    if (description == null) description = mf.getMainAttributes().getValue("Description");
                }
            } catch (IOException ignored) {}
        }

        // 3. Fallback finali
        if (id == null || id.trim().isEmpty()) {
            id = jar.getName().replaceFirst("\\.jar$", "");
        }
        if (name == null || name.trim().isEmpty()) {
            name = id;
        }
        if (version == null || version.trim().isEmpty()) {
            version = "1.0";
        }
        if (author == null) author = "";
        if (description == null) description = "";

        return new ModInfo(id, name, version, jar.getAbsolutePath(), author, description);
    }

    public void enableMods(List<ModInfo> targets, boolean enabled) {
        boolean changed = false;
        for (ModInfo mod : targets) {
            if (mod.isEnabled() != enabled) {
                mod.setEnabled(enabled);
                changed = true;
            }
        }
        if (changed) {
            stateManager.saveState(mods);
            pcs.firePropertyChange("mods", null, mods);
        }
    }

    public void deleteMods(List<ModInfo> targets) {
        List<String> removedIds = new ArrayList<>();
        for (ModInfo mod : targets) {
            installer.deleteMod(mod);
            removedIds.add(mod.getId());
            ModLogger.info("Deleted mod: " + mod.getName() + " (ID: " + mod.getId() + ")");
        }
        if (!removedIds.isEmpty()) {
            stateManager.removeEntries(removedIds);
        }
        loadMods(); // ricarica la lista (e lo stato)
    }

    public void installMod(File zipFile) {
        if (installer.installMod(zipFile)) {
            loadMods();
        }
    }

    public boolean copyJar(File jarFile) {
        boolean success = installer.copyJar(jarFile);
        if (success) loadMods();
        return success;
    }
}