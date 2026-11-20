package org.telegram.messenger;

import android.content.SharedPreferences;

/**
 * Настройки мода. Хранятся в отдельном SharedPreferences, не пересекаются
 * с настройками Telegram. Вызывается из Theme.getColor(), MessagesController
 * и экрана настроек мода.
 */
public class Customization {

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("tgmod_settings", 0);
    }

    private static volatile int accentCache = -2;

    public static boolean ghostMode()    { return prefs().getBoolean("ghost_mode", false); }
    public static boolean ghostTyping()  { return prefs().getBoolean("ghost_typing", true); }
    public static boolean freezeOnline() { return prefs().getBoolean("freeze_online", false); }
    public static boolean saveDeleted()  { return prefs().getBoolean("save_deleted", true); }

    public static int accent() {
        int a = accentCache;
        if (a == -2) {
            a = prefs().getInt("accent", 0);
            accentCache = a;
        }
        return a;
    }

    public static void set(String key, boolean value) { prefs().edit().putBoolean(key, value).apply(); }
    public static void setAccent(int value) { accentCache = value; prefs().edit().putInt("accent", value).apply(); }
    public static void reset() { accentCache = -2; prefs().edit().clear().apply(); }

    /** 0 — стандартная тема Telegram (без переопределений). */
    private static final int[] ACCENTS = {
            0,           // 0 стандартный
            0xffe53935,  // 1 красный
            0xff43a047,  // 2 зелёный
            0xff8e24aa,  // 3 фиолетовый
            0xfffb8c00,  // 4 оранжевый
            0xffd81b60,  // 5 розовый
            0xff00acc1,  // 6 бирюзовый
            0xff3949ab,  // 7 индиго
            0xff546e7a   // 8 серый
    };

    public static int[] accents() { return ACCENTS; }

    public static int accentColor() {
        int i = accent();
        if (i <= 0 || i >= ACCENTS.length) return 0;
        return ACCENTS[i];
    }

    /**
     * Вызывается из Theme.getColor(String key) — первой строкой.
     * Возвращает цвет-переопределение или null (тогда цвет берётся из темы).
     * Ключи — строковые константы Theme (например key_dialogTextBlue = "dialogTextBlue").
     */
    public static Integer colorOverride(String key) {
        int color = accentColor();
        if (color == 0 || key == null) return null;
        switch (key) {
            case "dialogTextBlue":
            case "dialogTextBlue2":
            case "dialogTextBlue3":
            case "dialogTextBlue4":
            case "dialogTextBlueSelector":
            case "dialogTextLink":
            case "windowBackgroundWhiteBlueText":
            case "windowBackgroundWhiteBlueText2":
            case "windowBackgroundWhiteBlueText3":
            case "windowBackgroundWhiteBlueIcon":
            case "windowBackgroundWhiteLinkText":
            case "chats_actionBackground":
            case "chats_actionIcon":
            case "profile_status":
            case "profile_verifiedBackground":
            case "chat_messagePanelSend":
            case "chat_inPreviewInstantText":
            case "chat_outPreviewInstantText":
            case "switchTrackBlueChecked":
            case "checkboxSquareBackground":
            case "featuredStickers_addButton":
            case "featuredStickers_buttonText":
            case "inappPlayerPerformer":
            case "returnToCallBackground":
            case "groupcreate_spanText":
            case "contacts_inviteBackground":
            case "calls_callReceivedGreenIcon":
            case "chats_menuPhoneCats":
            case "login_progressInner":
                return color;
            default:
                return null;
        }
    }
}
