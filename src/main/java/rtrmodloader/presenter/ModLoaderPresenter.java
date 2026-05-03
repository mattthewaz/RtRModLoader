package rtrmodloader.presenter;

import rtrmodloader.Main;
import rtrmodloader.core.GameLauncher;
import rtrmodloader.model.ModInfo;
import rtrmodloader.model.ModManager;
import rtrmodloader.view.ModLoaderView;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ModLoaderPresenter {
    private final ModManager model;
    private ModLoaderView view;
    private boolean listenerAdded = false;


    public ModLoaderPresenter(ModManager model, ModLoaderView view) {
        this.model = model;
        setView(view); // usa il setter per uniformare
    }

    public void setView(ModLoaderView view) {
        this.view = view;
        if (!listenerAdded && view != null) {
            model.addPropertyChangeListener(evt -> {
                if ("mods".equals(evt.getPropertyName())) {
                    SwingUtilities.invokeLater(() -> {
                        if (this.view != null) { // doppia sicurezza
                            this.view.setMods(model.getMods());
                        }
                    });
                }
            });
            listenerAdded = true;
        }
    }

    public void onRefresh() {
        new Thread(() -> {
            model.loadMods();
            view.appendLog("Mod list refreshed.");
        }).start();
    }

    public void onLaunch() {
        // Check if any mods are enabled (optional)
        List<ModInfo> enabled = model.getEnabledMods();
        if (enabled.isEmpty()) {
            view.showConfirmation("No mods enabled. Launch game anyway?", () -> doLaunch());
        } else {
            doLaunch();
        }
    }

    private void doLaunch() {
        view.appendLog("Launching game with Java Agent...");
        new Thread(() -> {
            try {
                // The agent JAR is the current JAR (where this class is located)
                String agentJar = getAgentJarPath();
                GameLauncher.launch(agentJar, new GameLauncher.GameLauncherCallback() {
                    @Override public void onGameStarting() {
                        SwingUtilities.invokeLater(() -> view.appendLog("Game process started."));
                    }
                    @Override public void onGameOutput(String line) {
                        SwingUtilities.invokeLater(() -> view.appendLog("[GAME] " + line));
                    }
                    @Override public void onGameFinished(int exitCode) {
                        SwingUtilities.invokeLater(() -> view.appendLog("Game exited with code " + exitCode));
                    }
                    @Override public void onGameError(Exception e) {
                        SwingUtilities.invokeLater(() -> view.showError("Game error", e.getMessage()));
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> view.showError("Launch failed", e.getMessage()));
            }
        }).start();
    }

    private String getAgentJarPath() {
        try {
            return new File(Main.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Exception e) {
            // fallback
            return Main.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();
        }
    }

    public void onModsDropped(List<File> files) {
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".zip")) {
                model.installMod(f);
                view.appendLog("Installed mod from " + f.getName());
            } else if (name.endsWith(".jar")) {
                if (model.copyJar(f)) {
                    view.appendLog("Copied mod JAR: " + f.getName());
                } else {
                    view.showError("Copy failed", "Could not copy JAR: " + f.getName());
                }
            } else {
                view.showError("Invalid file", "Only .zip and .jar files are supported.");
            }
        }
        model.loadMods();
    }

    public void onEnableSelected(List<ModInfo> selected) {
        model.enableMods(selected, true);
    }

    public void onDisableSelected(List<ModInfo> selected) {
        model.enableMods(selected, false);
    }

    public void onDeleteSelected(List<ModInfo> selected) {
        view.showConfirmation("Delete selected mods?", () -> {
            model.deleteMods(selected);
            view.appendLog("Deleted selected mods.");
        });
    }

    public void onEnableAll() {
        model.enableMods(model.getMods(), true);
    }

    public void onDisableAll() {
        model.enableMods(model.getMods(), false);
    }
}