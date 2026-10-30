package com.thefunone.chatwm;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

enum DisplayMessageType {
    JOINED,
    LEFT,
    SIMPLE_CHAT,
    ALERT_CHAT,
    FILE
}

public class ChatWindow {

    ChatManager chatManager;
    ChatWM chatWM;

    final String username;
    final String ipAddress;
    final int portNum;
    String keystorePath, password;

    // Frame & Core UI Components
    public JFrame frame;
    private JPanel chatListPanel;
    private JScrollPane chatScrollPane;
    private JTextArea messageInputArea;
    private JToggleButton alertToggleBtn;
    private JLabel uidBadge;
    private JLabel roleBadge;
    private JLabel onlineLabel;
    private JPopupMenu usersHoverPopup;
    private JPanel infoLogContainer;

    // File Attachment State
    private File selectedFile;
    private byte[] selectedFileBytes;
    private JPanel attachmentPreviewBar;
    private JLabel attachmentNameLabel;

    private static final long MAX_FILE_SIZE = 200L * 1024L * 1024L; // 200 MB

    // Windows 11 Color Palette & Fonts
    private static final Color WIN11_BG = new Color(243, 243, 243);          // Mica Light Gray
    private static final Color WIN11_SURFACE = Color.WHITE;                  // Card / Input Surface
    private static final Color WIN11_ACCENT = new Color(0, 103, 192);        // Fluent Blue
    private static final Color WIN11_ACCENT_HOVER = new Color(25, 118, 205);
    private static final Color WIN11_TEXT_PRIMARY = new Color(26, 26, 26);
    private static final Color WIN11_TEXT_SECONDARY = new Color(96, 96, 96);
    private static final Color WIN11_BORDER = new Color(218, 218, 218);

    // Join / Leave / Alert Specific Colors
    private static final Color DARK_GREEN = new Color(15, 105, 35);
    private static final Color LIGHT_GREEN = new Color(46, 150, 68);
    private static final Color DARK_RED = new Color(168, 20, 20);
    private static final Color LIGHT_RED = new Color(216, 59, 59);
    private static final Color ALERT_RED = new Color(196, 43, 28);
    private static final Color ALERT_BG = new Color(253, 243, 242);

    private static final Font WIN11_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font WIN11_FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font WIN11_SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font WIN11_LARGE_FONT = new Font("Segoe UI", Font.BOLD, 18);

    ChatWindow(ChatWM chatWM, String username, String ipAddress, int portNum, String keystorePath, String password) {
        this.chatWM = chatWM;
        this.username = username;
        this.ipAddress = ipAddress;
        this.portNum = portNum;
        this.keystorePath = keystorePath;
        this.password = password;
        chatManager = new ChatManager(this, username, ipAddress, portNum, keystorePath, password);
    }

    void createFrame() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            frame = new JFrame("ChatWM - " + ipAddress + ":" + portNum);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    closeChatWindow();
                }
            });

            Image logo = loadLogoImage();
            if (logo != null) {
                frame.setIconImage(logo);
            }

            frame.setSize(860, 650);
            frame.setMinimumSize(new Dimension(640, 480));

            // 1. Menu Bar
            frame.setJMenuBar(buildMenuBar());

            // Main Content Container
            JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
            mainContainer.setBackground(WIN11_BG);
            frame.setContentPane(mainContainer);

            // 2. Top Bar + InfoLog Banner Stack
            JPanel northStack = new JPanel();
            northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
            northStack.setOpaque(false);

            infoLogContainer = new JPanel();
            infoLogContainer.setLayout(new BoxLayout(infoLogContainer, BoxLayout.Y_AXIS));
            infoLogContainer.setOpaque(false);

            northStack.add(buildTopBar());
            northStack.add(infoLogContainer);

            mainContainer.add(northStack, BorderLayout.NORTH);

            // 3. Center Chat Area
            mainContainer.add(buildChatArea(), BorderLayout.CENTER);

            // 4. Bottom Typing & Controls Area
            mainContainer.add(buildTypingArea(), BorderLayout.SOUTH);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            messageInputArea.requestFocusInWindow();
        });
    }

    void updateStatus(int UID, boolean isServer, int numberOfOnline) {
        SwingUtilities.invokeLater(() -> {
            if (uidBadge != null) {
                uidBadge.setText("UID:" + UID);
            }

            if (roleBadge != null) {
                roleBadge.setText(isServer ? "Server" : "Client");
                roleBadge.setBackground(isServer ? new Color(228, 241, 254) : new Color(238, 238, 238));
                roleBadge.setForeground(isServer ? WIN11_ACCENT : WIN11_TEXT_PRIMARY);
                roleBadge.repaint();
            }

            if (onlineLabel != null) {
                onlineLabel.setText(Math.max(0, numberOfOnline) + " Online");
            }

            if (frame != null) {
                frame.revalidate();
                frame.repaint();
            }
        });
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(WIN11_SURFACE);
        menuBar.setBorder(new MatteBorder(0, 0, 1, 0, WIN11_BORDER));

        // 1. Control Menu
        JMenu controlMenu = new JMenu("Control");
        controlMenu.setFont(WIN11_FONT);

        JMenuItem setupItem = new JMenuItem("Go to Set-Up Window");
        setupItem.setFont(WIN11_FONT);
        setupItem.addActionListener(e -> {
            closeChatWindow();
            if (chatWM != null) {
                chatWM.setSetUpWindow();
            }
        });

        JMenuItem copyAddressItem = new JMenuItem("Copy IP:Port to Clipboard");
        copyAddressItem.setFont(WIN11_FONT);
        copyAddressItem.addActionListener(e -> {
            String address = ipAddress + ":" + portNum;
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(address), null);
            showInfoLog("Copied " + address + " to clipboard.");
        });

        JMenuItem clearChatItem = new JMenuItem("Clear Chat Screen");
        clearChatItem.setFont(WIN11_FONT);
        clearChatItem.addActionListener(e -> {
            if (chatListPanel != null) {
                chatListPanel.removeAll();
                chatListPanel.revalidate();
                chatListPanel.repaint();
                showInfoLog("Chat history cleared from view.");
            }
        });

        controlMenu.add(setupItem);
        controlMenu.addSeparator();
        controlMenu.add(copyAddressItem);
        controlMenu.add(clearChatItem);

        // 2. View Menu (Suggested Addition)
        JMenu viewMenu = new JMenu("View");
        viewMenu.setFont(WIN11_FONT);

        JCheckBoxMenuItem alwaysOnTopItem = new JCheckBoxMenuItem("Always on Top");
        alwaysOnTopItem.setFont(WIN11_FONT);
        alwaysOnTopItem.addActionListener(e -> {
            if (frame != null) {
                frame.setAlwaysOnTop(alwaysOnTopItem.isSelected());
                showInfoLog("Always on Top " + (alwaysOnTopItem.isSelected() ? "enabled." : "disabled."));
            }
        });
        viewMenu.add(alwaysOnTopItem);

        // 3. Exit Menu
        JMenu exitMenu = new JMenu("Exit");
        exitMenu.setFont(WIN11_FONT);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setFont(WIN11_FONT);
        exitItem.addActionListener(e -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
        exitMenu.add(exitItem);

        menuBar.add(controlMenu);
        menuBar.add(viewMenu);
        menuBar.add(exitMenu);
        return menuBar;
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setBackground(WIN11_SURFACE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, WIN11_BORDER),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // Left Side: IP:Port (Large font) and username (Small text just below)
        JPanel leftInfoPanel = new JPanel();
        leftInfoPanel.setOpaque(false);
        leftInfoPanel.setLayout(new BoxLayout(leftInfoPanel, BoxLayout.Y_AXIS));

        String text = ipAddress + ":" + portNum;
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            text = address.getHostAddress()+":"+portNum+"    Host: "+address.getHostName();
        }catch (Exception _){}
        JLabel ipPortLabel = new JLabel(text);
        ipPortLabel.setFont(WIN11_LARGE_FONT);
        ipPortLabel.setForeground(WIN11_TEXT_PRIMARY);
        ipPortLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userLabel = new JLabel(username);
        userLabel.setFont(WIN11_SMALL_FONT);
        userLabel.setForeground(WIN11_TEXT_SECONDARY);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        leftInfoPanel.add(ipPortLabel);
        leftInfoPanel.add(Box.createVerticalStrut(2));
        leftInfoPanel.add(userLabel);

        topBar.add(leftInfoPanel, BorderLayout.WEST);

        // Right Side: UID, Server/Client Role, and Online Count with Green Dot & Hover Popup
        JPanel rightInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        rightInfoPanel.setOpaque(false);

        // Inside buildTopBar():
        String uidValue = (chatManager != null)
                ? String.valueOf(chatManager.connection.getUserID()) : "0";
        uidBadge = createStatusPill("UID:" + uidValue, new Color(240, 240, 240), WIN11_TEXT_PRIMARY);

        boolean isServer = chatManager != null
                && chatManager.connection != null
                && chatManager.connection.isServer();
        String roleText = isServer ? "Server" : "Client";
        Color roleBg = isServer ? new Color(228, 241, 254) : new Color(238, 238, 238);
        Color roleFg = isServer ? WIN11_ACCENT : WIN11_TEXT_PRIMARY;
        roleBadge = createStatusPill(roleText, roleBg, roleFg);

        // Online Status Pill with Green Dot
        JPanel onlineBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(231, 246, 234));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(new Color(195, 230, 203));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        onlineBadge.setOpaque(false);
        onlineBadge.setBorder(new EmptyBorder(1, 8, 1, 10));
        onlineBadge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel greenDot = new JLabel(new GreenDotIcon());
        onlineLabel = new JLabel(getOnlineUsersCount() + " Online");
        onlineLabel.setFont(WIN11_FONT_BOLD);
        onlineLabel.setForeground(DARK_GREEN);

        onlineBadge.add(greenDot);
        onlineBadge.add(onlineLabel);

        // Hover Listener to show usernames below cursor
        usersHoverPopup = new JPopupMenu();
        usersHoverPopup.setBackground(WIN11_SURFACE);
        usersHoverPopup.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, WIN11_BORDER),
                new EmptyBorder(6, 10, 6, 10)
        ));

        MouseAdapter hoverAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                onlineLabel.setText(getOnlineUsersCount() + " Online");
                showOnlineUsersBelowCursor(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (usersHoverPopup != null) {
                    usersHoverPopup.setVisible(false);
                }
            }
        };
        onlineBadge.addMouseListener(hoverAdapter);
        onlineLabel.addMouseListener(hoverAdapter);
        greenDot.addMouseListener(hoverAdapter);

        rightInfoPanel.add(uidBadge);
        rightInfoPanel.add(roleBadge);
        rightInfoPanel.add(onlineBadge);

        topBar.add(rightInfoPanel, BorderLayout.EAST);
        return topBar;
    }

    public void showInfoLog(String information) {
        if (information == null || information.trim().isEmpty()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (infoLogContainer == null) {
                return;
            }

            JPanel banner = new JPanel(new BorderLayout(10, 0));
            banner.setBackground(new Color(230, 242, 252));
            banner.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 1, 0, new Color(180, 215, 245)),
                    new EmptyBorder(8, 20, 8, 14)
            ));

            JLabel infoLabel = new JLabel(information);
            infoLabel.setFont(WIN11_FONT);
            infoLabel.setForeground(WIN11_TEXT_PRIMARY);

            // Cross button on top-right
            JButton closeBtn = new JButton("✕");
            closeBtn.setFont(WIN11_FONT_BOLD);
            closeBtn.setForeground(WIN11_TEXT_SECONDARY);
            closeBtn.setBorder(new EmptyBorder(0, 6, 0, 6));
            closeBtn.setContentAreaFilled(false);
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeBtn.setToolTipText("Dismiss");

            Timer autoRemoveTimer = new Timer(10_000, null);
            autoRemoveTimer.setRepeats(false);

            Runnable removeBanner = () -> {
                autoRemoveTimer.stop();
                if (banner.getParent() != null) {
                    infoLogContainer.remove(banner);
                    infoLogContainer.revalidate();
                    infoLogContainer.repaint();
                }
            };

            closeBtn.addActionListener(e -> removeBanner.run());
            autoRemoveTimer.addActionListener(e -> removeBanner.run());

            banner.add(infoLabel, BorderLayout.CENTER);
            banner.add(closeBtn, BorderLayout.EAST);

            infoLogContainer.add(banner);
            infoLogContainer.revalidate();
            infoLogContainer.repaint();

            autoRemoveTimer.start();
        });
    }



    private JLabel createStatusPill(String text, Color bg, Color fg) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(WIN11_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setOpaque(false);
        label.setBackground(bg);
        label.setForeground(fg);
        label.setFont(WIN11_FONT_BOLD);
        label.setBorder(new EmptyBorder(5, 12, 5, 12));
        return label;
    }

    private int getOnlineUsersCount() {
        try {
            if (chatManager != null && chatManager.connection != null) {
                return chatManager.connection.getNumberOfUsers();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void showOnlineUsersBelowCursor(MouseEvent e) {
        usersHoverPopup.removeAll();

        JLabel header = new JLabel("Connected Users");
        header.setFont(WIN11_FONT_BOLD);
        header.setForeground(WIN11_TEXT_SECONDARY);
        header.setBorder(new EmptyBorder(0, 0, 4, 0));
        usersHoverPopup.add(header);
        usersHoverPopup.add(new JSeparator());

        try {
            if (chatManager != null && chatManager.connection != null) {
                Object rawList = chatManager.connection.getAllUsernames();
                boolean hasUsers = false;

                if (rawList instanceof Collection<?>) {
                    for (Object item : (Collection<?>) rawList) {
                        addUsernameToPopup(String.valueOf(item));
                        hasUsers = true;
                    }
                } else if (rawList instanceof Object[]) {
                    for (Object item : (Object[]) rawList) {
                        addUsernameToPopup(String.valueOf(item));
                        hasUsers = true;
                    }
                }

                if (!hasUsers) {
                    JLabel empty = new JLabel("No users found");
                    empty.setFont(WIN11_SMALL_FONT);
                    empty.setForeground(WIN11_TEXT_SECONDARY);
                    usersHoverPopup.add(empty);
                }
            }
        } catch (Exception ex) {
            JLabel err = new JLabel("Unable to load user list");
            err.setFont(WIN11_SMALL_FONT);
            err.setForeground(ALERT_RED);
            usersHoverPopup.add(err);
        }

        usersHoverPopup.pack();
        // Show directly below the mouse cursor
        usersHoverPopup.show(e.getComponent(), e.getX(), e.getY() + 18);
    }

    private void addUsernameToPopup(String uname) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setOpaque(false);
        row.add(new JLabel(new GreenDotIcon()));
        JLabel nameLbl = new JLabel(uname);
        nameLbl.setFont(WIN11_FONT);
        nameLbl.setForeground(WIN11_TEXT_PRIMARY);
        row.add(nameLbl);
        usersHoverPopup.add(row);
    }

    private JScrollPane buildChatArea() {
        chatListPanel = new JPanel();
        chatListPanel.setLayout(new BoxLayout(chatListPanel, BoxLayout.Y_AXIS));
        chatListPanel.setBackground(WIN11_BG);
        chatListPanel.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Wrapper keeps messages anchored to the top of the scroll viewport
        JPanel topAlignedWrapper = new JPanel(new BorderLayout());
        topAlignedWrapper.setBackground(WIN11_BG);
        topAlignedWrapper.add(chatListPanel, BorderLayout.NORTH);

        chatScrollPane = new JScrollPane(topAlignedWrapper);
        chatScrollPane.setBorder(null);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return chatScrollPane;
    }

    private JPanel buildTypingArea() {
        JPanel bottomBar = new JPanel(new BorderLayout(0, 8));
        bottomBar.setBackground(WIN11_SURFACE);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, WIN11_BORDER),
                new EmptyBorder(12, 20, 14, 20)
        ));

        // Top Options Row: Alert Toggle + Attach File Button + Attachment Pill
        JPanel optionsRow = new JPanel(new BorderLayout(10, 0));
        optionsRow.setOpaque(false);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftControls.setOpaque(false);

        // Alert Message Toggle
        alertToggleBtn = new JToggleButton("Alert Message");
        alertToggleBtn.setFont(WIN11_FONT_BOLD);
        alertToggleBtn.setFocusPainted(false);
        alertToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        alertToggleBtn.setBackground(WIN11_SURFACE);
        alertToggleBtn.setForeground(WIN11_TEXT_SECONDARY);
        alertToggleBtn.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, WIN11_BORDER),
                new EmptyBorder(5, 12, 5, 12)
        ));
        alertToggleBtn.addActionListener(e -> {
            if (alertToggleBtn.isSelected()) {
                alertToggleBtn.setBackground(ALERT_BG);
                alertToggleBtn.setForeground(ALERT_RED);
                alertToggleBtn.setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 1, 1, 1, ALERT_RED),
                        new EmptyBorder(5, 12, 5, 12)
                ));
            } else {
                alertToggleBtn.setBackground(WIN11_SURFACE);
                alertToggleBtn.setForeground(WIN11_TEXT_SECONDARY);
                alertToggleBtn.setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 1, 1, 1, WIN11_BORDER),
                        new EmptyBorder(5, 12, 5, 12)
                ));
            }
        });

        // Attach File Button
        Win11Button attachBtn = new Win11Button("Attach File", false);
        attachBtn.setIcon(new PaperclipIcon());
        attachBtn.addActionListener(e -> pickFileToAttach());

        leftControls.add(alertToggleBtn);
        leftControls.add(attachBtn);

        // Attached File Preview Bar (Hidden until a valid <200 MB file is picked)
        attachmentPreviewBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        attachmentPreviewBar.setBackground(new Color(235, 243, 252));
        attachmentPreviewBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, WIN11_ACCENT),
                new EmptyBorder(2, 8, 2, 6)
        ));
        attachmentPreviewBar.setVisible(false);

        attachmentNameLabel = new JLabel();
        attachmentNameLabel.setFont(WIN11_SMALL_FONT);
        attachmentNameLabel.setForeground(WIN11_ACCENT);

        JButton removeFileBtn = new JButton("✕");
        removeFileBtn.setFont(WIN11_SMALL_FONT);
        removeFileBtn.setForeground(ALERT_RED);
        removeFileBtn.setBorder(new EmptyBorder(0, 4, 0, 4));
        removeFileBtn.setContentAreaFilled(false);
        removeFileBtn.setFocusPainted(false);
        removeFileBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeFileBtn.addActionListener(e -> clearAttachment());

        attachmentPreviewBar.add(attachmentNameLabel);
        attachmentPreviewBar.add(removeFileBtn);
        leftControls.add(attachmentPreviewBar);

        optionsRow.add(leftControls, BorderLayout.WEST);
        bottomBar.add(optionsRow, BorderLayout.NORTH);

        // Large Text Area + Send Button Row
        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);

        messageInputArea = new JTextArea(3, 30);
        messageInputArea.setFont(WIN11_FONT);
        messageInputArea.setForeground(WIN11_TEXT_PRIMARY);
        messageInputArea.setBackground(WIN11_SURFACE);
        messageInputArea.setLineWrap(true);
        messageInputArea.setWrapStyleWord(true);
        messageInputArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane inputScroll = new JScrollPane(messageInputArea);
        inputScroll.setBorder(new Win11InputBorder(messageInputArea));
        messageInputArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                inputScroll.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                inputScroll.repaint();
            }
        });

        // Ctrl + Enter shortcut to send message
        messageInputArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "sendMsg");
        messageInputArea.getActionMap().put("sendMsg", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCurrentMessage();
            }
        });

        Win11Button sendBtn = new Win11Button("Send", true);
        sendBtn.setPreferredSize(new Dimension(105, 64));
        sendBtn.addActionListener(e -> sendCurrentMessage());

        inputRow.add(inputScroll, BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);

        bottomBar.add(inputRow, BorderLayout.CENTER);
        return bottomBar;
    }

    /**
     * Packs [4-byte filename byte length][filename bytes][raw file bytes] into a single byte[].
     */
    private byte[] packFilePayload(String fileName, byte[] rawFileBytes) {
        byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + nameBytes.length + rawFileBytes.length);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.put(rawFileBytes);
        return buffer.array();
    }

    /**
     * Extracts the filename from the packed byte[].
     */
    private String extractFileName(byte[] packedData, String fallbackName) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(packedData);
            int nameLen = buffer.getInt();
            if (nameLen > 0 && nameLen <= packedData.length - 4) {
                byte[] nameBytes = new byte[nameLen];
                buffer.get(nameBytes);
                return new String(nameBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return (fallbackName != null && !fallbackName.isEmpty()) ? fallbackName : "downloaded_file";
    }

    /**
     * Extracts the raw file bytes from the packed byte[].
     */
    private byte[] extractRawFileBytes(byte[] packedData) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(packedData);
            int nameLen = buffer.getInt();
            if (nameLen >= 0 && nameLen <= packedData.length - 4) {
                byte[] rawBytes = new byte[packedData.length - 4 - nameLen];
                buffer.position(4 + nameLen);
                buffer.get(rawBytes);
                return rawBytes;
            }
        } catch (Exception ignored) {
        }
        return packedData;
    }

    private void pickFileToAttach() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File to Send (Max 200 MB)");
        int result = chooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.exists() || !file.isFile()) {
                return;
            }

            if (file.length() >= MAX_FILE_SIZE) {
                JOptionPane.showMessageDialog(frame,
                        "Selected file is " + formatFileSize(file.length()) + ".\n"
                                + "Please pick a file strictly less than 200 MB.",
                        "File Too Large",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                byte[] rawBytes = Files.readAllBytes(file.toPath());
                // Pack [filename length][filename][file data]
                selectedFileBytes = packFilePayload(file.getName(), rawBytes);
                selectedFile = file;

                attachmentNameLabel.setText(file.getName() + " (" + formatFileSize(rawBytes.length) + ")");
                attachmentPreviewBar.setVisible(true);
                if (messageInputArea.getText().trim().isEmpty()) {
                    messageInputArea.setText(file.getName());
                }
                frame.revalidate();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Failed to read file: " + ex.getMessage(),
                        "File Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearAttachment() {
        selectedFile = null;
        selectedFileBytes = null;
        attachmentPreviewBar.setVisible(false);
        if (frame != null) {
            frame.revalidate();
        }
    }

    private void sendCurrentMessage() {
        String text = messageInputArea.getText().trim();

        if (selectedFile != null && selectedFileBytes != null) {
            // File Message
            String fileCaption = text.isEmpty() ? selectedFile.getName() : text;
            byte[] dataToSend = selectedFileBytes;

            ChatMsg chatMsg = new ChatMsg(ChatMsgType.FILE, username, fileCaption, dataToSend);
            if (chatManager != null && chatManager.connection != null) {
                chatManager.connection.sendData(chatMsg);
            }
            addMessage(DisplayMessageType.FILE, username, fileCaption, dataToSend);

            clearAttachment();
            messageInputArea.setText("");
        } else {
            // Text or Alert Message
            if (text.isEmpty()) {
                return;
            }

            boolean isAlert = alertToggleBtn.isSelected();
            ChatMsgType msgType = isAlert ? ChatMsgType.ALERT : ChatMsgType.SIMPLE_CHAT;
            DisplayMessageType displayType = isAlert ? DisplayMessageType.ALERT_CHAT : DisplayMessageType.SIMPLE_CHAT;

            ChatMsg chatMsg = new ChatMsg(msgType, username, text);
            if (chatManager != null && chatManager.connection != null) {
                chatManager.connection.sendData(chatMsg);
            }
            addMessage(displayType, username, text, null);

            messageInputArea.setText("");
        }

        messageInputArea.requestFocusInWindow();
    }

    void closeChatWindow() {
        chatManager.connection.close();
        SwingUtilities.invokeLater(() -> {
            if (usersHoverPopup != null) {
                usersHoverPopup.setVisible(false);
            }
            if (frame != null) {
                frame.dispose();
            }
        });
    }

    void addMessage(DisplayMessageType dmt, String username, String message, byte[] fileData) {
        SwingUtilities.invokeLater(() -> {
            if (chatListPanel == null) {
                return;
            }

            // Refresh top-bar online counter whenever someone joins/leaves or messages arrive
            if (onlineLabel != null) {
                onlineLabel.setText(getOnlineUsersCount() + " Online");
            }

            JComponent messageComponent;
            switch (dmt) {
                case JOINED:
                    messageComponent = createJoinLeaveBanner(username, true);
                    break;
                case LEFT:
                    messageComponent = createJoinLeaveBanner(username, false);
                    break;
                case SIMPLE_CHAT:
                    messageComponent = createChatCard(username, message, false, null);
                    break;
                case ALERT_CHAT:
                    messageComponent = createChatCard(username, message, true, null);
                    break;
                case FILE:
                    messageComponent = createChatCard(username, message, false, fileData);
                    break;
                default:
                    return;
            }

            chatListPanel.add(messageComponent);
            chatListPanel.add(Box.createVerticalStrut(10));
            chatListPanel.revalidate();
            chatListPanel.repaint();

            // Auto-scroll to newest message at the bottom
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    /**
     * Creates JOINED / LEFT status banners:
     * JOINED -> "-> " + username + " joined the chat!" (username dark green, rest light green)
     * LEFT   -> "-> " + username + " left the chat!"   (username dark red, rest light red)
     */
    private JComponent createJoinLeaveBanner(String user, boolean joined) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color nameColor = joined ? DARK_GREEN : DARK_RED;
        Color restColor = joined ? LIGHT_GREEN : LIGHT_RED;
        String actionText = joined ? " joined the chat!" : " left the chat!";

        JLabel prefixLabel = new JLabel("-> ");
        prefixLabel.setFont(WIN11_FONT);
        prefixLabel.setForeground(restColor);

        JLabel userLabel = new JLabel(user);
        userLabel.setFont(WIN11_FONT_BOLD);
        userLabel.setForeground(nameColor);

        JLabel suffixLabel = new JLabel(actionText);
        suffixLabel.setFont(WIN11_FONT);
        suffixLabel.setForeground(restColor);

        row.add(prefixLabel);
        row.add(userLabel);
        row.add(suffixLabel);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 6));
        return row;
    }

    /**
     * Creates a Windows 11 rounded card displaying:
     * 1. Header with username (and timestamp)
     * 2. Horizontal dividing line
     * 3. Message body (Red if ALERT_CHAT)
     * 4. Download File button below (if FILE)
     */
    private JComponent createChatCard(String sender, String message, boolean isAlert, byte[] fileData) {
        Color cardBg = isAlert ? ALERT_BG : WIN11_SURFACE;
        Color borderColor = isAlert ? ALERT_RED : WIN11_BORDER;

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cardBg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(10, 14, 12, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 1. Username Row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel senderLabel = new JLabel(isAlert ? "⚠ " + sender : sender);
        senderLabel.setFont(WIN11_FONT_BOLD);
        senderLabel.setForeground(isAlert ? ALERT_RED : WIN11_ACCENT);

        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        JLabel timeLabel = new JLabel(timeStr);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(WIN11_TEXT_SECONDARY);

        headerRow.add(senderLabel, BorderLayout.WEST);
        headerRow.add(timeLabel, BorderLayout.EAST);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        card.add(headerRow);
        card.add(Box.createVerticalStrut(6));

        // 2. Dividing Line
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(isAlert ? new Color(240, 180, 175) : WIN11_BORDER);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(separator);
        card.add(Box.createVerticalStrut(8));

        // 3. Message Body
        JTextArea msgBody = new JTextArea(message != null ? message : "");
        msgBody.setEditable(false);
        msgBody.setOpaque(false);
        msgBody.setLineWrap(true);
        msgBody.setWrapStyleWord(true);
        msgBody.setFont(isAlert ? WIN11_FONT_BOLD : WIN11_FONT);
        msgBody.setForeground(isAlert ? ALERT_RED : WIN11_TEXT_PRIMARY);
        msgBody.setBorder(null);
        msgBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(msgBody);

        // 4. Download File Button (Only for FILE messages)
        if (fileData != null) {
            card.add(Box.createVerticalStrut(10));

            String embeddedFileName = extractFileName(fileData, message);
            byte[] rawFileBytes = extractRawFileBytes(fileData);
            String sizeInfo = formatFileSize(rawFileBytes.length);

            Win11Button downloadBtn = new Win11Button("Download " + embeddedFileName + " (" + sizeInfo + ")", false);
            downloadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            downloadBtn.addActionListener(e -> promptAndSaveFile(embeddedFileName, rawFileBytes));
            card.add(downloadBtn);
        }

        return card;
    }

    /**
     * Prompts the user where to save the incoming file and writes byte[] fileData to disk.
     */
    private void promptAndSaveFile(String fileNameFromPacket, byte[] rawFileBytes) {
        JFileChooser saver = new JFileChooser();
        saver.setDialogTitle("Save Received File");

        if (fileNameFromPacket != null && !fileNameFromPacket.trim().isEmpty()) {
            String cleanName = new File(fileNameFromPacket.trim()).getName();
            saver.setSelectedFile(new File(cleanName));
        }

        int choice = saver.showSaveDialog(frame);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File targetFile = saver.getSelectedFile();
            if (targetFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(frame,
                        "File already exists. Do you want to overwrite it?",
                        "Confirm Save",
                        JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(rawFileBytes);
                fos.flush();
                showInfoLog("Saved file: " + targetFile.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Failed to save file: " + ex.getMessage(),
                        "Download Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.2f MB", mb);
    }

    private Image loadLogoImage() {
        try {
            URL resUrl = getClass().getResource("/Logo.png");
            if (resUrl == null) {
                resUrl = getClass().getResource("Logo.png");
            }
            if (resUrl != null) {
                return ImageIO.read(resUrl);
            }
            File[] candidates = {
                    new File("Logo.png"),
                    new File("src/Logo.png"),
                    new File(System.getProperty("user.home") + "\\ChatWM\\Logo.png")
            };
            for (File f : candidates) {
                if (f.exists() && f.isFile()) {
                    return ImageIO.read(f);
                }
            }
        } catch (IOException ignored) {
        }

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

    /**
     * Green Dot Icon for the Online users indicator.
     */
    private static class GreenDotIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(34, 177, 76));
            g2.fillOval(x, y, getIconWidth(), getIconHeight());
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 9;
        }

        @Override
        public int getIconHeight() {
            return 9;
        }
    }

    /**
     * Vector Paperclip Icon for the Attach File button.
     */
    private static class PaperclipIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(WIN11_TEXT_PRIMARY);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D clip = new Path2D.Double();
            clip.moveTo(x + 10, y + 4);
            clip.lineTo(x + 4, y + 10);
            clip.quadTo(x + 2, y + 12, x + 4, y + 14);
            clip.quadTo(x + 6, y + 16, x + 8, y + 14);
            clip.lineTo(x + 14, y + 8);
            clip.quadTo(x + 15, y + 5, x + 12, y + 3);
            clip.quadTo(x + 9, y + 2, x + 7, y + 4);
            clip.lineTo(x + 3, y + 8);
            g2.draw(clip);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    /**
     * Windows 11 style rounded border with bottom accent highlight on focus.
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

            g2.setColor(focused ? WIN11_ACCENT : WIN11_BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);

            if (focused) {
                g2.setColor(WIN11_ACCENT);
                g2.fillRoundRect(x + 2, y + height - 3, width - 4, 3, 2, 2);
            } else {
                g2.setColor(new Color(150, 150, 150));
                g2.drawLine(x + 3, y + height - 1, width - 4, y + height - 1);
            }

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 3, 2);
        }
    }

    /**
     * Windows 11 Fluent-styled rounded button.
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
            setBorder(new EmptyBorder(6, 14, 6, 14));

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
                if (getModel().isPressed()) {
                    g2.setColor(new Color(230, 230, 230));
                } else if (hovered) {
                    g2.setColor(new Color(248, 248, 248));
                } else {
                    g2.setColor(WIN11_SURFACE);
                }
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(WIN11_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                setForeground(WIN11_TEXT_PRIMARY);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}