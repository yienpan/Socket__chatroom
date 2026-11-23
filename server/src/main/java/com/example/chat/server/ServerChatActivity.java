package com.example.chat.server;

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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerChatActivity extends AppCompatActivity {

    private String serverName;
    private int serverPort;

    private TextView tvServerTitle;
    private TextView tvServerInfo;
    private TextView tvChatLog;
    private ScrollView scrollView;
    private EditText etMessage;
    private Button btnSend;
    private Button btnStopServer;

    private ServerSocket serverSocket;
    private Thread serverAcceptThread;
    private boolean isServerRunning = false;

    // Client connection handlers list (thread-safe)
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_chat);

        serverName = getIntent().getStringExtra("SERVER_NAME");
        serverPort = getIntent().getIntExtra("SERVER_PORT", 6100);

        tvServerTitle = findViewById(R.id.tvServerTitle);
        tvServerInfo = findViewById(R.id.tvServerInfo);
        tvChatLog = findViewById(R.id.tvChatLog);
        scrollView = findViewById(R.id.scrollView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnStopServer = findViewById(R.id.btnStopServer);

        tvServerTitle.setText("Server: " + serverName);

        btnSend.setOnClickListener(v -> sendServerBroadcast());
        btnStopServer.setOnClickListener(v -> finish());

        startServerSocket();
    }

    private void startServerSocket() {
        isServerRunning = true;
        serverAcceptThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(serverPort);
                String ipAddress = getLocalIpAddress();

                runOnUiThread(() -> {
                    updateServerInfo(ipAddress);
                    appendLog("[系統提示] Server 已成功啟動，監聽 Port: " + serverPort);
                });

                while (isServerRunning && !serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept(); // Blocking accept call
                    String remoteAddress = clientSocket.getRemoteSocketAddress().toString();
                    android.util.Log.d("ServerChatActivity", "Client accepted: " + remoteAddress);
                    runOnUiThread(() -> appendLog("[系統提示] 收到連線請求來自: " + remoteAddress));

                    ClientHandler handler = new ClientHandler(clientSocket);
                    activeClients.add(handler);
                    new Thread(handler).start();
                }
            } catch (Exception e) {
                if (isServerRunning) {
                    android.util.Log.e("ServerChatActivity", "ServerSocket exception", e);
                    String errorDetails = e.getClass().getSimpleName() + ": " + e.getMessage();
                    runOnUiThread(() -> appendLog("[系統錯誤] ServerSocket 異常: " + errorDetails));
                }
            }
        });
        serverAcceptThread.start();
    }

    private void sendServerBroadcast() {
        String msgText = etMessage.getText().toString().trim();
        if (msgText.isEmpty()) return;

        etMessage.setText("");
        String jsonMsg = Protocol.createJsonMessage(Protocol.TYPE_CHAT, "【Server廣播】" + serverName, msgText);
        appendLog("【Server廣播】: " + msgText);
        broadcastJson(jsonMsg);
    }

    private void broadcastJson(String jsonStr) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(jsonStr);
        }
    }

    private void updateServerInfo(String ipAddress) {
        tvServerInfo.setText(String.format("IP: %s | Port: %d | 在線 Client: %d 人", ipAddress, serverPort, activeClients.size()));
    }

    private void appendLog(String message) {
        runOnUiThread(() -> {
            tvChatLog.append(message + "\n");
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            String ip = getLocalIpAddress();
            updateServerInfo(ip);
        });
    }

    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "10.0.2.15 (模擬器) / 127.0.0.1";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }

    private void stopServer() {
        isServerRunning = false;
        // Broadcast server close to all clients
        String closeJson = Protocol.createJsonMessage(Protocol.TYPE_SERVER_CLOSE, "Server", "Server 已關閉連線");
        broadcastJson(closeJson);

        for (ClientHandler client : activeClients) {
            client.closeConnection();
        }
        activeClients.clear();

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
    }

    // Individual Client Handler Thread
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String clientName = "未知使用者";

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        JSONObject obj = new JSONObject(line);
                        String type = obj.optString("type");
                        String sender = obj.optString("sender");
                        String message = obj.optString("message");

                        if (Protocol.TYPE_JOIN.equals(type)) {
                            this.clientName = sender;
                            appendLog(">>> [" + clientName + "] 加入了聊天室 (IP: " + socket.getInetAddress().getHostAddress() + ")");
                            broadcastJson(line);
                        } else if (Protocol.TYPE_CHAT.equals(type)) {
                            appendLog(sender + ": " + message);
                            broadcastJson(line);
                        } else if (Protocol.TYPE_LEAVE.equals(type)) {
                            appendLog("<<< [" + sender + "] 離開了聊天室");
                            broadcastJson(line);
                            break;
                        }
                    } catch (JSONException e) {
                        // Raw text message fallback
                        appendLog(this.clientName + ": " + line);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ServerChatActivity", "ClientHandler exception for " + clientName, e);
            } finally {
                closeConnection();
                activeClients.remove(this);
                appendLog("<<< [" + clientName + "] 已離線");
                String leaveJson = Protocol.createJsonMessage(Protocol.TYPE_LEAVE, clientName, clientName + " 離開了聊天室");
                broadcastJson(leaveJson);
            }
        }

        public void sendMessage(String jsonMsg) {
            new Thread(() -> {
                synchronized (this) {
                    if (writer != null && !writer.checkError()) {
                        writer.println(jsonMsg);
                        writer.flush();
                    }
                }
            }).start();
        }

        public void closeConnection() {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}
