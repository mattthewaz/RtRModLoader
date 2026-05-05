package rtrmodloader.model;

import rtrmodloader.core.ModLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import rtrmodloader.util.ModMetadataReader;

public class ModInstaller {
    private static final String MODS_DIR = "mods";

    /**
     * Installs a mod from a ZIP file.
     * Converts the ZIP into a valid JAR (with META-INF/services if necessary)
     * and saves it to the mods folder as [modId].jar.
     * For simplicity, it assumes that the ZIP already contains the classes in the
     * correct structure and, optionally, the service file.
     */

    public boolean installMod(File modFile) {
        ModLogger.info("Installing mod from " + modFile.getName());
        String lower = modFile.getName().toLowerCase();
        if (!(lower.endsWith(".zip"))) {
            ModLogger.error("Only .zip files are supported for conversion.");
            return false;
        }

        File modsDir = new File(MODS_DIR);
        if (!modsDir.exists() && !modsDir.mkdirs()) {
            ModLogger.error("Cannot create mods directory: " + modsDir.getAbsolutePath());
            return false;
        }

        String realId = getModIdFromFile(modFile);
        if (realId == null) {
            ModLogger.error("Cannot determine mod ID from " + modFile.getName());
            return false;
        }

        File targetJar = new File(modsDir, realId + ".jar");

        if (targetJar.exists()) {
            ModLogger.warn("Mod already exists, will overwrite.");
        }

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(modFile.toPath()));
             JarOutputStream jos = new JarOutputStream(Files.newOutputStream(targetJar.toPath()))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Copy the entry to the JAR
                jos.putNextEntry(new ZipEntry(entry.getName()));
                byte[] buffer = new byte[4096];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    jos.write(buffer, 0, len);
                }
                jos.closeEntry();
                zis.closeEntry();
            }

            ModLogger.info("Mod installed successfully as " + targetJar.getName());
            return true;
        } catch (IOException e) {
            ModLogger.error("Installation failed: " + e.getMessage());
            if (targetJar.exists() && !targetJar.delete()) {
                ModLogger.warn("Failed to delete incomplete JAR: " + targetJar.getName());
            }
            return false;
        }
    }

    public boolean copyJar(File sourceJar) {
        File modsDir = new File(MODS_DIR);
        if (!modsDir.exists() && !modsDir.mkdirs()) {
            ModLogger.error("Cannot create mods directory: " + modsDir.getAbsolutePath());
            return false;
        }
        String id = getModIdFromFile(sourceJar);
        if (id == null || id.isEmpty()) {
            id = sourceJar.getName().replaceFirst("\\.jar$", "");
        }
        File dest = new File(modsDir, id + ".jar");
        try {
            Files.copy(sourceJar.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ModLogger.info("JAR copied as " + dest.getName());
            return true;
        } catch (IOException e) {
            ModLogger.error("Failed to copy JAR: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the mod ID that would be used for the given file (without checking existence).
     */
    public String getModIdFromFile(File modFile) {
        return ModMetadataReader.getModIdFromFile(modFile);
    }

    /**
     * Checks whether a mod with the given ID is already installed.
     */
    public boolean isModInstalled(String modId) {
        File target = new File(MODS_DIR, modId + ".jar");
        return target.exists();
    }

    public boolean deleteMod(ModInfo mod) {
        File jar = new File(mod.getPath());
        if (!jar.exists()) {
            // The file does not exist: as far as we're concerned, it's as if it had been deleted.
            ModLogger.warn("JAR not found (already removed?): " + jar.getName());
            return true;
        }
        if (jar.delete()) {
            ModLogger.info("Deleted JAR: " + jar.getName());
            return true;
        } else {
            ModLogger.error("Failed to delete JAR: " + jar.getName());
            return false;
        }
    }

}