package rtrmodloader;

import rtrmodloader.patches.AIFindJobPatch;
import rtrmodloader.patches.GameVersionPatch;

import java.lang.instrument.Instrumentation;

public class ModLoaderAgent {

    public static void premain(String args, Instrumentation inst) {
        System.out.println("[RtRModLoader] Agent loaded v" + ModLoaderVersion.VERSION);
        inst.addTransformer(new GameVersionPatch());
        inst.addTransformer(new AIFindJobPatch());
    }
}
