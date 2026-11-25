# Socket__chatroom
本專案為物聯網專題中針對 Socket 網路通訊主題所進行的實作練習，於 Android Studio 環境建置。實作 Client-Server 通訊架構、多執行緒訊息收發，以及在行動端或裝置間建立即時聊天室的基礎機制。

## 架構與簡介

### 1. Client-Server 網路通訊設計
* 專案中包含 `server`、`client` 與 `app` 相關模組，用於練習主機端監聽連線與客戶端主動連線的互動流程。
* 主機端透過 `ServerSocket` 綁定指定 Port 等待連線請求；客戶端則透過目標 IP 位址與 Port 建立 `Socket` 連線。
* 此架構可作為後續物聯網專題中，讓行動裝置（Android App）與後端節點或感測控制端進行即時雙向文字/指令傳輸的基礎原型。

### 2. 多執行緒收發與介面更新
* **背景監聽與傳送**：為避免網路 I/O 阻塞主執行緒（UI Thread），連線建立與輸入/輸出串流（`InputStream`、`OutputStream` / `BufferedReader`、`PrintWriter`）的讀寫操作皆安排於獨立執行緒中處理。
* **即時訊息推播與顯示**：當伺服器收到任一客戶端傳來的訊息時，透過串流轉發或廣播至已連線的對象；接收端讀取資料後，再透過執行緒切換機制更新聊天室列表或畫面顯示。

## 專案目錄結構

```text
Socket__chatroom/
├── app/                # Android 應用程式模組（介面與行動端互動邏輯）
├── client/             # 客戶端連線與通訊相關實作模組
├── server/             # 伺服器端監聽與訊息處理相關實作模組
├── gradle/             # Gradle Wrapper 設定檔
├── build.gradle.kts    # 專案建置設定
└── settings.gradle.kts # 多模組設定檔
