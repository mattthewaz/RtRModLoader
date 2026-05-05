package rtrmodloader;

import com.formdev.flatlaf.FlatDarculaLaf;
import rtrmodloader.model.ModManager;
import rtrmodloader.presenter.ModLoaderPresenter;
import rtrmodloader.view.SwingMainFrame;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Install FlatLaf Darcula (Similar to IntelliJ dark theme)
        FlatDarculaLaf.setup();

        Color accent = new Color(255, 140, 0); // I like how well gray and orange go together
        // Focus ring
        UIManager.put("Component.focusColor", accent);

        Font defaultFont = UIManager.getFont("defaultFont");
        Font sizedFont = defaultFont.deriveFont(13f);
        UIManager.put("defaultFont", sizedFont);

        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Component.arc", 20); // this should round edges, I dunno what actually changes, but it strangely feels better (?)

        // From this point on, Swing will automatically use the new Look and Feel
        SwingUtilities.invokeLater(() -> {
            ModManager model = new ModManager();
            ModLoaderPresenter presenter = new ModLoaderPresenter(model, null);
            SwingMainFrame view = new SwingMainFrame(presenter);
            presenter.setView(view);
            model.loadMods();
        });
    }
}