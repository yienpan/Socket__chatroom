package com.example.chat.client;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientChatActivity extends AppCompatActivity {

    private String clientName;
    private String serverIp;
    private int serverPort;

    private TextView tvClientTitle;
    private TextView tvClientInfo;
    private TextView tvChatLog;
    private ScrollView scrollView;
    private EditText etMessage;
    private Button btnSend;
    private Button btnLeave;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread receiveThread;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_chat);

        clientName = getIntent().getStringExtra("CLIENT_NAME");
        serverIp = getIntent().getStringExtra("SERVER_IP");
        serverPort = getIntent().getIntExtra("SERVER_PORT", 6100);

        tvClientTitle = findViewById(R.id.tvClientTitle);
        tvClientInfo = findViewById(R.id.tvClientInfo);
        tvChatLog = findViewById(R.id.tvChatLog);
        scrollView = findViewById(R.id.scrollView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnLeave = findViewById(R.id.btnLeave);

        tvClientTitle.setText("使用者: " + clientName);
        tvClientInfo.setText(String.format("正在連線至 %s:%d...", serverIp, serverPort));

        btnSend.setOnClickListener(v -> sendMessage());
        btnLeave.setOnClickListener(v -> leaveChatRoom());

        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket();
                // 5 秒逾時設定，若 IP 錯誤可迅速返回失敗提示
                socket.connect(new java.net.InetSocketAddress(serverIp, serverPort), 5000);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                isConnected = true;

                runOnUiThread(() -> {
                    tvClientInfo.setText(String.format("已連線至 %s:%d", serverIp, serverPort));
                    appendLog("[系統提示] 成功連線至 Server!");
                });

                // Send JOIN message to server
                String joinJson = Protocol.createJsonMessage(Protocol.TYPE_JOIN, clientName, clientName + " 加入了聊天室");
                sendRawMessage(joinJson);

                // Start receiving background thread
                startReceiving();

            } catch (Exception e) {
                android.util.Log.e("ClientChatActivity", "Connection failed", e);
                String errorDetails = e.getClass().getSimpleName() + ": " + e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(ClientChatActivity.this, "連線失敗: " + errorDetails, Toast.LENGTH_LONG).show();
                    appendLog("[系統錯誤] 無法連線至 " + serverIp + ":" + serverPort + "\n詳細原因: " + errorDetails);
                    tvClientInfo.setText(String.format("連線失敗 (%s:%d)", serverIp, serverPort));
                });
            }
        }).start();
    }

    private void startReceiving() {
        receiveThread = new Thread(() -> {
            try {
                String line;
                while (isConnected && (line = reader.readLine()) != null) {
                    try {
                        JSONObject obj = new JSONObject(line);
                        String type = obj.optString("type");
                        String sender = obj.optString("sender");
                        String message = obj.optString("message");

                        if (Protocol.TYPE_JOIN.equals(type)) {
                            appendLog(">>> [" + sender + "] 加入了聊天室");
                        } else if (Protocol.TYPE_CHAT.equals(type)) {
                            appendLog(sender + ": " + message);
                        } else if (Protocol.TYPE_LEAVE.equals(type)) {
                            appendLog("<<< [" + sender + "] 離開了聊天室");
                        } else if (Protocol.TYPE_SERVER_CLOSE.equals(type)) {
                            appendLog("[系統提示] Server 已關閉聊天室！");
                            disconnectInternal();
                            runOnUiThread(() -> Toast.makeText(ClientChatActivity.this, "Server 已關閉連線", Toast.LENGTH_SHORT).show());
                            break;
                        }
                    } catch (JSONException e) {
                        appendLog(line);
                    }
                }
            } catch (Exception e) {
                if (isConnected) {
                    android.util.Log.e("ClientChatActivity", "Receiving thread exception", e);
                    String errorDetails = e.getClass().getSimpleName() + ": " + e.getMessage();
                    appendLog("[系統提示] 與 Server 的連線中斷: " + errorDetails);
                }
            } finally {
                disconnectInternal();
            }
        });
        receiveThread.start();
    }

    private void appendLog(String message) {
        runOnUiThread(() -> {
            tvChatLog.append(message + "\n");
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    private void sendMessage() {
        String msgText = etMessage.getText().toString().trim();
        if (msgText.isEmpty() || !isConnected) return;

        etMessage.setText("");
        String jsonMsg = Protocol.createJsonMessage(Protocol.TYPE_CHAT, clientName, msgText);
        sendRawMessage(jsonMsg);
    }

    private void sendRawMessage(String msg) {
        new Thread(() -> {
            synchronized (this) {
                if (writer != null && !writer.checkError()) {
                    writer.println(msg);
                    writer.flush();
                }
            }
        }).start();
    }

    private void leaveChatRoom() {
        new Thread(() -> {
            if (isConnected) {
                String leaveJson = Protocol.createJsonMessage(Protocol.TYPE_LEAVE, clientName, clientName + " 離開聊天室");
                if (writer != null && !writer.checkError()) {
                    writer.println(leaveJson);
                    writer.flush();
                }
            }
            disconnectInternal();
            runOnUiThread(this::finish);
        }).start();
    }

    private synchronized void disconnectInternal() {
        if (!isConnected) return;
        isConnected = false;

        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnected) {
            leaveChatRoom();
        }
    }
}
