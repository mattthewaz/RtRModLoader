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

    public String getModIdFromFile(File modFile) {
        return installer.getModIdFromFile(modFile);
    }

    public boolean isModInstalled(File modFile) {
        String id = getModIdFromFile(modFile);
        return id != null && installer.isModInstalled(id);
    }

    private ModInfo readModMetadata(JarFile jarFile, File jar) {
        String id = null;
        String name = null;
        String version = null;
        String author = null;
        String description = null;

        Properties props = new Properties();

        // Try reading mod.properties, to search infos, if they are missing, read from MANIFEST.MF and in the worst case it does a fallback on the filename

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
        List<String> successfullyRemovedIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        for (ModInfo mod : targets) {
            boolean deleted = installer.deleteMod(mod);
            if (deleted) {
                successfullyRemovedIds.add(mod.getId());
                ModLogger.info("Deleted mod: " + mod.getName() + " (ID: " + mod.getId() + ")");
            } else {
                failedIds.add(mod.getId());
                ModLogger.error("Could not delete mod: " + mod.getName());
            }
        }
        // Remove the status only for mods that have actually been deleted
        if (!successfullyRemovedIds.isEmpty()) {
            stateManager.removeEntries(successfullyRemovedIds);
        }
        // Reload the list to reflect the current state of the filesystem
        loadMods();
        // Notify the view of the error
        if (!failedIds.isEmpty()) {
            // For example, using a callback method or by triggering an event
            ModLogger.warn("Some mods could not be deleted: " + String.join(", ", failedIds));
            pcs.firePropertyChange("deleteFailed", null, failedIds);
        }
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