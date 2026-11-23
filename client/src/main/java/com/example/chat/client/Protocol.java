package com.example.chat.client;

import org.json.JSONException;
import org.json.JSONObject;

public class Protocol {
    public static final String TYPE_JOIN = "JOIN";
    public static final String TYPE_CHAT = "CHAT";
    public static final String TYPE_LEAVE = "LEAVE";
    public static final String TYPE_SERVER_CLOSE = "SERVER_CLOSE";
    public static final String TYPE_SERVER_INFO = "SERVER_INFO";

    public static String createJsonMessage(String type, String sender, String message) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", type);
            obj.put("sender", sender);
            obj.put("message", message);
            obj.put("timestamp", System.currentTimeMillis());
            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }
}
