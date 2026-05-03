package rtrmodloader;

import rtrmodloader.model.ModManager;
import rtrmodloader.presenter.ModLoaderPresenter;
import rtrmodloader.view.SwingMainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ModManager model = new ModManager();
            ModLoaderPresenter presenter = new ModLoaderPresenter(model, null);
            SwingMainFrame view = new SwingMainFrame(presenter);
            presenter.setView(view);
            model.loadMods();
        });
    }
}