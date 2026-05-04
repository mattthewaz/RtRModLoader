package rtrmodloader.model;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ModStateLoader {
    private static final File STATE_FILE = new File("mods/mod_state.properties");

    private ModStateLoader() {}

    public static Map<String, Boolean> loadStates() {
        Map<String, Boolean> states = new HashMap<>();
        if (!STATE_FILE.exists()) return states;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(STATE_FILE)) {
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                states.put(key, Boolean.parseBoolean(props.getProperty(key)));
            }
        } catch (IOException e) {
            System.err.println("Failed to load mod state: " + e.getMessage());
        }
        return states;
    }

    public static void saveStates(Map<String, Boolean> states) {
        Properties props = new Properties();
        for (Map.Entry<String, Boolean> e : states.entrySet()) {
            props.setProperty(e.getKey(), String.valueOf(e.getValue()));
        }
        try (FileOutputStream out = new FileOutputStream(STATE_FILE)) {
            props.store(out, "Mod enabled states (modId=enabled)");
        } catch (IOException e) {
            System.err.println("Failed to save mod state: " + e.getMessage());
        }
    }
}