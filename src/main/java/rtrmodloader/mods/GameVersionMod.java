package rtrmodloader.mods;

import javassist.CtClass;
import javassist.CtMethod;
import rtrmodloader.api.ModPatch;
import rtrmodloader.api.RtRMod;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in mod: draws the RtRModLoader version label on the main menu.
 */
public class GameVersionMod implements RtRMod {

    @Override
    public String getId() {
        return "game-version-label";
    }

    @Override
    public Map<String, List<ModPatch>> getPatches() {
        Map<String, List<ModPatch>> patches = new HashMap<String, List<ModPatch>>();
        patches.put("rtr/gui/states/mainmenu/MainMenuPanel",
            Collections.<ModPatch>singletonList(new ModPatch() {
                @Override
                public void apply(CtClass cc, ClassLoader loader) throws Exception {
                    CtMethod method = cc.getDeclaredMethod("render");
                    method.insertAfter(
                        "font.drawString(" +
                        "    (float)(x + 5)," +
                        "    (float)(rtr.system.ScaleControl.getInterfaceHeight() - 50)," +
                        "    \"RtRModLoader v\" + rtrmodloader.core.ModLoaderVersion.VERSION," +
                        "    rtr.font.Text.FontType.BODY," +
                        "    2," +
                        "    true" +
                        ");"
                    );
                    System.out.println("[RtRModLoader] Patched MainMenuPanel.render()");
                }
            }));
        return patches;
    }
}
