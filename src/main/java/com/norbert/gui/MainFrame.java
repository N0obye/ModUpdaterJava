package com.norbert.gui;

import com.norbert.config.ModConfigWriter;
import com.norbert.model.ModInfo;
import com.norbert.model.ModUpdateResult;
import com.norbert.model.Profile;
import com.norbert.service.ModDownloadService;
import com.norbert.service.ModScanner;
import com.norbert.service.ModUpdateService;
import com.norbert.service.ModrinthUpdateService;
import com.norbert.service.ProfileService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private static final String DEFAULT_MINECRAFT_VERSION = "1.21.10";
    private static final String DEFAULT_MOD_LOADER = "fabric";
    private static final Color APP_BACKGROUND = new Color(239, 244, 241);
    private static final Color HEADER_BACKGROUND = new Color(45, 105, 92);
    private static final Color PANEL_BACKGROUND = new Color(250, 252, 251);
    private static final Color BORDER_COLOR = new Color(210, 222, 216);
    private static final Color TEXT_COLOR = new Color(35, 47, 43);
    private static final Color PRIMARY_BUTTON = new Color(67, 132, 116);
    private static final Color PRIMARY_BUTTON_HOVER = new Color(55, 115, 101);
    private static final Color SECONDARY_BUTTON = new Color(230, 238, 234);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font CONTROL_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private final ProfileService profileService = new ProfileService();
    private final ModScanner scanner = new ModScanner();
    private final ModConfigWriter configWriter = new ModConfigWriter();
    private final ModUpdateService updateService = new ModrinthUpdateService();
    private final ModDownloadService downloadService = new ModDownloadService();

    private JComboBox<Profile> profileComboBox;
    private JTextField minecraftVersionField;
    private JComboBox<String> modLoaderComboBox;
    private JTextField modsDirectoryField;
    private JButton downloadUpdatesButton;
    private JTable modTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JPanel statusPanel;
    private List<Profile> profiles = new ArrayList<>();
    private List<ModInfo> loadedMods = new ArrayList<>();
    private List<ModUpdateResult> lastUpdateResults = new ArrayList<>();
    private File currentModsDir;
    private boolean changingProfileSelection;

    public MainFrame() {
        setTitle("Minecraft Mod Updater");
        setSize(1200, 920);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(APP_BACKGROUND);

        //------FEJLEC PANEL---------
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

        northPanel.add(createHeaderPanel());
        northPanel.add(createTopPanel());

        //-----STATUS FOOTER--------
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(CONTROL_FONT);
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setBorder(
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        );

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);

        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(PANEL_BACKGROUND);
        statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.EAST);

        //------KOMPONENSEK HOZZADASA
        add(northPanel, BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        //-------ADATOK BETOLTESE------
        loadProfiles();

        setVisible(true);
    }

    private JPanel createHeaderPanel() {

        //--------FEJLEC PANEL---------
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(HEADER_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel("Minecraft Mod Updater");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(title, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTopPanel() {

        //------FELSO VEZERLOPANEL KONTENER--------
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer,BoxLayout.Y_AXIS));
        topContainer.setBackground(APP_BACKGROUND);
        topContainer.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel profilePaneUpper = createControlPanel("Profile settings");
        JPanel actionsPane = createControlPanel("Mods directory");

        //-----FORM LETREHOZASA------
        profileComboBox = new JComboBox<>();
        profileComboBox.setPreferredSize(new Dimension(200, 32));

        minecraftVersionField = new JTextField(DEFAULT_MINECRAFT_VERSION, 8);
        modLoaderComboBox = new JComboBox<>(new String[]{"fabric", "forge", "neoforge", "quilt"});
        modLoaderComboBox.setSelectedItem(DEFAULT_MOD_LOADER);
        modsDirectoryField = new JTextField(profileService.getDefaultModsDirectory(), 28);

        //--------BUTTONOK--------
        JButton newProfileButton = new JButton("New Profile");
        JButton saveProfileButton = new JButton("Save Profile");
        JButton deleteProfileButton = new JButton("Delete Profile");
        JButton browseButton = new JButton("Browse");
        JButton loadButton = new JButton("Load Mods");
        JButton checkUpdatesButton = new JButton("Check Updates");
        downloadUpdatesButton = new JButton("Download Updates");
        downloadUpdatesButton.setEnabled(false);

        //--------KOMPONENS STYLING----------
        styleComboBox(profileComboBox);
        styleTextField(minecraftVersionField);
        styleComboBox(modLoaderComboBox);
        styleTextField(modsDirectoryField);
        styleSecondaryButton(newProfileButton);
        styleSecondaryButton(saveProfileButton);
        styleSecondaryButton(deleteProfileButton);
        styleSecondaryButton(browseButton);
        stylePrimaryButton(loadButton);
        stylePrimaryButton(checkUpdatesButton);
        stylePrimaryButton(downloadUpdatesButton);


        addFormControl(profilePaneUpper, 0, 1, "Profile:", profileComboBox);
        addButton(profilePaneUpper, 2, 1, newProfileButton);
        addButton(profilePaneUpper, 3, 1, saveProfileButton);
        addButton(profilePaneUpper, 4, 1, deleteProfileButton);
        addFormControl(profilePaneUpper, 0, 2, "Minecraft version:", minecraftVersionField);
        addFormControl(profilePaneUpper, 2, 2, "Mod loader:", modLoaderComboBox);

        addFormControl(actionsPane, 0, 1, "Mods directory:", modsDirectoryField);
        addButton(actionsPane, 2, 1, browseButton);
        addButton(actionsPane, 0, 2, loadButton);
        addButton(actionsPane, 1, 2, checkUpdatesButton);
        addButton(actionsPane, 2, 2, downloadUpdatesButton);

        //--------Action listenerek regisztralasa------------
        profileComboBox.addActionListener(e -> applySelectedProfile());
        newProfileButton.addActionListener(e -> createNewProfile());
        saveProfileButton.addActionListener(e -> saveSelectedProfile());
        deleteProfileButton.addActionListener(e -> deleteSelectedProfile());
        browseButton.addActionListener(e -> browseModsDirectory(modsDirectoryField));
        loadButton.addActionListener(e -> loadMods());
        checkUpdatesButton.addActionListener(e -> checkUpdates());
        downloadUpdatesButton.addActionListener(e -> downloadUpdates());

        topContainer.add(centerPanel(profilePaneUpper));
        topContainer.add(Box.createVerticalStrut(10));
        topContainer.add(centerPanel(actionsPane));

        return topContainer;
    }

    private JScrollPane createTablePanel() {

        //---------TABLAZAT OSZLOPAI----------
        String[] columns = {
                "Mod name",
                "Version",
                "Modrinth version",
                "Status"
        };

        tableModel = new DefaultTableModel(columns, 0);

        //----tablazat style
        modTable = new JTable(tableModel);
        modTable.setFont(CONTROL_FONT);
        modTable.setForeground(TEXT_COLOR);
        modTable.setRowHeight(30);
        modTable.setShowGrid(false);
        modTable.setIntercellSpacing(new Dimension(0, 0));
        modTable.setSelectionBackground(new Color(80, 214, 183));
        modTable.setSelectionForeground(TEXT_COLOR);
        modTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        modTable.getTableHeader().setBackground(HEADER_BACKGROUND);
        modTable.getTableHeader().setForeground(Color.WHITE);
        modTable.getTableHeader().setPreferredSize(new Dimension(0, 34));

        //--------status oszlop appearance--------------
        modTable.getColumnModel()
                .getColumn(3)
                .setCellRenderer(new StatusCellRenderer());

        //-----gorgeto tablazat letrehozasa--------
        JScrollPane scrollPane = new JScrollPane(modTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 18, 14, 18));
        scrollPane.getViewport().setBackground(PANEL_BACKGROUND);

        return scrollPane;
    }

    private JPanel createControlPanel(String title) {

        //-------Vezerlopanel letrehozasa--------
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        //--------Panel cimek letrehozasa-------
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(HEADER_BACKGROUND);

        //-------GridBagLayout rugalmassagert------
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 5;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 8, 0);
        panel.add(titleLabel, constraints);

        return panel;
    }

    private JPanel centerPanel(JPanel contentPanel) {
        //----panel a CreateTopPanel kozepre igazitasahoz--------
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setBackground(APP_BACKGROUND);
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.add(contentPanel);
        return wrapper;
    }

    private void addFormControl(JPanel panel, int column, int row, String labelText, JComponent component) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = column;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 0, 4, 6);
        panel.add(createLabel(labelText), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = column + 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL; // - ha tobb hely van hozza akkor a mezo vizszintesen nyulhat
        fieldConstraints.insets = new Insets(4, 0, 4, 14);
        panel.add(component, fieldConstraints);
    }

    private void addButton(JPanel panel, int column, int row, JButton button) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 0, 4, 8);
        panel.add(button, constraints);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }

    private void styleTextField(JTextField field) {
        field.setFont(CONTROL_FONT);
        field.setForeground(TEXT_COLOR);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 32));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(CONTROL_FONT);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setBackground(Color.WHITE);
        comboBox.setPreferredSize(new Dimension(comboBox.getPreferredSize().width, 32));
    }

    private void stylePrimaryButton(JButton button) {
        styleButton(button, PRIMARY_BUTTON, Color.WHITE);
        button.addChangeListener(e -> {
            if (button.isEnabled() && button.getModel().isRollover()) {
                button.setBackground(PRIMARY_BUTTON_HOVER);
            } else {
                button.setBackground(PRIMARY_BUTTON);
            }
        });
    }

    private void styleSecondaryButton(JButton button) {
        styleButton(button, SECONDARY_BUTTON, TEXT_COLOR);
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void setStatus(String text) {

        SwingUtilities.invokeLater(() ->
                statusLabel.setText(text));
    }

    private void loadProfiles() {
        profiles = profileService.loadProfiles();
        refreshProfileCombo(profiles.get(0));
        applyProfile(profiles.get(0));
    }

    private void refreshProfileCombo(Profile selectedProfile) {
        changingProfileSelection = true;
        profileComboBox.removeAllItems();

        for (Profile profile : profiles) {
            profileComboBox.addItem(profile);
        }

        profileComboBox.setSelectedItem(selectedProfile);
        changingProfileSelection = false;
    }

    private void applySelectedProfile() {
        if (changingProfileSelection) {
            return;
        }

        Profile selectedProfile = getSelectedProfile();
        if (selectedProfile != null) {
            applyProfile(selectedProfile);
            resetLoadedData();
        }
    }

    private void applyProfile(Profile profile) {
        minecraftVersionField.setText(profile.getMinecraftVersion());
        modLoaderComboBox.setSelectedItem(profile.getModLoader());
        modsDirectoryField.setText(profile.getModsDirectory());
    }

    private void createNewProfile() {
        JTextField nameField = new JTextField("Minecraft " + getMinecraftVersion() + " " + getModLoader(), 20);
        JTextField versionField = new JTextField(getMinecraftVersion(), 10);
        JComboBox<String> loaderComboBox = new JComboBox<>(new String[]{"fabric", "forge", "neoforge", "quilt"});
        loaderComboBox.setSelectedItem(getModLoader());
        JTextField directoryField = new JTextField(getModsDirectory(), 24);
        JButton browseButton = new JButton("Browse");

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addDialogRow(panel, constraints, 0, "Profile name:", nameField);
        addDialogRow(panel, constraints, 1, "Minecraft version:", versionField);
        addDialogRow(panel, constraints, 2, "Mod loader:", loaderComboBox);

        JPanel directoryPanel = new JPanel(new BorderLayout(4, 0));
        directoryPanel.add(directoryField, BorderLayout.CENTER);
        directoryPanel.add(browseButton, BorderLayout.EAST);
        addDialogRow(panel, constraints, 3, "Mods directory:", directoryPanel);

        browseButton.addActionListener(e -> browseModsDirectory(directoryField));

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "New Profile",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Profile profile = new Profile(
                nameField.getText().trim(),
                versionField.getText().trim(),
                loaderComboBox.getSelectedItem().toString(),
                directoryField.getText().trim()
        );

        if (!isProfileValid(profile)) {
            return;
        }

        if (profileService.profileExists(profiles, profile.getName())) {
            JOptionPane.showMessageDialog(this,
                    "A profile with this name already exists.",
                    "Profile already exists",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        profileService.saveProfile(profiles, profile);
        refreshProfileCombo(profile);
        applyProfile(profile);
        resetLoadedData();
    }

    private void addDialogRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            Component component
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(component, constraints);
    }

    private void saveSelectedProfile() {
        Profile selectedProfile = getSelectedProfile();
        if (selectedProfile == null) {
            return;
        }

        selectedProfile.setMinecraftVersion(getMinecraftVersion());
        selectedProfile.setModLoader(getModLoader());
        selectedProfile.setModsDirectory(getModsDirectory());

        if (!isProfileValid(selectedProfile)) {
            return;
        }

        profileService.saveProfiles(profiles);
        refreshProfileCombo(selectedProfile);
        JOptionPane.showMessageDialog(this,
                "Profile saved.",
                "Saving successful",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedProfile() {
        Profile selectedProfile = getSelectedProfile();
        if (selectedProfile == null) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this profile?\n" + selectedProfile.getName()
                        + "\n\nThe mods directory and files will not be deleted.",
                "Deleting profile",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        profiles.remove(selectedProfile);

        if (profiles.isEmpty()) {
            profiles.add(profileService.createDefaultProfile());
        }

        Profile nextProfile = profiles.get(0);
        profileService.saveProfiles(profiles);
        refreshProfileCombo(nextProfile);
        applyProfile(nextProfile);
        resetLoadedData();

        JOptionPane.showMessageDialog(this,
                "Profile deleted.",
                "Delete done",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void browseModsDirectory(JTextField targetField) {
        JFileChooser fileChooser = new JFileChooser(targetField.getText());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            targetField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private boolean isProfileValid(Profile profile) {
        if (profile.getName().isEmpty()
                || profile.getMinecraftVersion().isEmpty()
                || profile.getModLoader().isEmpty()
                || profile.getModsDirectory().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Profile name, Minecraft version, mod loader and mods directory are mandatory",
                    "Profile error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void loadMods() {
        tableModel.setRowCount(0);
        resetUpdateState();
        setStatus("Loading mods...");

        File modsDir = new File(getModsDirectory());
        if (!ensureModsDirectoryExists(modsDir)) {
            return;
        }

        String minecraftVersion = getMinecraftVersion();
        String modLoader = getModLoader();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setValue(0);
        progressBar.setString("Scanning mods...");

        new SwingWorker<LoadModsResult, Void>() {
            @Override
            protected LoadModsResult doInBackground() {
                List<ModInfo> mods = scanner.scanMods(modsDir);
                Exception configError = null;

                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setMaximum(Math.max(mods.size(), 1));
                    progressBar.setValue(0);
                    progressBar.setString("0 / " + mods.size() + " mods");
                    setStatus("Creating config... 0/" + mods.size());
                });

                try {
                    configWriter.writeConfig(modsDir, mods, minecraftVersion, modLoader, progress -> SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        progressBar.setString(progress + " / " + mods.size() + " mods");
                        setStatus("Creating config... " + progress + "/" + mods.size());
                    }));
                } catch (Exception e) {
                    configError = e;
                }

                return new LoadModsResult(modsDir, mods, configError);
            }

            @Override
            protected void done() {
                try {
                    LoadModsResult result = get();
                    currentModsDir = result.modsDir;
                    loadedMods = result.mods;

                    for (ModInfo mod : loadedMods) {
                        tableModel.addRow(new Object[]{
                                mod.getName(),
                                mod.getVersion(),
                                "-",
                                "Not verified"
                        });
                    }

                    setStatus("Loaded " + loadedMods.size() + " mods");

                    if (result.configError != null) {
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Couldn't create config.json file:" + result.configError.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to load mods: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    setStatus("Failed to load mods");
                } finally {
                    progressBar.setIndeterminate(false);
                    progressBar.setString("");
                    progressBar.setVisible(false);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private static class LoadModsResult {
        private final File modsDir;
        private final List<ModInfo> mods;
        private final Exception configError;

        private LoadModsResult(File modsDir, List<ModInfo> mods, Exception configError) {
            this.modsDir = modsDir;
            this.mods = mods;
            this.configError = configError;
        }
    }

    private boolean ensureModsDirectoryExists(File modsDir) {
        if (modsDir.isDirectory()) {
            return true;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "The profile's mods folder does not exist. Would you like to create it?\n" + modsDir.getAbsolutePath(),
                "Create mods directory",
                JOptionPane.YES_NO_OPTION);

        if (result != JOptionPane.YES_OPTION) {
            return false;
        }

        if (!modsDir.mkdirs() && !modsDir.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "Couldn't create mods folder.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void checkUpdates() {
        if (loadedMods.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please load the mods first using the 'Load Mods' button.",
                    "No mods loaded.",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String minecraftVersion = getMinecraftVersion();
        String modLoader = getModLoader();
        setStatus("Checking updates...");

        tableModel.setRowCount(0);
        for (ModInfo mod : loadedMods) {
            tableModel.addRow(new Object[]{
                    mod.getName(),
                    mod.getVersion(),
                    "-",
                    "Checking..."
            });
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);

        progressBar.setMaximum(loadedMods.size());
        progressBar.setValue(0);



        new SwingWorker<List<ModUpdateResult>, Void>() {
            @Override
            protected List<ModUpdateResult> doInBackground() {
                return updateService.checkUpdates(
                        loadedMods,
                        minecraftVersion,
                        modLoader,
                        progress -> SwingUtilities.invokeLater(() -> {

                            progressBar.setValue(progress);

                            progressBar.setString(
                                    progress + " / " + loadedMods.size() + " mods"
                            );

                            setStatus(
                                    "Checking updates... "
                                            + progress
                                            + "/"
                                            + loadedMods.size()
                            );
                        })
                );
            }

            @Override
            protected void done() {
                try {
                    showUpdateResults(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to check for updates: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {

                    progressBar.setValue(progressBar.getMaximum());

                    progressBar.setString(
                            loadedMods.size() + " / " + loadedMods.size() + " mods"
                    );
                    progressBar.setVisible(false);
                    setCursor(Cursor.getDefaultCursor());
                    setStatus("Update check completed");
                }
            }
        }.execute();
    }

    private void showUpdateResults(List<ModUpdateResult> results) {
        lastUpdateResults = results;
        tableModel.setRowCount(0);

        for (ModUpdateResult result : results) {
            ModInfo mod = result.getModInfo();
            tableModel.addRow(new Object[]{
                    mod.getName(),
                    mod.getVersion(),
                    result.getLatestVersion(),
                    result.getStatus()
            });
        }

        boolean hasDownloadableUpdate = hasDownloadableUpdate(results);
        downloadUpdatesButton.setVisible(true);
        downloadUpdatesButton.setEnabled(hasDownloadableUpdate);
        revalidate();
        repaint();
    }

    private void downloadUpdates() {
        if (currentModsDir == null || lastUpdateResults.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please run the update check first.",
                    "No updates available for download.",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!hasDownloadableUpdate(lastUpdateResults)) {
            JOptionPane.showMessageDialog(this,
                    "No updates available for download.",
                    "No updates available for download.",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        downloadUpdatesButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return downloadService.downloadUpdates(
                        currentModsDir,
                        lastUpdateResults,
                        MainFrame.this::setStatus
                );
            }

            @Override
            protected void done() {
                try {
                    int downloadedCount = get();
                    JOptionPane.showMessageDialog(MainFrame.this,
                            downloadedCount + " file(s) downloaded. The old files have been moved to the mods/backup folder.",
                            "Download Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadMods();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to download updates:" + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    downloadUpdatesButton.setEnabled(true);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private boolean hasDownloadableUpdate(List<ModUpdateResult> results) {
        for (ModUpdateResult result : results) {
            if (result.canDownloadUpdate()) {
                return true;
            }
        }

        return false;
    }

    private void resetLoadedData() {
        loadedMods = new ArrayList<>();
        tableModel.setRowCount(0);
        resetUpdateState();
    }

    private void resetUpdateState() {
        lastUpdateResults = new ArrayList<>();
        currentModsDir = null;
        downloadUpdatesButton.setEnabled(false);
    }

    private Profile getSelectedProfile() {
        return (Profile) profileComboBox.getSelectedItem();
    }

    private String getMinecraftVersion() {
        String minecraftVersion = minecraftVersionField.getText().trim();
        return minecraftVersion.isEmpty() ? DEFAULT_MINECRAFT_VERSION : minecraftVersion;
    }

    private String getModLoader() {
        return modLoaderComboBox.getSelectedItem().toString();
    }

    private String getModsDirectory() {
        return modsDirectoryField.getText().trim();
    }
}
