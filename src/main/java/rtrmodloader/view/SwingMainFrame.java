package rtrmodloader.view;

import rtrmodloader.core.ModLogger;
import rtrmodloader.model.ModInfo;
import rtrmodloader.presenter.ModLoaderPresenter;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

public class SwingMainFrame extends JFrame implements ModLoaderView {
    private DefaultListModel<ModInfo> listModel;
    private JList<ModInfo> modList;
    private JTextArea logArea;
    private JPanel leftPanel;
    private ModLoaderPresenter presenter;
    private JTextArea detailArea;
    private JPanel dropPanel;

    public SwingMainFrame(ModLoaderPresenter presenter) {
        this.presenter = presenter;
        initComponents();
        setupModDetails();
        initListeners();
        setupDragAndDrop();
        ModLogger.setCallback(this::appendLog);
        ModLogger.setConsoleOutput(true);
        appendLog("=== Rise to Ruins Mod Loader ===");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Rise to Ruins Mod Loader");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Top panel con pulsanti
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        launchButton = new JButton("🚀 Start game");
        refreshButton = new JButton("🔄 Refresh mods");
        enableAllButton = new JButton("✅ Enable all");
        disableAllButton = new JButton("❌ Disable all");

        topPanel.add(launchButton);
        topPanel.add(refreshButton);
        topPanel.add(enableAllButton);
        topPanel.add(disableAllButton);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // ----- PANNELLO SINISTRO -----
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("📦 Installed Mods"));

        // 1. Area drop in alto
        dropPanel = new JPanel(new BorderLayout());
        dropPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "📂 Drop .zip or .jar files here"
        ));
        JLabel dropLabel = new JLabel("Drag & drop your mod files here", JLabel.CENTER);
        dropLabel.setFont(dropLabel.getFont().deriveFont(Font.ITALIC));
        dropPanel.add(dropLabel, BorderLayout.CENTER);
        leftPanel.add(dropPanel, BorderLayout.NORTH);

        // 2. Lista mod al centro
        listModel = new DefaultListModel<>();
        modList = new JList<>(listModel);
        modList.setCellRenderer(new ModListRenderer());
        modList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        modList.setComponentPopupMenu(createPopupMenu());
        leftPanel.add(new JScrollPane(modList), BorderLayout.CENTER);

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 3. Dettagli mod
        detailArea = new JTextArea(9, 20);
        detailArea.setEditable(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailArea.setBackground(new Color(240, 240, 240));
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        JPanel detailContainer = new JPanel(new BorderLayout());
        detailContainer.setBorder(BorderFactory.createTitledBorder("Mod Details"));
        detailContainer.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        verticalSplit.setTopComponent(detailContainer);

        // 4. Log
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("📟 Log"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        rightPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        verticalSplit.setBottomComponent(rightPanel);

        // Assegna i pannelli allo split principale
        horizontalSplit.setLeftComponent(leftPanel);
        horizontalSplit.setRightComponent(verticalSplit);

        add(horizontalSplit, BorderLayout.CENTER);
    }

    private void initListeners() {
        launchButton.addActionListener(e -> presenter.onLaunch());
        refreshButton.addActionListener(e -> presenter.onRefresh());
        enableAllButton.addActionListener(e -> presenter.onEnableAll());
        disableAllButton.addActionListener(e -> presenter.onDisableAll());
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem enableItem = new JMenuItem("Enable");
        JMenuItem disableItem = new JMenuItem("Disable");
        JMenuItem deleteItem = new JMenuItem("Delete");

        enableItem.addActionListener(e -> presenter.onEnableSelected(modList.getSelectedValuesList()));
        disableItem.addActionListener(e -> presenter.onDisableSelected(modList.getSelectedValuesList()));
        deleteItem.addActionListener(e -> presenter.onDeleteSelected(modList.getSelectedValuesList()));

        menu.add(enableItem);
        menu.add(disableItem);
        menu.addSeparator();
        menu.add(deleteItem);
        return menu;
    }

    private void setupModDetails() {
        modList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ModInfo selected = modList.getSelectedValue();
                if (selected != null) {
                    String text = "Name: " + selected.getName() + "\n"
                            + "Version: " + selected.getVersion() + "\n"
                            + "Author: " + (selected.getAuthor().isEmpty() ? "-" : selected.getAuthor()) + "\n"
                            + "Description:\n" + (selected.getDescription().isEmpty() ? "none" : selected.getDescription()) + "\n"
                            + "Path: " + selected.getPath() + "\n";
                    detailArea.setText(text);
                } else {
                    detailArea.setText("");
                }
            }
        });
    }

    private void setupDragAndDrop() {
        Color originalBackground = leftPanel.getBackground();
        Color darkerBackground = originalBackground.darker(); // scurisce

        Border originalBorder = leftPanel.getBorder();
        Border dragOverBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GREEN, 2),
                "📂 Release to install"
        );

        DropTargetListener listener = new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    SwingUtilities.invokeLater(() -> {
                        modList.setBackground(darkerBackground);
                        dropPanel.setBackground(darkerBackground);
                        leftPanel.setBackground(darkerBackground);
                        leftPanel.setBorder(dragOverBorder);
                    });
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                SwingUtilities.invokeLater(() -> {
                    leftPanel.setBackground(originalBackground);
                    modList.setBackground(originalBackground);
                    dropPanel.setBackground(originalBackground);
                    leftPanel.setBorder(originalBorder);
                });
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable t = dtde.getTransferable();
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        presenter.onModsDropped(files);
                        dtde.dropComplete(true);
                    } else {
                        dtde.dropComplete(false);
                    }
                } catch (Exception ex) {
                    appendLog("Drag & drop error: " + ex.getMessage());
                    dtde.dropComplete(false);
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        modList.setBackground(originalBackground);
                        dropPanel.setBackground(originalBackground);
                        leftPanel.setBackground(originalBackground);
                        leftPanel.setBorder(originalBorder);
                    });
                }
            }
        };
        new DropTarget(leftPanel, listener);
    }

    @Override
    public void setMods(List<ModInfo> mods) {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (ModInfo m : mods) listModel.addElement(m);
        });
    }

    @Override
    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void showError(String title, String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE));
    }

    @Override
    public void showConfirmation(String message, Runnable onConfirm) {
        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(this, message, "Confirm", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) onConfirm.run();
        });
    }

    @Override
    public void clearSelection() {
        SwingUtilities.invokeLater(() -> modList.clearSelection());
    }

    private static class ModListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            ModInfo mod = (ModInfo) value;
            JLabel label = (JLabel) super.getListCellRendererComponent(list, mod.toString(), index, isSelected, cellHasFocus);
            label.setForeground(mod.isEnabled() ? Color.BLACK : Color.GRAY);
            StringBuilder tip = new StringBuilder("<html>");
            if (mod.getAuthor() != null && !mod.getAuthor().isEmpty()) {
                tip.append("<b>Author:</b> ").append(mod.getAuthor()).append("<br>");
            }
            if (mod.getDescription() != null && !mod.getDescription().isEmpty()) {
                tip.append("<b>Description:</b> ").append(mod.getDescription());
            } else {
                tip.append("No description available.");
            }
            tip.append("</html>");
            label.setToolTipText(tip.toString());
            return label;
        }
    }

    private JButton launchButton, refreshButton, enableAllButton, disableAllButton;
}