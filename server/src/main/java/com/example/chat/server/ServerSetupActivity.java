package com.example.chat.server;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ServerSetupActivity extends AppCompatActivity {

    private EditText etServerName;
    private EditText etServerPort;
    private Button btnStartServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_setup);

        etServerName = findViewById(R.id.etServerName);
        etServerPort = findViewById(R.id.etServerPort);
        btnStartServer = findViewById(R.id.btnStartServer);

        btnStartServer.setOnClickListener(v -> {
            String name = etServerName.getText().toString().trim();
            String portStr = etServerPort.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "請輸入 Server 名稱", Toast.LENGTH_SHORT).show();
                return;
            }

            if (portStr.isEmpty()) {
                Toast.makeText(this, "請輸入 Port 號碼", Toast.LENGTH_SHORT).show();
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Port 必須為有效數字", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ServerSetupActivity.this, ServerChatActivity.class);
            intent.putExtra("SERVER_NAME", name);
            intent.putExtra("SERVER_PORT", port);
            startActivity(intent);
        });
    }
}
