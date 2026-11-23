package com.example.chat.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ClientSetupActivity extends AppCompatActivity {

    private EditText etClientName;
    private EditText etServerIp;
    private EditText etServerPort;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_setup);

        etClientName = findViewById(R.id.etClientName);
        etServerIp = findViewById(R.id.etServerIp);
        etServerPort = findViewById(R.id.etServerPort);
        btnConnect = findViewById(R.id.btnConnect);

        btnConnect.setOnClickListener(v -> {
            String clientName = etClientName.getText().toString().trim();
            String ip = etServerIp.getText().toString().trim();
            String portStr = etServerPort.getText().toString().trim();

            if (clientName.isEmpty()) {
                Toast.makeText(this, "請輸入使用者名稱", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ip.isEmpty()) {
                Toast.makeText(this, "請輸入 Server IP", Toast.LENGTH_SHORT).show();
                return;
            }

            if (portStr.isEmpty()) {
                Toast.makeText(this, "請輸入 Port", Toast.LENGTH_SHORT).show();
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Port 必須為有效數字", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ClientSetupActivity.this, ClientChatActivity.class);
            intent.putExtra("CLIENT_NAME", clientName);
            intent.putExtra("SERVER_IP", ip);
            intent.putExtra("SERVER_PORT", port);
            startActivity(intent);
        });
    }
}
