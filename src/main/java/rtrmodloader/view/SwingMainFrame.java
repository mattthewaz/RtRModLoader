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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class SwingMainFrame extends JFrame implements ModLoaderView {
    private DefaultListModel<ModInfo> listModel;
    private JList<ModInfo> modList;
    private JTextArea logArea;
    private JPanel leftPanel;
    private final ModLoaderPresenter presenter;
    private JTextArea detailArea;
    private JPanel dropPanel;
    private JPanel detailContainer;
    private JButton openUrlButton;

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

        // Top panel with buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        launchButton = new JButton("🚀 Start game");
        refreshButton = new JButton("🔄 Refresh mods");
        enableAllButton = new JButton("✅ Enable all");
        disableAllButton = new JButton("❌ Disable all");
        saveFolderButton = new JButton("📁 Manage Saves");

        topPanel.add(launchButton);
        topPanel.add(refreshButton);
        topPanel.add(enableAllButton);
        topPanel.add(disableAllButton);
        topPanel.add(saveFolderButton);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // ----- Left Panel -----
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("📦 Installed Mods"));

        dropPanel = new JPanel(new BorderLayout());
        dropPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "📂 Drop .zip or .jar files here"
        ));
        JLabel dropLabel = new JLabel("Drag & drop your mod files here", JLabel.CENTER);
        dropLabel.setFont(dropLabel.getFont().deriveFont(Font.ITALIC));
        dropPanel.add(dropLabel, BorderLayout.CENTER);
        leftPanel.add(dropPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        modList = new JList<>(listModel);
        modList.setFocusable(false);
        modList.setCellRenderer(new ModListRenderer());
        modList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        modList.setComponentPopupMenu(createPopupMenu());
        modList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    int index = modList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        ModInfo mod = listModel.getElementAt(index);
                        List<ModInfo> single = Collections.singletonList(mod);
                        presenter.onToggleSelected(single);
                        e.consume();
                    }
                }
            }
        });
        leftPanel.add(new JScrollPane(modList), BorderLayout.CENTER);

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // ----- Right Panel (I suppose?) -----
        detailArea = new JTextArea(9, 20);
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setFocusable(false);
        detailArea.setWrapStyleWord(true);
        detailContainer = new JPanel(new BorderLayout());
        detailContainer.setBorder(BorderFactory.createTitledBorder("Mod Details"));
        detailContainer.add(new JScrollPane(detailArea), BorderLayout.CENTER);

        verticalSplit.setTopComponent(detailContainer);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("📟 Log"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFocusable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        rightPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        verticalSplit.setBottomComponent(rightPanel);

        // Assign the panels to the main split
        horizontalSplit.setLeftComponent(leftPanel);
        horizontalSplit.setRightComponent(verticalSplit);

        add(horizontalSplit, BorderLayout.CENTER);
    }

    private void initListeners() {
        launchButton.addActionListener(e -> presenter.onLaunch());
        refreshButton.addActionListener(e -> presenter.onRefresh());
        enableAllButton.addActionListener(e -> presenter.onEnableAll());
        disableAllButton.addActionListener(e -> presenter.onDisableAll());
        saveFolderButton.addActionListener(e -> presenter.onManageSaveFolder());
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
        openUrlButton = new JButton("🌐 Open ModPage");
        openUrlButton.setEnabled(false);
        openUrlButton.addActionListener(e -> {
            String url = (String) openUrlButton.getClientProperty("currentUrl");
            if (url != null && !url.isEmpty()) {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new java.net.URI(url));
                    } else {
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new java.awt.datatransfer.StringSelection(url), null);
                        JOptionPane.showMessageDialog(this, "Browser not supported on this platform.\nURL copied to clipboard:\n" + url, "Info", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    showError("Error", "Unable to launch the browser: " + ex.getMessage());
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(openUrlButton);
        detailContainer.add(buttonPanel, BorderLayout.SOUTH);

        // Listener for list selection
        modList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ModInfo selected = modList.getSelectedValue();
                if (selected != null) {
                    // Write the mod description
                    StringBuilder sb = new StringBuilder();
                    sb.append("Name: ").append(selected.getName()).append("\n")
                            .append("Version: ").append(selected.getVersion()).append("\n")
                            .append("Author: ").append(selected.getAuthor().isEmpty() ? "-" : selected.getAuthor()).append("\n");
                    String urlStr = selected.getUrl();
                    if (urlStr != null && !urlStr.isEmpty()) {
                        sb.append("URL: ").append(urlStr).append("\n");
                    }
                    sb.append("\nDescription:\n").append(selected.getDescription().isEmpty() ? "none" : selected.getDescription()).append("\n")
                            .append("\nPath: ").append(selected.getPath()).append("\n");

                    detailArea.setText(sb.toString());

                    // Here, the button is enabled or disabled depending on whether the link is present. I could also hide it in that case, but the GUI would start flickering every time it's toggled on or off.
                    boolean hasUrl = (urlStr != null && !urlStr.isEmpty());
                    openUrlButton.setEnabled(hasUrl);
                    openUrlButton.putClientProperty("currentUrl", hasUrl ? urlStr : null);
                } else {
                    detailArea.setText("");
                    openUrlButton.setEnabled(false);
                    openUrlButton.putClientProperty("currentUrl", null);
                }
            }
        });
        if (modList.getSelectedValue() != null) {
            modList.getSelectedValue();
        }
    }

    private void setupDragAndDrop() {
        Color originalBackground = leftPanel.getBackground();
        Color darkerBackground = originalBackground.darker();

        Border originalBorder = leftPanel.getBorder();
        Border dragOverBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 140, 0), 2),
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
    public void showManageSaveDialog(String currentFolder, List<String> history, Consumer<String> onSelect, Consumer<String> onNew, Consumer<String> onDelete) {
        JDialog dialog = new JDialog(this, "Manage Save Folders", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String f : history) {
            listModel.addElement(f);
        }
        JList<String> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBorder(BorderFactory.createTitledBorder("Previously used folders"));
        list.setFocusable(false);
        dialog.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton selectBtn = new JButton("Select");
        JButton newBtn = new JButton("New...");
        JButton deleteBtn = new JButton("Delete");
        JButton cancelBtn = new JButton("Cancel");

        buttonPanel.add(selectBtn);
        buttonPanel.add(newBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        selectBtn.addActionListener(e -> {
            String selected = list.getSelectedValue();
            if (selected != null) {
                onSelect.accept(selected);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Select a folder from the list.", "No selection", JOptionPane.WARNING_MESSAGE);
            }
        });

        newBtn.addActionListener(e -> {
            String newFolder = JOptionPane.showInputDialog(dialog,
                    "Enter new folder name:\n(Only letters, numbers, underscore and hyphen allowed)",
                    "New Save Folder",
                    JOptionPane.PLAIN_MESSAGE);
            if (newFolder != null && !newFolder.trim().isEmpty()) {
                onNew.accept(newFolder.trim());
                dialog.dispose();
            }
        });

        deleteBtn.setEnabled(false); // Here I resolved the profile deletion problem, really nice

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = list.getSelectedValue();
                boolean isDefault = selected != null && selected.trim().equalsIgnoreCase("profiles");
                deleteBtn.setEnabled(!isDefault);
                if (isDefault) {
                    deleteBtn.setToolTipText("Cannot delete the default 'profiles' folder.");
                } else {
                    deleteBtn.setToolTipText(null);
                }
            }
        });

        // If an element is already selected (e.g., the first one), update the state
        if (list.getSelectedValue() != null && list.getSelectedValue().trim().equalsIgnoreCase("profiles")) {
            deleteBtn.setEnabled(false);
        } else if (list.getSelectedValue() != null) {
            deleteBtn.setEnabled(true);
        }


        deleteBtn.addActionListener(e -> {
            String selected = list.getSelectedValue();
            if (selected != null) {
                if (selected.equals(currentFolder)) {
                    JOptionPane.showMessageDialog(dialog,
                            "Cannot delete the currently active folder.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Remove '" + selected + "' from history?\n(The actual folder on disk will NOT be deleted.)",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    onDelete.accept(selected);
                    listModel.removeElement(selected);
                }
            } else {
                JOptionPane.showMessageDialog(dialog, "Select a folder to delete.", "No selection", JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private static class ModListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            ModInfo mod = (ModInfo) value;
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, mod.toString(), index, isSelected, cellHasFocus);
            label.setForeground(mod.isEnabled() ? new Color(255, 140, 0) : Color.GRAY);

            StringBuilder tip = new StringBuilder("<html>");
            String author = mod.getAuthor();
            if (author != null && !author.isEmpty()) {
                tip.append("<b>Author:</b> ").append(escapeHtml(author)).append("<br>");
            }
            String desc = mod.getDescription();
            if (desc != null && !desc.isEmpty()) {
                tip.append("<b>Description:</b> ").append(escapeHtml(desc));
            } else {
                tip.append("No description available.");
            }
            tip.append("</html>");
            label.setToolTipText(tip.toString());
            return label;
        }

        private String escapeHtml(String text) {
            if (text == null) return "";
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '&':  sb.append("&amp;"); break;
                    case '<':  sb.append("&lt;");  break;
                    case '>':  sb.append("&gt;");  break;
                    case '"':  sb.append("&quot;"); break;
                    case '\'': sb.append("&#39;");  break;
                    default:   sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    private JButton launchButton, refreshButton, enableAllButton, disableAllButton, saveFolderButton;
}