package com.thefunone.chatwm;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String DATA_FILE_NAME = "data";
    private static final String DEFAULT_KEYSTORE = "defaultKeyStore.p12";
    private static final String DEFAULT_PASSWORD = "password123";

    public static ChatManager chatManager;

    private EditText etUsername, etIpAddress, etPortNumber, etKeyStorePath;
    private TextInputEditText etKeyStorePassword;
    private Button btnChooseKeyStore, btnDefaultKeyStore, btnConnect;
    private ProgressBar pbLoading;

    private String currentUsername, currentIpAddress, currentKeystorePath, currentKeystorePassword;
    private int currentPortNum;

    private ActivityResultLauncher<String[]> chooseKeyStoreLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etIpAddress = findViewById(R.id.etIpAddress);
        etPortNumber = findViewById(R.id.etPortNumber);
        etKeyStorePath = findViewById(R.id.etKeyStorePath);
        etKeyStorePassword = findViewById(R.id.etKeyStorePassword);
        btnChooseKeyStore = findViewById(R.id.btnChooseKeyStore);
        btnDefaultKeyStore = findViewById(R.id.btnDefaultKeyStore);
        btnConnect = findViewById(R.id.btnConnect);
        pbLoading = findViewById(R.id.pbLoading);

        etPortNumber.setFilters(new InputFilter[]{new PortNumberFilter(0, 65535)});

        etKeyStorePath.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String path = s.toString().trim();
                if (path.equals(DEFAULT_KEYSTORE) || path.endsWith("/" + DEFAULT_KEYSTORE)) {
                    etKeyStorePassword.setText(DEFAULT_PASSWORD);
                }
            }
        });

        setupFileChooserLauncher();
        loadData();

        btnChooseKeyStore.setOnClickListener(v -> chooseKeyStoreLauncher.launch(new String[]{
                "application/x-pkcs12",
                "application/octet-stream"
        }));

        // "Select Default KeyStore File" button click
        btnDefaultKeyStore.setOnClickListener(v -> setDefaultKeyStore());

        btnConnect.setOnClickListener(v -> handleConnect());
    }

    private void handleConnect() {
        currentUsername = etUsername.getText().toString().trim();
        currentIpAddress = etIpAddress.getText().toString().trim();
        String portStr = etPortNumber.getText().toString().trim();
        currentKeystorePath = etKeyStorePath.getText().toString().trim();
        currentKeystorePassword = etKeyStorePassword.getText() != null ?
                etKeyStorePassword.getText().toString() : "";

        if (currentUsername.isEmpty()) {
            etUsername.setError("Your Name is required");
            etUsername.requestFocus();
            return;
        }

        if (currentIpAddress.isEmpty()) {
            currentIpAddress = "localhost";
        }

        if (portStr.isEmpty()) {
            etPortNumber.setError("Port Number is required");
            etPortNumber.requestFocus();
            return;
        }

        try {
            currentPortNum = Integer.parseInt(portStr);
            if (currentPortNum < 0 || currentPortNum > 65535) {
                etPortNumber.setError("Port must be between 0 and 65535");
                etPortNumber.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etPortNumber.setError("Invalid port number");
            etPortNumber.requestFocus();
            return;
        }

        if (currentKeystorePath.isEmpty()) {
            etKeyStorePath.setError("Please enter or choose a KeyStore file");
            etKeyStorePath.requestFocus();
            return;
        }

        if (currentKeystorePassword.isEmpty()) {
            etKeyStorePassword.setError("KeyStore Password is required");
            etKeyStorePassword.requestFocus();
            return;
        }

        saveData(currentUsername, currentIpAddress, currentPortNum, currentKeystorePath);

        setLoadingState(true);

        chatManager = new ChatManager(
                this,
                currentUsername,
                currentIpAddress,
                currentPortNum,
                currentKeystorePath,
                currentKeystorePassword
        );
    }

    public void connectionEstablished() {
        runOnUiThread(() -> {
            setLoadingState(false);
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("username", currentUsername);
            intent.putExtra("ipAddress", currentIpAddress);
            intent.putExtra("portNum", currentPortNum);
            intent.putExtra("keystorePath", currentKeystorePath);
            intent.putExtra("keystorePassword", currentKeystorePassword);
            startActivity(intent);
        });
    }

    public void failedToConnect(String information) {
        runOnUiThread(() -> {
            setLoadingState(false);
            if (information != null && !information.isEmpty()) {
                Toast.makeText(MainActivity.this, information, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnConnect.setEnabled(!isLoading);
        btnChooseKeyStore.setEnabled(!isLoading);
        btnDefaultKeyStore.setEnabled(!isLoading);
        etUsername.setEnabled(!isLoading);
        etIpAddress.setEnabled(!isLoading);
        etPortNumber.setEnabled(!isLoading);
        etKeyStorePath.setEnabled(!isLoading);
        etKeyStorePassword.setEnabled(!isLoading);
    }

    private void setupFileChooserLauncher() {
        chooseKeyStoreLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        String fileName = getFileNameFromUri(uri);
                        if (fileName != null && (fileName.endsWith(".p12") || fileName.endsWith(".pfx"))) {
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException ignored) {}

                            if (fileName.equals(DEFAULT_KEYSTORE)) {
                                etKeyStorePath.setText(DEFAULT_KEYSTORE);
                            } else {
                                etKeyStorePath.setText(uri.toString());
                            }
                        } else {
                            Toast.makeText(this, "Please select a valid .p12 or .pfx file", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void saveData(String username, String ipAddress, int portNum, String keystorePath) {
        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("ipAddress", ipAddress);
            json.put("portNum", portNum);
            json.put("keystorePath", keystorePath);

            try (FileOutputStream fos = openFileOutput(DATA_FILE_NAME, Context.MODE_PRIVATE)) {
                fos.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        File dataFile = new File(getFilesDir(), DATA_FILE_NAME);
        if (dataFile.exists()) {
            try (FileInputStream fis = openFileInput(DATA_FILE_NAME);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                JSONObject json = new JSONObject(sb.toString());
                etUsername.setText(json.optString("username", ""));
                etIpAddress.setText(json.optString("ipAddress", ""));
                if (json.has("portNum")) {
                    etPortNumber.setText(String.valueOf(json.optInt("portNum")));
                }

                String savedKeyStore = json.optString("keystorePath", DEFAULT_KEYSTORE);
                etKeyStorePath.setText(savedKeyStore);
                if (savedKeyStore.equals(DEFAULT_KEYSTORE) || savedKeyStore.endsWith("/" + DEFAULT_KEYSTORE)) {
                    etKeyStorePassword.setText(DEFAULT_PASSWORD);
                }
            } catch (Exception e) {
                setDefaultKeyStore();
            }
        } else {
            setDefaultKeyStore();
        }
    }

    private void setDefaultKeyStore() {
        etKeyStorePath.setText(DEFAULT_KEYSTORE);
        etKeyStorePassword.setText(DEFAULT_PASSWORD);
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

    private static class PortNumberFilter implements InputFilter {
        private final int min, max;

        public PortNumberFilter(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            try {
                String newVal = dest.subSequence(0, dstart).toString()
                        + source.subSequence(start, end).toString()
                        + dest.subSequence(dend, dest.length()).toString();
                if (newVal.length() > 1 && newVal.startsWith("0")) {
                    return "";
                }
                int input = Integer.parseInt(newVal);
                if (input >= min && input <= max) {
                    return null;
                }
            } catch (NumberFormatException ignored) {}
            return "";
        }
    }
}