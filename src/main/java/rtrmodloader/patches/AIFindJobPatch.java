package rtrmodloader.patches;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class AIFindJobPatch implements ClassFileTransformer {

    private static final String TARGET_CLASS = "rtr/mobs/ai/AIFindJob";

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (!TARGET_CLASS.equals(className)) {
            return null; // null = leave untouched
        }

        System.out.println("[RtRModLoader] Patching AIFindJob...");

        // TODO: use Byte Buddy or Javassist to rewrite findJob() here
        // Return modified bytecode, or classfileBuffer to pass through unchanged
        return classfileBuffer;
    }
}
