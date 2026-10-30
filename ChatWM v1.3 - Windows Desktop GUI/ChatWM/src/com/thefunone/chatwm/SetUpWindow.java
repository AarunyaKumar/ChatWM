package com.thefunone.chatwm;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SetUpWindow extends JFrame {

    private final ChatWM chatWM;

    // Windows 11 Color Palette
    private static final Color WIN11_BG = new Color(243, 243, 243);          // Mica Light Gray
    private static final Color WIN11_SURFACE = Color.WHITE;                  // Input / Card Surface
    private static final Color WIN11_ACCENT = new Color(0, 103, 192);        // Fluent Blue
    private static final Color WIN11_ACCENT_HOVER = new Color(25, 118, 205);
    private static final Color WIN11_TEXT_PRIMARY = new Color(26, 26, 26);
    private static final Color WIN11_TEXT_SECONDARY = new Color(96, 96, 96);
    private static final Color WIN11_BORDER = new Color(209, 209, 209);
    private static final Font WIN11_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font WIN11_FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font WIN11_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);

    private JTextField nameField;
    private PlaceholderTextField ipField;
    private PlaceholderTextField portField;
    private JTextField keyStorePathField;
    private JPasswordField passwordField;
    private JToggleButton eyeButton;
    private char defaultEchoChar;

    private Win11Button browseButton;
    private Win11Button createKeyStoreButton;
    private Win11Button connectButton;

    private JPanel loadingPanel;
    private CircularProgressIndicator progressIndicator;

    private final File appDir;
    private final File dataFile;
    private final String defaultKeyStorePath;

    public SetUpWindow(ChatWM chatWM) {
        this.chatWM = chatWM;

        // Apply system look and feel for native file chooser dialogs
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        String userHome = System.getProperty("user.home");
        this.appDir = new File(userHome + "\\ChatWM");
        if (!this.appDir.exists()) {
            this.appDir.mkdirs();
        }
        this.dataFile = new File(appDir, "data");
        this.defaultKeyStorePath = userHome + "\\ChatWM\\defaultKeyStore.p12";

        initUI();
        loadSavedData();
        updateDefaultPasswordIfNeeded();
    }

    private void initUI() {
        setTitle("ChatWM - Setup Connection");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 16)); // BorderLayout divides in NORTH, SOUTH, EAST, WEST and CENTER
        mainPanel.setBackground(WIN11_BG);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24)); // padding=0
        setContentPane(mainPanel);

        // --- Header Panel with Logo.png ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); // default Layout keeps placing on right and then new line and then places elements
        headerPanel.setOpaque(false); // to show background JFrame

        Image logoImg = loadLogoImage();
        if (logoImg != null) {
            setIconImage(logoImg);
            Image scaledLogo = logoImg.getScaledInstance(36, 36, Image.SCALE_SMOOTH);
            headerPanel.add(new JLabel(new ImageIcon(scaledLogo)));
        }

        JPanel titleTextPanel = new JPanel();
        titleTextPanel.setOpaque(false);
        titleTextPanel.setLayout(new BoxLayout(titleTextPanel, BoxLayout.Y_AXIS)); // BoxLayout adds elements either in row or in column

        JLabel titleLabel = new JLabel("ChatWM Connection Setup");
        titleLabel.setFont(WIN11_HEADER_FONT);
        titleLabel.setForeground(WIN11_TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("Configure your profile and TLS KeyStore credentials");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(WIN11_TEXT_SECONDARY);

        titleTextPanel.add(titleLabel);
        titleTextPanel.add(Box.createVerticalStrut(2)); // 2 pixel of space
        titleTextPanel.add(subtitleLabel);
        headerPanel.add(titleTextPanel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- Form Card Panel ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 4, 7, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Your Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(createWin11Label("Your Name"), gbc);

        nameField = new JTextField(20);
        styleWin11TextField(nameField);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(nameField, gbc);

        // 2. Enter IP Address
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(createWin11Label("Enter IP Address"), gbc);

        ipField = new PlaceholderTextField("localhost", 20);
        styleWin11TextField(ipField);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(ipField, gbc);

        // 3. Enter Port Number
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formPanel.add(createWin11Label("Enter Port Number"), gbc);

        portField = new PlaceholderTextField("0 (0 - 65535)", 20);
        styleWin11TextField(portField);
        ((AbstractDocument) portField.getDocument()).setDocumentFilter(new PortNumberFilter());
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(portField, gbc);

        // 4. Choose Key Store File
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        formPanel.add(createWin11Label("Choose Key Store File"), gbc);

        JPanel filePanel = new JPanel(new BorderLayout(6, 0));
        filePanel.setOpaque(false);
        keyStorePathField = new JTextField(defaultKeyStorePath, 15);
        styleWin11TextField(keyStorePathField);

        JPanel fileButtonsPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        fileButtonsPanel.setOpaque(false);
        browseButton = new Win11Button("Browse...", false);
        browseButton.addActionListener(e -> chooseKeyStoreFile());

        createKeyStoreButton = new Win11Button("Create New...", false);
        createKeyStoreButton.addActionListener(e -> showCreateKeyStoreDialog());

        fileButtonsPanel.add(browseButton);
        fileButtonsPanel.add(createKeyStoreButton);

        filePanel.add(keyStorePathField, BorderLayout.CENTER);
        filePanel.add(fileButtonsPanel, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(filePanel, gbc);

        // 5. KeyStore Password (with Eye Button beside it)
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        formPanel.add(createWin11Label("KeyStore Password"), gbc);

        passwordField = new JPasswordField(20);
        defaultEchoChar = passwordField.getEchoChar();
        if (defaultEchoChar == 0) {
            defaultEchoChar = '\u2022';
        }
        passwordField.setEchoChar(defaultEchoChar);

        JPanel passwordWrapper = createPasswordWithEyePanel(passwordField);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(passwordWrapper, gbc);

        // Auto-fill "password123" when defaultKeyStore.p12 is selected
        keyStorePathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateDefaultPasswordIfNeeded();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateDefaultPasswordIfNeeded();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateDefaultPasswordIfNeeded();
            }
        });

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // --- South Panel (Buttons + Loading Indicator) ---
        JPanel southPanel = new JPanel(new BorderLayout(0, 12));
        southPanel.setOpaque(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);

        connectButton = new Win11Button("Connect", true);
        connectButton.addActionListener(e -> onConnectClicked());

        buttonPanel.add(connectButton);
        southPanel.add(buttonPanel, BorderLayout.NORTH);

        // Loading Section (Hidden initially)
        loadingPanel = new JPanel();
        loadingPanel.setOpaque(false);
        loadingPanel.setLayout(new BoxLayout(loadingPanel, BoxLayout.Y_AXIS));
        loadingPanel.setBorder(new EmptyBorder(8, 0, 4, 0));

        progressIndicator = new CircularProgressIndicator();
        progressIndicator.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel connectingLabel = new JLabel("Connecting... ");
        connectingLabel.setFont(WIN11_FONT);
        connectingLabel.setForeground(WIN11_TEXT_SECONDARY);
        connectingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectingLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        loadingPanel.add(progressIndicator);
        loadingPanel.add(connectingLabel);
        loadingPanel.setVisible(false);

        southPanel.add(loadingPanel, BorderLayout.SOUTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(connectButton);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Loads Logo.png from working directory, classpath resources, or appDir.
     * Falls back to a clean Windows 11 style vector chat icon if the file isn't found yet.
     */
    private Image loadLogoImage() {
        try {
            // 1. Check classpath resources
            URL resUrl = getClass().getResource("/Logo.png");
            if (resUrl == null) {
                resUrl = getClass().getResource("Logo.png");
            }
            if (resUrl != null) {
                return ImageIO.read(resUrl);
            }

            // 2. Check current working directory or appDir
            File[] candidates = {
                    new File("Logo.png"),
                    new File("src/Logo.png"),
                    new File(appDir, "Logo.png")
            };
            for (File f : candidates) {
                if (f.exists() && f.isFile()) {
                    return ImageIO.read(f);
                }
            }
        } catch (IOException ignored) {
        }

        // Fallback vector logo if Logo.png file is missing on disk
        BufferedImage fallback = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = fallback.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(WIN11_ACCENT);
        g2.fillRoundRect(4, 4, 56, 56, 18, 18);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(16, 18, 32, 22, 10, 10);
        int[] px = {22, 30, 22};
        int[] py = {38, 38, 46};
        g2.fillPolygon(px, py, 3);
        g2.dispose();
        return fallback;
    }

    private JLabel createWin11Label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(WIN11_FONT);
        label.setForeground(WIN11_TEXT_PRIMARY);
        return label;
    }

    private void styleWin11TextField(JTextField field) {
        field.setFont(WIN11_FONT);
        field.setForeground(WIN11_TEXT_PRIMARY);
        field.setBackground(WIN11_SURFACE);
        field.setCaretColor(WIN11_TEXT_PRIMARY);
        field.setBorder(new Win11InputBorder(field));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.repaint();
            }
        });
    }

    /**
     * Creates a unified Windows 11 password box with an embedded Eye reveal button on the right.
     */
    private JPanel createPasswordWithEyePanel(JPasswordField passField) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(WIN11_SURFACE);
        wrapper.setBorder(new Win11InputBorder(passField));

        passField.setFont(WIN11_FONT);
        passField.setForeground(WIN11_TEXT_PRIMARY);
        passField.setBackground(WIN11_SURFACE);
        passField.setCaretColor(WIN11_TEXT_PRIMARY);
        passField.setBorder(new EmptyBorder(6, 10, 6, 4));

        passField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                wrapper.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                wrapper.repaint();
            }
        });

        eyeButton = new JToggleButton(new EyeIcon(false));
        eyeButton.setSelectedIcon(new EyeIcon(true));
        eyeButton.setToolTipText("Show / Hide Password");
        eyeButton.setFocusable(false);
        eyeButton.setContentAreaFilled(false);
        eyeButton.setBorder(new EmptyBorder(0, 8, 0, 10));
        eyeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        eyeButton.addActionListener(e -> {
            if (eyeButton.isSelected()) {
                passField.setEchoChar((char) 0); // Show plain text
            } else {
                passField.setEchoChar(defaultEchoChar); // Mask password
            }
        });

        wrapper.add(passField, BorderLayout.CENTER);
        wrapper.add(eyeButton, BorderLayout.EAST);
        return wrapper;
    }

    private void updateDefaultPasswordIfNeeded() {
        String currentPath = keyStorePathField.getText().trim();
        File currentFile = new File(currentPath);
        File defaultFile = new File(defaultKeyStorePath);

        if (currentFile.getName().equalsIgnoreCase("defaultKeyStore.p12")
                || currentFile.getAbsolutePath().equalsIgnoreCase(defaultFile.getAbsolutePath())) {
            passwordField.setText("password123");
        } else {
            passwordField.setText("");
        }
    }

    /**
     * Checks that a file path strictly ends with .p12 or .pfx.
     */
    private boolean isValidKeyStoreExtension(String path) {
        if (path == null) return false;
        String lower = path.trim().toLowerCase();
        return lower.endsWith(".p12") || lower.endsWith(".pfx");
    }

    private void chooseKeyStoreFile() {
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        JFileChooser fileChooser = new JFileChooser(appDir);
        fileChooser.setDialogTitle("Select KeyStore File (.p12, .pfx)");
        // Strictly restrict picker to .p12 and .pfx only
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PKCS12 KeyStore Files (*.p12, *.pfx)", "p12", "pfx");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!isValidKeyStoreExtension(selectedFile.getName())) {
                JOptionPane.showMessageDialog(this,
                        "Only .p12 and .pfx KeyStore files are allowed.",
                        "Invalid File Type",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            keyStorePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void showCreateKeyStoreDialog() {
        JTextField fileNameInput = new JTextField("myKeyStore.p12", 18);
        JTextField aliasInput = new JTextField("chatwm", 18);
        JTextField cnInput = new JTextField(nameField.getText().trim().isEmpty() ? "ChatWMUser" : nameField.getText().trim(), 18);
        JTextField orgInput = new JTextField("ChatWM", 18);
        JPasswordField newPassInput = new JPasswordField(18);
        JPasswordField confirmPassInput = new JPasswordField(18);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {
                "File Name (.p12 or .pfx):",
                "Key Alias:",
                "Common Name (CN):",
                "Organization (O):",
                "KeyStore Password (min 6 chars):",
                "Confirm Password:"
        };
        Component[] fields = {fileNameInput, aliasInput, cnInput, orgInput, newPassInput, confirmPassInput};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0;
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(WIN11_FONT);
            panel.add(lbl, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            fields[i].setFont(WIN11_FONT);
            panel.add(fields[i], gbc);
        }

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Create New KeyStore in " + appDir.getPath(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String fileName = fileNameInput.getText().trim();
            String alias = aliasInput.getText().trim();
            String cn = cnInput.getText().trim();
            String org = orgInput.getText().trim();
            String pass = new String(newPassInput.getPassword());
            String confirmPass = new String(confirmPassInput.getPassword());

            if (fileName.isEmpty() || alias.isEmpty() || cn.isEmpty() || org.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required to generate a KeyStore.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!pass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (pass.length() < 6) {
                JOptionPane.showMessageDialog(this, "KeyStore password must be at least 6 characters long.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!isValidKeyStoreExtension(fileName)) {
                fileName += ".p12";
            }

            if (!appDir.exists()) {
                appDir.mkdirs();
            }

            File targetFile = new File(appDir, fileName);
            if (targetFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this,
                        "File already exists in " + appDir.getPath() + ". Overwrite it?",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
                targetFile.delete();
            }

            boolean created = generateKeyStoreFile(targetFile, alias, cn, org, pass);
            if (created) {
                keyStorePathField.setText(targetFile.getAbsolutePath());
                passwordField.setText(pass);
                JOptionPane.showMessageDialog(this,
                        "KeyStore created successfully at:\n" + targetFile.getAbsolutePath(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private boolean generateKeyStoreFile(File targetFile, String alias, String cn, String org, String password) {
        String keytoolPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool";
        String dname = String.format("CN=%s, O=%s", cn.replace(",", "\\,"), org.replace(",", "\\,"));

        List<String> command = new ArrayList<>();
        command.add(keytoolPath);
        command.add("-genkeypair");
        command.add("-alias");
        command.add(alias);
        command.add("-keyalg");
        command.add("RSA");
        command.add("-keysize");
        command.add("2048");
        command.add("-storetype");
        command.add("PKCS12");
        command.add("-keystore");
        command.add(targetFile.getAbsolutePath());
        command.add("-storepass");
        command.add(password);
        command.add("-keypass");
        command.add(password);
        command.add("-dname");
        command.add(dname);
        command.add("-validity");
        command.add("3650");

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && targetFile.exists()) {
                return true;
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to create KeyStore:\n" + output,
                        "KeyStore Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error executing keytool: " + e.getMessage(),
                    "KeyStore Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void onConnectClicked() {
        String username = nameField.getText().trim();
        String ipAddress = ipField.getText().trim();
        String portText = portField.getText().trim();
        String keystorePath = keyStorePathField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (ipAddress.isEmpty()) {
            ipAddress = "localhost";
        }

        int portNum = 0;
        if (!portText.isEmpty()) {
            try {
                portNum = Integer.parseInt(portText);
                if (portNum < 0 || portNum > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Port number must be between 0 and 65535.",
                        "Invalid Port",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (username.isEmpty() || keystorePath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter your Name and KeyStore file path.",
                    "Incomplete Data",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Enforce .p12 or .pfx extension even if manually typed
        if (!isValidKeyStoreExtension(keystorePath)) {
            JOptionPane.showMessageDialog(this,
                    "KeyStore file must be a .p12 or .pfx file.",
                    "Invalid KeyStore File",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Do not connect if the password field is empty
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "KeyStore password cannot be empty.",
                    "Password Required",
                    JOptionPane.WARNING_MESSAGE);
            passwordField.requestFocusInWindow();
            return;
        }

        setInputsEnabled(false);

        loadingPanel.setVisible(true);
        progressIndicator.start();
        pack();

        final String finalIp = ipAddress;
        final int finalPort = portNum;

        new Thread(() -> {
            if (chatWM != null) {
                chatWM.connect(username, finalIp, finalPort, keystorePath, password);
            }
        }, "ChatWM-Connect-Thread").start();
    }

    public void resetLoading(String errorMessage) {
        if (progressIndicator != null) {
            progressIndicator.stop();
        }
        loadingPanel.setVisible(false);
        setInputsEnabled(true);
        pack();
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    errorMessage,
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void closeSetUp() {
        saveData();
        SwingUtilities.invokeLater(() -> {
            if (progressIndicator != null) {
                progressIndicator.stop();
            }
            dispose();
        });
    }

    private void loadSavedData() {
        if (!dataFile.exists() || !dataFile.isFile()) {
            return;
        }

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(dataFile)))) {
            String savedUsername = dis.readUTF();
            String savedIp = dis.readUTF();
            int savedPort = dis.readInt();
            String savedKeystorePath = dis.readUTF();

            nameField.setText(savedUsername);
            ipField.setText(savedIp);
            if (savedPort >= 0 && savedPort <= 65535) {
                portField.setText(String.valueOf(savedPort));
            }
            if (!savedKeystorePath.isEmpty() && isValidKeyStoreExtension(savedKeystorePath)) {
                keyStorePathField.setText(savedKeystorePath);
            }
        } catch (IOException e) {
            System.err.println("Could not read binary setup data: " + e.getMessage());
        }
    }

    private void saveData() {
        if (!appDir.exists()) {
            appDir.mkdirs();
        }

        String username = nameField.getText().trim();
        String ipAddress = ipField.getText().trim();
        if (ipAddress.isEmpty()) {
            ipAddress = "localhost";
        }

        int portNum = 0;
        String portText = portField.getText().trim();
        if (!portText.isEmpty()) {
            try {
                portNum = Integer.parseInt(portText);
            } catch (NumberFormatException ignored) {
                portNum = 0;
            }
        }

        String keystorePath = keyStorePathField.getText().trim();

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)))) {
            dos.writeUTF(username);
            dos.writeUTF(ipAddress);
            dos.writeInt(portNum);
            dos.writeUTF(keystorePath);
            dos.flush();
        } catch (IOException e) {
            System.err.println("Could not save binary setup data: " + e.getMessage());
        }
    }

    private void setInputsEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        ipField.setEnabled(enabled);
        portField.setEnabled(enabled);
        keyStorePathField.setEnabled(enabled);
        browseButton.setEnabled(enabled);
        createKeyStoreButton.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        eyeButton.setEnabled(enabled);
        connectButton.setEnabled(enabled);
    }

    /**
     * Windows 11 style rounded input border with bottom accent highlight when focused.
     */
    private static class Win11InputBorder extends AbstractBorder {
        private final Component focusTarget;

        public Win11InputBorder(Component focusTarget) {
            this.focusTarget = focusTarget;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean focused = focusTarget != null && focusTarget.hasFocus();
            int arc = 8;

            // Outer subtle rounded border
            g2.setColor(focused ? WIN11_ACCENT : WIN11_BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);

            // Windows 11 bottom accent underline
            if (focused) {
                g2.setColor(WIN11_ACCENT);
                g2.fillRoundRect(x + 2, y + height - 3, width - 4, 3, 2, 2);
            } else {
                g2.setColor(new Color(140, 140, 140));
                g2.drawLine(x + 3, y + height - 1, width - 4, y + height - 1);
            }

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            if (c instanceof JPanel) {
                return new Insets(1, 1, 2, 1);
            }
            return new Insets(6, 10, 7, 10);
        }
    }

    /**
     * Windows 11 Fluent-styled rounded button (supports Primary Accent and Secondary Standard styles).
     */
    private static class Win11Button extends JButton {
        private final boolean primary;
        private boolean hovered = false;

        public Win11Button(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(primary ? WIN11_FONT_BOLD : WIN11_FONT);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(7, 16, 7, 16));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 8;

            if (primary) {
                if (!isEnabled()) {
                    g2.setColor(new Color(160, 195, 230));
                } else if (getModel().isPressed()) {
                    g2.setColor(new Color(0, 84, 158));
                } else if (hovered) {
                    g2.setColor(WIN11_ACCENT_HOVER);
                } else {
                    g2.setColor(WIN11_ACCENT);
                }
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                setForeground(Color.WHITE);
            } else {
                if (!isEnabled()) {
                    g2.setColor(new Color(235, 235, 235));
                    setForeground(new Color(160, 160, 160));
                } else if (getModel().isPressed()) {
                    g2.setColor(new Color(230, 230, 230));
                    setForeground(WIN11_TEXT_PRIMARY);
                } else if (hovered) {
                    g2.setColor(new Color(248, 248, 248));
                    setForeground(WIN11_TEXT_PRIMARY);
                } else {
                    g2.setColor(WIN11_SURFACE);
                    setForeground(WIN11_TEXT_PRIMARY);
                }
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(WIN11_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Vector Eye Icon for viewing/hiding the password.
     */
    private static class EyeIcon implements Icon {
        private final boolean visibleState;

        public EyeIcon(boolean visibleState) {
            this.visibleState = visibleState;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? WIN11_TEXT_SECONDARY : new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int w = getIconWidth();
            int h = getIconHeight();

            // Draw eye outline
            Path2D eye = new Path2D.Double();
            eye.moveTo(x + 1, y + h / 2.0);
            eye.quadTo(x + w / 2.0, y + 1, x + w - 1, y + h / 2.0);
            eye.quadTo(x + w / 2.0, y + h - 1, x + 1, y + h / 2.0);
            g2.draw(eye);

            // Draw pupil
            g2.fillOval(x + w / 2 - 2, y + h / 2 - 2, 5, 5);

            // Draw slash when password is hidden
            if (!visibleState) {
                g2.drawLine(x + 3, y + h - 2, x + w - 3, y + 2);
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }

    /**
     * JTextField with gray placeholder text when empty.
     */
    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        public PlaceholderTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && placeholder != null && !placeholder.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(145, 145, 145));
                g2.setFont(WIN11_FONT);
                Insets insets = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, insets.left, y);
                g2.dispose();
            }
        }
    }

    /**
     * DocumentFilter that only allows valid port numbers from 0 to 65535.
     */
    private static class PortNumberFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string == null) return;
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nextText = new StringBuilder(currentText).insert(offset, string).toString();
            if (isValidPort(nextText)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null) return;
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nextText = new StringBuilder(currentText).replace(offset, offset + length, text).toString();
            if (isValidPort(nextText)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nextText = new StringBuilder(currentText).delete(offset, offset + length).toString();
            if (nextText.isEmpty() || isValidPort(nextText)) {
                super.remove(fb, offset, length);
            }
        }

        private boolean isValidPort(String text) {
            if (text.isEmpty()) return true;
            if (!text.matches("^(0|[1-9]\\d*)$")) {
                return false;
            }
            try {
                int port = Integer.parseInt(text);
                return port >= 0 && port <= 65535;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    /**
     * Windows 11 style CircularProgressIndicator component.
     */
    private static class CircularProgressIndicator extends JComponent {
        private int angle = 0;
        private final Timer timer;

        public CircularProgressIndicator() {
            setPreferredSize(new Dimension(32, 32));
            setMaximumSize(new Dimension(32, 32));
            timer = new Timer(16, e -> {
                angle = (angle + 10) % 360;
                repaint();
            });
        }

        public void start() {
            if (!timer.isRunning()) {
                timer.start();
            }
        }

        public void stop() {
            if (timer.isRunning()) {
                timer.stop();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 6;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(224, 224, 224));
            g2.drawOval(x, y, size, size);

            g2.setColor(WIN11_ACCENT);
            g2.draw(new Arc2D.Double(x, y, size, size, -angle, 250, Arc2D.OPEN));

            g2.dispose();
        }
    }
}