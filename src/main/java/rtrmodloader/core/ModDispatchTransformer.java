package rtrmodloader.core;

import javassist.ClassPool;
import javassist.CtClass;
import rtrmodloader.api.ModPatch;
import rtrmodloader.api.RtRMod;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single ClassFileTransformer that dispatches to all registered mod patches.
 * Replaces one-transformer-per-patch with a single map lookup per class load.
 */

public class ModDispatchTransformer implements ClassFileTransformer {

    private final Map<String, List<ModPatch>> patchMap;
    private final ClassPool pool;

    public ModDispatchTransformer(List<RtRMod> mods) {
        this.patchMap = new HashMap<>();
        for (RtRMod mod : mods) {
            for (Map.Entry<String, List<ModPatch>> entry : mod.getPatches().entrySet()) {
                String className = entry.getKey();
                patchMap.computeIfAbsent(className, k -> new ArrayList<>())
                        .addAll(entry.getValue());
            }
        }

        pool = ClassPool.getDefault();

        // Classpaths added only once
        pool.insertClassPath(new javassist.LoaderClassPath(ClassLoader.getSystemClassLoader()));
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {

        List<ModPatch> patches = patchMap.get(className);
        if (patches == null || patches.isEmpty()) return null;

        String dotName = className.replace('/', '.');
        try {
            // Instead of inserting a new classpath, create the CtClass directly.
            synchronized (pool) {   // guard all pool access
                CtClass cc = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
            // If the original class was already loaded (e.g., during retransformation),
            // we must copy the original class's attributes to avoid verification errors.
            // This is a safe guard for Java agents; for a first-load scenario it's optional.
            // cc.setName(dotName);  // makeClass already sets the name correctly

            // Apply all patches
                for (ModPatch patch : patches) {
                    patch.apply(cc, loader);
                }
                byte[] modified = cc.toBytecode();
                cc.detach();
                return modified;
            }
        } catch (Exception e) {
            // TODO: If an error occurs, the transformer returns null (no changes). It might be useful to at least log the full stack trace and consider failing the class loading to avoid inconsistent states in the game.
            ModLogger.error("Failed to patch " + dotName, e);
            return null;
        }
    }
}
