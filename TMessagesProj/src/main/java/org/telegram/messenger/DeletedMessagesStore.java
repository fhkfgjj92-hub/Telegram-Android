package org.telegram.messenger;

import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Хранилище удалённых сообщений. Простой JSON-lines файл в filesDir —
 * без изменений схемы БД Telegram (поэтому легко переживает обновления).
 */
public class DeletedMessagesStore {

    public static class Entry {
        public long chatId;
        public String chat;
        public String text;
        public long ts;
    }

    private static File file() {
        return new File(ApplicationLoader.applicationContext.getFilesDir(), "deleted_messages.jsonl");
    }

    public static synchronized void add(long chatId, String chat, String text) {
        if (!Customization.saveDeleted()) return;
        try {
            JSONObject o = new JSONObject();
            o.put("chatId", chatId);
            o.put("chat", chat);
            o.put("text", text == null ? "" : text);
            o.put("ts", System.currentTimeMillis());
            File f = file();
            BufferedWriter w = new BufferedWriter(new FileWriter(f, true));
            w.write(o.toString());
            w.newLine();
            w.close();
            if (f.length() > 4 * 1024 * 1024) trim(f);
        } catch (Throwable ignore) {}
    }

    private static ArrayList<String> readLines(File f) {
        ArrayList<String> out = new ArrayList<>();
        try {
            if (!f.exists()) return out;
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().length() > 0) out.add(line);
            }
            r.close();
        } catch (Throwable ignore) {}
        return out;
    }

    private static void trim(File f) {
        try {
            ArrayList<String> lines = readLines(f);
            BufferedWriter w = new BufferedWriter(new FileWriter(f, false));
            int start = Math.max(0, lines.size() - 1500);
            for (int i = start; i < lines.size(); i++) {
                w.write(lines.get(i));
                w.newLine();
            }
            w.close();
        } catch (Throwable ignore) {}
    }

    public static synchronized ArrayList<Entry> all() {
        ArrayList<Entry> out = new ArrayList<>();
        ArrayList<String> lines = readLines(file());
        Collections.reverse(lines);
        for (String line : lines) {
            try {
                JSONObject o = new JSONObject(line);
                Entry e = new Entry();
                e.chatId = o.optLong("chatId");
                e.chat = o.optString("chat");
                e.text = o.optString("text");
                e.ts = o.optLong("ts");
                out.add(e);
            } catch (Throwable ignore) {}
        }
        return out;
    }

    public static synchronized int count() {
        return readLines(file()).size();
    }

    public static synchronized void clear() {
        try {
            File f = file();
            if (f.exists()) f.delete();
        } catch (Throwable ignore) {}
    }

    /**
     * Хук из MessagesController.deleteMessages(...).
     */
    public static void onDelete(int currentAccount, long dialogId, ArrayList<Integer> messageIds) {
        if (!Customization.saveDeleted() || messageIds == null || messageIds.isEmpty()) return;
        try {
            MessagesController mc = MessagesController.getInstance(currentAccount);
            String title = "Чат";
            try {
                if (dialogId > 0) {
                    TLRPC.User user = mc.getUser(dialogId);
                    if (user != null) title = ContactsController.formatName(user.first_name, user.last_name);
                } else if (dialogId < 0) {
                    TLRPC.Chat chat = mc.getChat(-dialogId);
                    if (chat != null && chat.title != null) title = chat.title;
                }
            } catch (Throwable ignore) {}

            MessagesStorage ms = MessagesStorage.getInstance(currentAccount);
            java.lang.reflect.Method getMessage = null;
            try {
                getMessage = MessagesStorage.class.getMethod("getMessage", long.class, int.class);
            } catch (Throwable ignore) {}
            for (Integer mid : messageIds) {
                if (mid == null) continue;
                String text = null;
                try {
                    if (getMessage != null) {
                        Object msgObj = getMessage.invoke(ms, dialogId, mid.intValue());
                        if (msgObj instanceof MessageObject) {
                            MessageObject msg = (MessageObject) msgObj;
                            text = msg.messageText != null ? msg.messageText.toString() : "(вложение)";
                        }
                    }
                } catch (Throwable ignore) {}
                if (text == null) text = "(содержимое недоступно, id " + mid + ")";
                add(dialogId, title, text);
            }
        } catch (Throwable ignore) {}
    }
}
