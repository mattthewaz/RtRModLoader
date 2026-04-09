package rtrmodloader.patches;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class GameVersionPatch implements ClassFileTransformer {

    private static final String TARGET_CLASS = "rtr/gui/states/mainmenu/MainMenuPanel";

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (!TARGET_CLASS.equals(className)) {
            return null;
        }

        try {
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(new javassist.ByteArrayClassPath(
                "rtr.gui.states.mainmenu.MainMenuPanel", classfileBuffer));
            CtClass cc = pool.get("rtr.gui.states.mainmenu.MainMenuPanel");
            CtMethod method = cc.getDeclaredMethod("render");

            // Append "RtRModLoader" label just below the version box
            method.insertAfter(
                "font.drawString(" +
                "    (float)(x + 5)," +
                "    (float)(rtr.system.ScaleControl.getInterfaceHeight() - 50)," +
                "    \"RtRModLoader v\" + rtrmodloader.ModLoaderVersion.VERSION," +
                "    rtr.font.Text.FontType.BODY," +
                "    2," +
                "    true" +
                ");"
            );

            byte[] modified = cc.toBytecode();
            cc.detach();
            System.out.println("[RtRModLoader] Patched MainMenuPanel.render()");
            return modified;
        } catch (Exception e) {
            System.err.println("[RtRModLoader] Failed to patch MainMenuPanel.render(): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
