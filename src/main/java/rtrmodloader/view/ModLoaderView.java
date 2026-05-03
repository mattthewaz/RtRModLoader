package rtrmodloader.view;

import rtrmodloader.model.ModInfo;
import java.util.List;

public interface ModLoaderView {
    void setMods(List<ModInfo> mods);
    void appendLog(String message);
    void showError(String title, String message);
    void showConfirmation(String message, Runnable onConfirm);
    void clearSelection();
    // any other methods for interacting with the UI
}