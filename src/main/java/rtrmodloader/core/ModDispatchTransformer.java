package rtrmodloader.core;

import javassist.ByteArrayClassPath;
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

    private final Map<String, List<ModPatch>> patchMap = new HashMap<>();
    private final List<ClassLoader> modClassLoaders;

    public ModDispatchTransformer(List<RtRMod> mods, List<ClassLoader> modClassLoaders) {
        this.modClassLoaders = modClassLoaders;
        for (RtRMod mod : mods) {
            for (Map.Entry<String, List<ModPatch>> entry : mod.getPatches().entrySet()) {
                String className = entry.getKey();
                if (!patchMap.containsKey(className)) {
                    patchMap.put(className, new ArrayList<>());
                }
                patchMap.get(className).addAll(entry.getValue());
            }
        }
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        List<ModPatch> patches = patchMap.get(className);
        if (patches == null || patches.isEmpty()) return null;

        String dotName = className.replace('/', '.');
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(new javassist.LoaderClassPath(loader));
            for (ClassLoader modCl : modClassLoaders) {
                pool.insertClassPath(new javassist.LoaderClassPath(modCl));
            }
            pool.insertClassPath(new ByteArrayClassPath(dotName, classfileBuffer));
            CtClass cc = pool.get(dotName);

            for (ModPatch patch : patches) {
                patch.apply(cc, loader);
            }

            byte[] modified = cc.toBytecode();
            cc.detach();
            return modified;
        } catch (Exception e) {
            ModLogger.error("Failed to patch " + dotName, e);
            return null;
        }
    }
}
