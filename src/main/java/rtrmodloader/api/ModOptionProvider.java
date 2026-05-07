package rtrmodloader.api;

import java.util.List;

/**
 * Optional interface for mods that expose user-facing configurable options.
 * Implement alongside {@link RtRMod} — existing mods require no changes.
 */
public interface ModOptionProvider {
    List<ModOption> getOptions();
}
