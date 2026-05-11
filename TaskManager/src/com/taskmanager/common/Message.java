package com.taskmanager.common;

public class Message {
    private String type;   // 消息类型，对应 MessageType 里的常量
    private String data;   // 消息内容，JSON 字符串

    public Message() {}

    public Message(String type, String data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    // 把 Message 对象序列化成一行字符串发送
    // 格式：TYPE|||DATA
    // 用 ||| 分隔，避免 JSON 内容里的特殊字符干扰
    public String serialize() {
        return type + "|||" + (data == null ? "" : data);
    }

    // 从一行字符串反序列化成 Message 对象
    public static Message deserialize(String raw) {
        int idx = raw.indexOf("|||");
        if (idx < 0) return new Message("ERROR", "invalid message");
        String type = raw.substring(0, idx);
        String data = raw.substring(idx + 3);
        return new Message(type, data);
    }

    @Override
    public String toString() {
        return "Message{type='" + type + "', data='" + data + "'}";
    }
}