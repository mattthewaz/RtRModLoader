package rtrmodloader.mods;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.expr.ExprEditor;
import javassist.expr.NewExpr;
import rtrmodloader.api.ModPatch;
import rtrmodloader.api.RtRMod;

import java.util.*;

public class SaveRedirectMod implements RtRMod {

    @Override
    public String getId() {
        return "save-redirect";
    }

    @Override
    public Map<String, List<ModPatch>> getPatches() {
        Map<String, List<ModPatch>> patches = new HashMap<>();
        String[] targets = {
                "rtr/ProfileModule",
                "rtr/SettingsParser",
                "rtr/gui/states/WorldMapGUIData",
                "rtr/gui/states/mainmenu/SelectProfilePanel",
                "rtr/gui/states/shared/SettingsPanel",
                "rtr/gui/states/worldmap/GameModeConfigPanel",
                "rtr/help/HelpModule",
                "rtr/save/RegionalSavedGame",
                "rtr/save/SaveModule",
                "rtr/save/SavedGamesHandler",
                "rtr/save/WorldSavedGame",
                "rtr/states/MapEditorState",
                "rtr/states/MainMenuState",
                "rtr/states/PlayState",
                "rtr/system/gamemodetemplates/GameModeTemplateBase",
                "rtr/utilities/Utilities"
        };

        ModPatch patch = new ModPatch() {
            @Override
            public void apply(CtClass cc, ClassLoader loader) throws Exception {
                cc.instrument(new ExprEditor() {
                    @Override
                    public void edit(NewExpr e) throws CannotCompileException {
                        if ("(Ljava/lang/String;)V".equals(e.getSignature())) {
                            String className = e.getClassName();
                            if ("java.io.File".equals(className) ||
                                    "java.io.FileOutputStream".equals(className) ||
                                    "java.io.FileInputStream".equals(className)) {

                                e.replace(
                                        "{" +
                                                "  String _p = $1;" +
                                                "  if (_p != null && _p.startsWith(\"profiles/\")) {" +
                                                "    String customFolder = System.getProperty(\"rtr.save.folder\", \"profiles\");" +
                                                "    _p = customFolder + \"/\" + _p.substring(9);" +
                                                "  }" +
                                                "  $_ = new " + className + "(_p);" +
                                                "}"
                                );
                            }
                        }
                    }
                });
                System.out.println("[RtRModLoader] Patched " + cc.getName() + " for save redirection");
            }
        };

        for (String target : targets) {
            patches.put(target, Collections.singletonList(patch));
        }
        return patches;
    }
}