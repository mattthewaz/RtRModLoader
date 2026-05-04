package rtrmodloader.presenter;

import rtrmodloader.Main;
import rtrmodloader.core.GameLauncher;
import rtrmodloader.model.ModInfo;
import rtrmodloader.model.ModManager;
import rtrmodloader.view.ModLoaderView;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class ModLoaderPresenter {
    private final ModManager model;
    private ModLoaderView view;
    private boolean listenerAdded = false;


    public ModLoaderPresenter(ModManager model, ModLoaderView view) {
        this.model = model;
        setView(view);
    }

    public void setView(ModLoaderView view) {
        this.view = view;
        if (!listenerAdded && view != null) {
            model.addPropertyChangeListener(evt -> {
                if ("mods".equals(evt.getPropertyName())) {
                    SwingUtilities.invokeLater(() -> {
                        if (this.view != null) {
                            this.view.setMods(model.getMods());
                        }
                    });
                } else if ("deleteFailed".equals(evt.getPropertyName())) {
                    // Receives a notification that the deletion failed
                    @SuppressWarnings("unchecked")
                    List<String> failedIds = (List<String>) evt.getNewValue();
                    if (failedIds != null && !failedIds.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            if (this.view != null) {
                                this.view.showError(
                                        "Deletion failed",
                                        "Could not delete mods: " + String.join(", ", failedIds)
                                );
                            }
                        });
                    }
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
            view.showConfirmation("No mods enabled. Launch game anyway?", this::doLaunch);
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
                String saveFolder = model.getSaveFolderManager().getCurrentFolder();
                GameLauncher.launch(agentJar, saveFolder, new GameLauncher.GameLauncherCallback() {
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

    public void onManageSaveFolder() {
        String current = model.getSaveFolderManager().getCurrentFolder();
        List<String> history = model.getSaveFolderManager().getHistory();

        view.showManageSaveDialog(current, history,
            folder -> { // onSelect: use an existing folder
                model.getSaveFolderManager().setCurrentFolder(folder);
                view.appendLog("Save folder changed to: " + folder);
                view.appendLog("Restart the game for changes to take effect.");
            },
            newFolder -> { // onNew: creates a new folder
                model.getSaveFolderManager().setCurrentFolder(newFolder);
                view.appendLog("New save folder created and set: " + newFolder);
                view.appendLog("Restart the game for changes to take effect.");
            },
            folder -> { // onDelete: remove from history
                boolean removed = model.getSaveFolderManager().removeFromHistory(folder);
                if (removed) {
                    view.appendLog("Removed '" + folder + "' from history (folder data remains on disk).");
                }
            }
        );
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
            String modId = model.getModIdFromFile(f);
            if (modId == null) {
                view.showError("Invalid file", "Could not identify mod ID from " + f.getName());
                continue;
            }

            boolean alreadyInstalled = model.isModInstalled(f);

            if (name.endsWith(".zip")) {
                if (alreadyInstalled) {
                    view.showConfirmation(
                            "Mod '" + modId + "' already exists. Overwrite?",
                            () -> {
                                model.installMod(f);
                                view.appendLog("Overwrote mod from " + f.getName());
                            }
                    );
                } else {
                    model.installMod(f);
                    view.appendLog("Installed mod from " + f.getName());
                }
            } else if (name.endsWith(".jar")) {
                if (alreadyInstalled) {
                    view.showConfirmation("Mod '" + modId + "' already exists. Overwrite?",
                            () -> {
                                boolean success = model.copyJar(f);
                                if (success) {
                                    view.appendLog("Overwrote mod JAR: " + f.getName());
                                } else {
                                    view.showError("Copy failed", "Could not copy JAR: " + f.getName());
                                }
                            });
                } else {
                    boolean success = model.copyJar(f);
                    if (success) {
                        view.appendLog("Copied mod JAR: " + f.getName());
                    } else {
                        view.showError("Copy failed", "Could not copy JAR: " + f.getName());
                    }
                }
            }
        }
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