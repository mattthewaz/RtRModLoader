package rtrmodloader.api;

import javassist.CtClass;

/**
 * A single bytecode patch targeting one class.
 * Implementations receive the Javassist CtClass and may call any
 * insertBefore/insertAfter/addMethod/etc. on it before it is compiled.
 */
public interface ModPatch {
    void apply(CtClass cc, ClassLoader loader) throws Exception;
}
