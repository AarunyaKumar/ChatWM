package com.thefunone.chatwm;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

enum DisplayMessageType {
    JOINED,
    LEFT,
    SIMPLE_CHAT,
    ALERT_CHAT,
    FILE
}

public class ChatActivity extends AppCompatActivity {

    public static ChatActivity currentInstance;

    ChatManager chatManager;
    private String username;
    private String ipAddress;
    private int portNum;
    private String hostname = "localhost";

    // UI Elements
    private LinearLayout rootChatLayout, topBarLayout, bottomInputSection, llMessagesContainer;
    private View topDivider, bottomDivider;
    private TextView tvIpPortHost, tvTopUsername, tvUidBadge, tvRoleBadge, tvOnlineBadge, tvAttachedFileStatus;
    private ImageButton btnMoreOptions;
    private ScrollView svMessages;
    private MaterialButton btnAlertMessage, btnAttachFile;
    private Button btnSend;
    private EditText etMessageInput;

    // State for Alert Toggle and File Attachment
    private boolean isAlertMode = false;
    private byte[] pendingFileData = null;
    private byte[] pendingDownloadBytes = null;

    private ActivityResultLauncher<String[]> attachFileLauncher;
    private ActivityResultLauncher<String> saveFileLauncher;

    private boolean isExiting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentInstance = this;
        setContentView(R.layout.activity_chat);

        this.chatManager = MainActivity.chatManager;

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        ipAddress = intent.getStringExtra("ipAddress");
        portNum = intent.getIntExtra("portNum", 0);

        initViews();
        applyThemeColorsToViews();
        setupKeyboardMovement();
        setupFileLaunchers();
        setupTopBar();
        setupBottomBarListeners();

        if (this.chatManager != null) {
            this.chatManager.chatActivity = this;
            this.chatManager.setChatActivity(this);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                exitChat();
            }
        });
    }

    /**
     * Checks if the device is currently in Dark Mode based on system settings.
     */
    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void initViews() {
        rootChatLayout = findViewById(R.id.rootChatLayout);
        topBarLayout = findViewById(R.id.topBarLayout);
        bottomInputSection = findViewById(R.id.bottomInputSection);
        topDivider = findViewById(R.id.topDivider);
        bottomDivider = findViewById(R.id.bottomDivider);

        tvIpPortHost = findViewById(R.id.tvIpPortHost);
        tvTopUsername = findViewById(R.id.tvTopUsername);
        tvUidBadge = findViewById(R.id.tvUidBadge);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        tvOnlineBadge = findViewById(R.id.tvOnlineBadge);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);

        svMessages = findViewById(R.id.svMessages);
        llMessagesContainer = findViewById(R.id.llMessagesContainer);

        btnAlertMessage = findViewById(R.id.btnAlertMessage);
        btnAttachFile = findViewById(R.id.btnAttachFile);
        tvAttachedFileStatus = findViewById(R.id.tvAttachedFileStatus);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);
    }

    /**
     * Styles every container, badge, input field, and button for Dark or Light mode.
     */
    private void applyThemeColorsToViews() {
        boolean dark = isDarkMode();

        int bgScreen = Color.parseColor(dark ? "#121212" : "#FFFFFF");
        int bgBar = Color.parseColor(dark ? "#1E1E1E" : "#F9FAFB");
        int dividerColor = Color.parseColor(dark ? "#2E2E2E" : "#E5E7EB");
        int textPrimary = Color.parseColor(dark ? "#F9FAFB" : "#111827");
        int textSecondary = Color.parseColor(dark ? "#9CA3AF" : "#6B7280");
        int borderColor = Color.parseColor(dark ? "#4B5563" : "#D1D5DB");

        rootChatLayout.setBackgroundColor(bgScreen);
        svMessages.setBackgroundColor(bgScreen);
        topBarLayout.setBackgroundColor(bgBar);
        bottomInputSection.setBackgroundColor(bgBar);
        topDivider.setBackgroundColor(dividerColor);
        bottomDivider.setBackgroundColor(dividerColor);

        tvIpPortHost.setTextColor(textPrimary);
        tvTopUsername.setTextColor(textSecondary);
        btnMoreOptions.setImageTintList(ColorStateList.valueOf(textPrimary));

        // UID Badge
        tvUidBadge.setTextColor(Color.parseColor(dark ? "#E5E7EB" : "#1F2937"));
        tvUidBadge.setBackground(createPillDrawable(
                Color.parseColor(dark ? "#2C2C2C" : "#EEEEEE"),
                Color.parseColor(dark ? "#4B5563" : "#D1D5DB")
        ));

        // Server / Client Badge
        tvRoleBadge.setTextColor(Color.parseColor(dark ? "#93C5FD" : "#0B63CE"));
        tvRoleBadge.setBackground(createPillDrawable(
                Color.parseColor(dark ? "#1E3A5F" : "#E8F1FD"),
                Color.parseColor(dark ? "#2563EB" : "#BFDBFE")
        ));

        // Online Badge
        tvOnlineBadge.setTextColor(Color.parseColor(dark ? "#86EFAC" : "#137333"));
        tvOnlineBadge.setBackground(createPillDrawable(
                Color.parseColor(dark ? "#143321" : "#E6F4EA"),
                Color.parseColor(dark ? "#16A34A" : "#A8DAB5")
        ));

        // Input Box
        GradientDrawable inputBoxDrawable = new GradientDrawable();
        inputBoxDrawable.setShape(GradientDrawable.RECTANGLE);
        inputBoxDrawable.setCornerRadius(12f);
        inputBoxDrawable.setColor(Color.parseColor(dark ? "#2C2C2C" : "#F3F4F6"));
        inputBoxDrawable.setStroke(2, borderColor);
        etMessageInput.setBackground(inputBoxDrawable);
        etMessageInput.setTextColor(textPrimary);
        etMessageInput.setHintTextColor(textSecondary);

        // Attach & Alert Buttons
        btnAttachFile.setTextColor(textPrimary);
        btnAttachFile.setStrokeColor(ColorStateList.valueOf(borderColor));
        tvAttachedFileStatus.setTextColor(Color.parseColor(dark ? "#93C5FD" : "#0B63CE"));
        applyAlertModeUI(isAlertMode);

        // Send Button
        btnSend.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(dark ? "#3B82F6" : "#0060B6")));
        btnSend.setTextColor(Color.WHITE);
    }

    private void setupKeyboardMovement() {
        ViewCompat.setOnApplyWindowInsetsListener(rootChatLayout, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom);
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);

            if (ime.bottom > 0) {
                svMessages.post(() -> svMessages.fullScroll(View.FOCUS_DOWN));
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupTopBar() {
        tvTopUsername.setText(username != null ? username : "");
        tvIpPortHost.setText(ipAddress + ":" + portNum + "   Host: " + hostname);

        new Thread(() -> {
            try {
                InetAddress addr = InetAddress.getByName(ipAddress);
                String resolvedHost = addr.getHostName();
                if (resolvedHost != null && !resolvedHost.isEmpty()) {
                    hostname = resolvedHost;
                    runOnUiThread(() -> tvIpPortHost.setText(ipAddress + ":" + portNum + "   Host: " + hostname));
                }
            } catch (Exception ignored) {}
        }).start();

        if (chatManager != null && chatManager.connection != null) {
            updateStatus(
                    chatManager.connection.getUserID(),
                    chatManager.connection.isServer(),
                    chatManager.connection.getNumberOfUsers()
            );
        }

        tvOnlineBadge.setOnHoverListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                showOnlineUsersPopup(v);
                return true;
            }
            return false;
        });
        tvOnlineBadge.setOnClickListener(this::showOnlineUsersPopup);

        btnMoreOptions.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(ChatActivity.this, v);
            popupMenu.getMenu().add(0, 1, 0, "Exit Chat");
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    exitChat();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });
    }

    private void showOnlineUsersPopup(View anchor) {
        if (chatManager == null || chatManager.connection == null) return;
        List<String> usernames = chatManager.connection.getAllUsernames();
        if (usernames == null || usernames.isEmpty()) return;

        ListPopupWindow popupWindow = new ListPopupWindow(this);
        popupWindow.setAnchorView(anchor);
        popupWindow.setWidth(450);
        popupWindow.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                usernames
        ));
        popupWindow.show();
    }

    void exitChat() {
        if (isExiting) return;
        isExiting = true;

        new Thread(() -> {
            try {
                if (chatManager != null && chatManager.connection != null) {
                    chatManager.connection.close();
                }
            } catch (Exception ignored) {}
        }).start();

        if (chatManager != null) {
            chatManager.chatActivity = null;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && !isExiting) {
            exitChat();
        }
    }

    private void setupFileLaunchers() {
        attachFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        packFileIntoByteArray(uri);
                    }
                }
        );

        saveFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("*/*"),
                uri -> {
                    if (uri != null && pendingDownloadBytes != null) {
                        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                            if (os != null) {
                                os.write(pendingDownloadBytes);
                                os.flush();
                                showInfomation("File downloaded successfully!");
                            }
                        } catch (Exception e) {
                            showInfomation("Download failed: " + e.getMessage());
                        } finally {
                            pendingDownloadBytes = null;
                        }
                    }
                }
        );
    }

    private void packFileIntoByteArray(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "attached_file";
            }

            byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
            if (nameBytes.length > 255) {
                String truncated = fileName.substring(fileName.length() - 100);
                nameBytes = truncated.getBytes(StandardCharsets.UTF_8);
            }

            ByteArrayOutputStream fileContentBuffer = new ByteArrayOutputStream();
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    byte[] chunk = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(chunk)) != -1) {
                        fileContentBuffer.write(chunk, 0, bytesRead);
                    }
                }
            }

            byte[] rawFileBytes = fileContentBuffer.toByteArray();
            pendingFileData = new byte[1 + nameBytes.length + rawFileBytes.length];

            pendingFileData[0] = (byte) nameBytes.length;
            System.arraycopy(nameBytes, 0, pendingFileData, 1, nameBytes.length);
            System.arraycopy(rawFileBytes, 0, pendingFileData, 1 + nameBytes.length, rawFileBytes.length);

            tvAttachedFileStatus.setText("Attached: " + fileName);
            showInfomation("Attached " + fileName);
        } catch (Exception e) {
            showInfomation("Failed to attach file: " + e.getMessage());
        }
    }

    private void applyAlertModeUI(boolean alertActive) {
        boolean dark = isDarkMode();
        if (alertActive) {
            btnAlertMessage.setBackgroundTintList(ColorStateList.valueOf(
                    Color.parseColor(dark ? "#451A1A" : "#FEE2E2")));
            btnAlertMessage.setTextColor(Color.parseColor(dark ? "#FCA5A5" : "#DC2626"));
            btnAlertMessage.setStrokeColor(ColorStateList.valueOf(
                    Color.parseColor(dark ? "#F87171" : "#EF4444")));
            etMessageInput.setHint("Type an ALERT message...");
        } else {
            btnAlertMessage.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnAlertMessage.setTextColor(Color.parseColor(dark ? "#F9FAFB" : "#111827"));
            btnAlertMessage.setStrokeColor(ColorStateList.valueOf(
                    Color.parseColor(dark ? "#4B5563" : "#D1D5DB")));
            etMessageInput.setHint("Type a message...");
        }
    }

    private void setupBottomBarListeners() {
        btnAlertMessage.setOnClickListener(v -> {
            isAlertMode = !isAlertMode;
            applyAlertModeUI(isAlertMode);
        });

        btnAttachFile.setOnClickListener(v -> attachFileLauncher.launch(new String[]{"*/*"}));

        btnSend.setOnClickListener(v -> {
            String message = etMessageInput.getText().toString().trim();

            if (pendingFileData == null && message.isEmpty()) {
                return;
            }

            final ChatMsgType msgType;
            final byte[] dataToSend;

            if (pendingFileData != null) {
                msgType = ChatMsgType.FILE;
                dataToSend = pendingFileData;
            } else if (isAlertMode) {
                msgType = ChatMsgType.ALERT;
                dataToSend = null;
            } else {
                msgType = ChatMsgType.SIMPLE_CHAT;
                dataToSend = null;
            }

            new Thread(() -> {
                addMessage((msgType==ChatMsgType.SIMPLE_CHAT) ? DisplayMessageType.SIMPLE_CHAT:((msgType==ChatMsgType.FILE) ? DisplayMessageType.FILE: DisplayMessageType.ALERT_CHAT), username, message, dataToSend);
                if (chatManager != null && chatManager.connection != null) {
                    chatManager.connection.sendData(new ChatMsg(msgType, username, message, dataToSend));
                }
            }).start();

            etMessageInput.setText("");
            pendingFileData = null;
            tvAttachedFileStatus.setText("");
            if (isAlertMode) {
                isAlertMode = false;
                applyAlertModeUI(false);
            }
        });
    }

    public void showInfomation(String information) {
        runOnUiThread(() -> Toast.makeText(ChatActivity.this, information, Toast.LENGTH_SHORT).show());
    }

    public void updateStatus(int userID, boolean isServer, int numberOfUser) {
        runOnUiThread(() -> {
            tvUidBadge.setText("UID:" + userID);
            tvRoleBadge.setText(isServer ? "Server" : "Client");
            tvOnlineBadge.setText("● " + numberOfUser + " Online");
        });
    }

    public void addMessage(DisplayMessageType type, String msgUsername, String message, byte[] fileData) {
        long epochSeconds = System.currentTimeMillis() / 1000;
        addMessage(type, msgUsername, message, fileData, epochSeconds);
    }

    public void addMessage(DisplayMessageType type, String msgUsername, String message, byte[] fileData, long epochSeconds) {
        runOnUiThread(() -> {
            boolean dark = isDarkMode();
            String formattedTime = formatEpochSeconds(epochSeconds);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 8, 0, 8);
            card.setLayoutParams(cardParams);
            card.setPadding(24, 16, 24, 16);

            if (type == DisplayMessageType.JOINED || type == DisplayMessageType.LEFT) {
                boolean isJoined = (type == DisplayMessageType.JOINED);
                String actionSuffix = isJoined ? " joined the chat!" : " left the chat!";
                String prefix = " -> ";
                String safeUser = msgUsername != null ? msgUsername : "User";
                String fullText = prefix + safeUser + actionSuffix;

                int baseColor = isJoined
                        ? Color.parseColor(dark ? "#66BB6A" : "#2E7D32")
                        : Color.parseColor(dark ? "#EF5350" : "#EF4444");
                int darkUserColor = isJoined
                        ? Color.parseColor(dark ? "#A5D6A7" : "#004D40")
                        : Color.parseColor(dark ? "#FFCDD2" : "#8E0000");

                SpannableString spannable = new SpannableString(fullText);
                spannable.setSpan(new ForegroundColorSpan(baseColor), 0, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new ForegroundColorSpan(darkUserColor), prefix.length(), prefix.length() + safeUser.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new StyleSpan(Typeface.BOLD), prefix.length(), prefix.length() + safeUser.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);

                TextView tvEvent = new TextView(this);
                tvEvent.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                tvEvent.setText(spannable);
                tvEvent.setTextSize(14f);

                TextView tvTime = createTimeTextView(formattedTime, dark);
                row.addView(tvEvent);
                row.addView(tvTime);

                card.addView(row);
            } else {
                boolean isAlert = (type == DisplayMessageType.ALERT_CHAT);
                int alertTextColor = Color.parseColor(dark ? "#FCA5A5" : "#DC2626");
                int primaryTextColor = Color.parseColor(dark ? "#F9FAFB" : "#111827");

                LinearLayout headerRow = new LinearLayout(this);
                headerRow.setOrientation(LinearLayout.HORIZONTAL);
                headerRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView tvUser = new TextView(this);
                tvUser.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                tvUser.setText(msgUsername != null ? msgUsername : "");
                tvUser.setTypeface(null, Typeface.BOLD);
                tvUser.setTextSize(14f);
                tvUser.setTextColor(isAlert ? alertTextColor : primaryTextColor);

                TextView tvTime = createTimeTextView(formattedTime, dark);
                if (isAlert) {
                    tvTime.setTextColor(alertTextColor);
                }

                headerRow.addView(tvUser);
                headerRow.addView(tvTime);
                card.addView(headerRow);

                if (message != null && !message.isEmpty()) {
                    TextView tvMsg = new TextView(this);
                    tvMsg.setText(message);
                    tvMsg.setTextSize(15f);
                    tvMsg.setPadding(0, 6, 0, 0);
                    tvMsg.setTextColor(isAlert ? alertTextColor : primaryTextColor);
                    card.addView(tvMsg);
                }

                if (isAlert) {
                    GradientDrawable alertBox = new GradientDrawable();
                    alertBox.setColor(Color.parseColor(dark ? "#3B1818" : "#FFF5F5"));
                    alertBox.setStroke(4, Color.parseColor(dark ? "#F87171" : "#EF4444"));
                    alertBox.setCornerRadius(16f);
                    card.setBackground(alertBox);
                } else {
                    GradientDrawable normalBox = new GradientDrawable();
                    normalBox.setColor(Color.parseColor(dark ? "#1F2937" : "#F3F4F6"));
                    normalBox.setStroke(2, Color.parseColor(dark ? "#374151" : "#E5E7EB"));
                    normalBox.setCornerRadius(16f);
                    card.setBackground(normalBox);
                }

                if (type == DisplayMessageType.FILE && fileData != null && fileData.length > 1) {
                    int nameLen = fileData[0] & 0xFF;
                    if (fileData.length >= 1 + nameLen) {
                        String extractedFileName = new String(fileData, 1, nameLen, StandardCharsets.UTF_8);
                        byte[] extractedFileContent = Arrays.copyOfRange(fileData, 1 + nameLen, fileData.length);

                        Button btnDownload = new Button(this);
                        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        btnParams.topMargin = 12;
                        btnDownload.setLayoutParams(btnParams);
                        btnDownload.setAllCaps(false);
                        btnDownload.setText("Download " + extractedFileName);
                        btnDownload.setBackgroundTintList(ColorStateList.valueOf(
                                Color.parseColor(dark ? "#3B82F6" : "#0060B6")));
                        btnDownload.setTextColor(Color.WHITE);

                        btnDownload.setOnClickListener(v -> {
                            pendingDownloadBytes = extractedFileContent;
                            saveFileLauncher.launch(extractedFileName);
                        });

                        card.addView(btnDownload);
                    }
                }
            }

            llMessagesContainer.addView(card);
            svMessages.post(() -> svMessages.fullScroll(View.FOCUS_DOWN));
        });
    }

    private TextView createTimeTextView(String formattedTime, boolean dark) {
        TextView tvTime = new TextView(this);
        tvTime.setText(formattedTime);
        tvTime.setTextSize(11f);
        tvTime.setTextColor(Color.parseColor(dark ? "#9CA3AF" : "#6B7280"));
        return tvTime;
    }

    private String formatEpochSeconds(long epochSeconds) {
        Date date = new Date(epochSeconds * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    private GradientDrawable createPillDrawable(int fillColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(20f);
        drawable.setColor(fillColor);
        drawable.setStroke(2, strokeColor);
        return drawable;
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }
}