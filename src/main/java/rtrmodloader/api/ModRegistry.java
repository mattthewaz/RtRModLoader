package rtrmodloader.api;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-classloader singleton registry backed by System.getProperties().
 *
 * System.properties is a Hashtable<Object,Object> held by the bootstrap
 * classloader, so the same map is visible regardless of which classloader
 * accesses it.  All mod instances registered here can be retrieved from
 * any classloader — including from inside Javassist-inserted patch code
 * that runs under the game's classloader.
 *
 * Usage in a mod constructor:
 *   ModRegistry.register("my-mod-id", this);
 *
 * Usage in patch hook code or static helpers:
 *   MyMod mod = (MyMod) ModRegistry.get("my-mod-id");
 */
public class ModRegistry {

    private static final String KEY = "rtrmodloader.registry";

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<String, Object> map() {
        ConcurrentHashMap<String, Object> map =
            (ConcurrentHashMap<String, Object>) System.getProperties().get(KEY);
        if (map == null) {
            map = new ConcurrentHashMap<String, Object>();
            System.getProperties().put(KEY, map);
        }
        return map;
    }

    public static void register(String modId, Object instance) {
        map().put(modId, instance);
        System.out.println("[RtRModLoader] Registered mod instance: " + modId);
    }

    public static Object get(String modId) {
        return map().get(modId);
    }
}
