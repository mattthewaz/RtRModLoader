package rtrmodloader.model;

import rtrmodloader.core.ModLogger;

import java.io.*;
import java.util.*;

public class SaveFolderManager {
    private static final File CONFIG_FILE = new File("mods/save_folder.properties");
    private static final File HISTORY_FILE = new File("mods/save_folders_history.properties");

    private String currentFolder = "profiles";
    private final List<String> history = new ArrayList<>();

    public SaveFolderManager() {
        load();
        loadHistory();
        // Pulisci la cronologia: rimuovi duplicati e normalizza "profiles"
        cleanHistory();
        if (history.isEmpty()) {
            history.add("profiles");
            saveHistory();
        }
    }

    private void cleanHistory() {
        boolean changed = history.removeIf(s -> s.trim().equalsIgnoreCase("profiles") && !s.equals("profiles"));
        // Rimuovi eventuali varianti di "profiles"
        // Assicurati che "profiles" sia presente in forma canonica
        if (!history.contains("profiles")) {
            history.add(0, "profiles");
            changed = true;
        }
        // Rimuovi eventuali stringhe vuote
        if (history.removeIf(s -> s == null || s.trim().isEmpty())) {
            changed = true;
        }
        if (changed) {
            saveHistory();
        }
    }

    public String getCurrentFolder() {
        return currentFolder;
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void setCurrentFolder(String folder) {
        if (folder == null || folder.trim().isEmpty()) {
            folder = "profiles";
        }
        // Sanitizza: solo caratteri alfanumerici, trattini, underscore
        String sanitized = folder.replaceAll("[^a-zA-Z0-9_-]", "");
        if (sanitized.isEmpty()) sanitized = "profiles";

        if (!this.currentFolder.equals(sanitized)) {
            this.currentFolder = sanitized;
            save();
            // Aggiungi alla cronologia se non già presente
            if (!history.contains(sanitized)) {
                history.add(sanitized);
                saveHistory();
            }
        }
    }

    public boolean removeFromHistory(String folder) {
        if (folder.trim().equalsIgnoreCase("profiles")) {
            return false;
        }
        if (history.remove(folder)) {
            saveHistory();
            ModLogger.debug("Removed '" + folder + "' from history.");
            return true;
        }
        return false;
    }

    private void load() {
        if (!CONFIG_FILE.exists()) return;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            String folder = props.getProperty("saveFolder");
            if (folder != null && !folder.isEmpty()) {
                currentFolder = folder;
            }
        } catch (IOException e) {
            System.err.println("Failed to load save folder config: " + e.getMessage());
        }
    }

    private void save() {
        Properties props = new Properties();
        props.setProperty("saveFolder", currentFolder);
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Save folder for modded profiles");
        } catch (IOException e) {
            System.err.println("Failed to save save folder config: " + e.getMessage());
        }
    }

    private void loadHistory() {
        if (!HISTORY_FILE.exists()) return;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(HISTORY_FILE)) {
            props.load(in);
            String hist = props.getProperty("history", "");
            if (!hist.isEmpty()) {
                String[] parts = hist.split(",");
                history.clear();
                for (String p : parts) {
                    String trimmed = p.trim();
                    if (!trimmed.isEmpty()) history.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load history: " + e.getMessage());
        }
    }

    private void saveHistory() {
        Properties props = new Properties();
        String hist = String.join(",", history);
        props.setProperty("history", hist);
        try (FileOutputStream out = new FileOutputStream(HISTORY_FILE)) {
            props.store(out, "Save folders history");
        } catch (IOException e) {
            System.err.println("Failed to save history: " + e.getMessage());
        }
    }
}