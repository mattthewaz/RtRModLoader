package rtrmodloader.api;

import java.util.List;
import java.util.Map;

/**
 * Entry point for a mod.
 * <p>
 * Implement this interface and register it via ServiceLoader
 * (META-INF/services/rtrmodloader.api.RtRMod) in your mod JAR.
 * <p>
 * getPatches() returns a map of internal class names (slash-separated,
 * e.g. "rtr/mobs/ai/AIBuildWork") to the list of patches to apply to
 * that class. Multiple mods may patch the same class; patches are applied
 * in mod-load order.
 */
public interface RtRMod {
    String getId();
    Map<String, List<ModPatch>> getPatches();
}
