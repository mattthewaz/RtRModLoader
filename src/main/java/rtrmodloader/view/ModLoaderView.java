package rtrmodloader.view;

import rtrmodloader.model.ModInfo;
import java.util.List;
import java.util.function.Consumer;

public interface ModLoaderView {
    void setMods(List<ModInfo> mods);
    void appendLog(String message);
    void showError(String title, String message);
    void showConfirmation(String message, Runnable onConfirm);
    void showManageSaveDialog(String currentFolder, List<String> history,
                              Consumer<String> onSelect,
                              Consumer<String> onNew,
                              Consumer<String> onDelete);
}