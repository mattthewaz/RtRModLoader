package rtrmodloader.util;

import rtrmodloader.core.ModLogger;
import java.io.*;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ModMetadataReader {

    private ModMetadataReader() {}

    /**
     * Returns the mod ID from a .jar or .zip file by reading mod.properties,
     * or falls back to the filename without extension.
     * Returns null if the file type is not supported.
     */
    public static String getModIdFromFile(File modFile) {
        String lower = modFile.getName().toLowerCase();
        if (lower.endsWith(".zip")) {
            String idFromZip = readModIdFromZip(modFile);
            return (idFromZip != null && !idFromZip.isEmpty()) ? idFromZip : modFile.getName().replaceFirst("\\.zip$", "");
        } else if (lower.endsWith(".jar")) {
            try (JarFile jf = new JarFile(modFile)) {
                String idFromJar = readModIdFromJar(jf);
                return (idFromJar != null && !idFromJar.isEmpty()) ? idFromJar : modFile.getName().replaceFirst("\\.jar$", "");
            } catch (IOException ignored) {}
            return modFile.getName().replaceFirst("\\.jar$", "");
        }
        return null;
    }

    private static String readModIdFromJar(JarFile jarFile) {
        try {
            ZipEntry entry = jarFile.getEntry("mod.properties");
            if (entry != null) {
                Properties props = new Properties();
                props.load(jarFile.getInputStream(entry));
                return props.getProperty("id");
            }
        } catch (IOException ignored) {}
        return null;
    }

    private static String readModIdFromZip(File zipFile) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("mod.properties")) {
                    Properties props = new Properties();
                    props.load(zis);
                    return props.getProperty("id");
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            ModLogger.warn("Could not read mod.properties from zip: " + e.getMessage());
        }
        return null;
    }
}