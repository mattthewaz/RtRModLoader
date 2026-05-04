package rtrmodloader.model;

import rtrmodloader.core.ModLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
            ModLogger.error("Only .zip files are supported.");
            return false;
        }

        File modsDir = new File(MODS_DIR);
        if (!modsDir.exists() && !modsDir.mkdirs()) {
            ModLogger.error("Cannot create mods directory: " + modsDir.getAbsolutePath());
            return false;
        }

        String realId;
        if (lower.endsWith(".zip")) {
            String idFromZip = readModIdFromZip(modFile);
            if (idFromZip != null && !idFromZip.isEmpty()) {
                realId = idFromZip;
            } else {
                realId = modFile.getName().replaceFirst("\\.zip$", "");
            }
        } else {
            // For .jar files, we use the filename (then, when loading, the ID will be read from within the file)
            realId = modFile.getName().replaceFirst("\\.jar$", "");
        }

        File targetJar = new File(modsDir, realId + ".jar");

        //TODO(Expand this check, for now it does nothing and overwrite)
        if (targetJar.exists()) {
            ModLogger.warn("Mod already exists, will overwrite after confirmation.");
            // In a GUI, we ask for confirmation; here, for simplicity, we overwrite
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

        // Read the ID from the JAR (if present)
        String id = null;
        try (JarFile jf = new JarFile(sourceJar)) {
            id = readModIdFromJar(jf);
        } catch (IOException e) {
            ModLogger.warn("Could not read JAR metadata: " + e.getMessage());
        }
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

    private String readModIdFromJar(JarFile jarFile) {
        try {
            java.util.zip.ZipEntry entry = jarFile.getEntry("mod.properties");
            if (entry != null) {
                Properties props = new Properties();
                props.load(jarFile.getInputStream(entry));
                return props.getProperty("id");
            }
        } catch (IOException ignored) {}
        return null;
    }

    public void deleteMod(ModInfo mod) {
        File jar = new File(mod.getPath());
        if (jar.exists() && jar.delete()) {
            ModLogger.info("Deleted JAR: " + jar.getName());
        } else {
            ModLogger.error("Failed to delete JAR: " + jar.getName());
        }
    }

    private String readModIdFromZip(File zipFile) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("mod.properties")) {
                    Properties props = new Properties();
                    props.load(zis);
                    zis.closeEntry();
                    return props.getProperty("id");
                }                zis.closeEntry();
            }
        } catch (IOException e) {
            ModLogger.warn("Could not read mod.properties from zip: " + e.getMessage());
        }
        return null;
    }

}