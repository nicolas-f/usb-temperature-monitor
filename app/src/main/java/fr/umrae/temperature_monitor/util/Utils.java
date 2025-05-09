package fr.umrae.temperature_monitor.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.format.DateFormat;

import java.util.Date;
import java.util.Random;

public class Utils {

    public static String randomHex(int size){
        StringBuilder ret = new StringBuilder("");
        Random rnd = new Random();
        for(int i=0;i<size;i++){
            byte b = (byte)(rnd.nextInt(255)-125);
            String str = Integer.toHexString(b & 0xFF);
            if(str.length()<2){
                str = "0"+str;
            }
            ret.append(str);
        }
        return ret.toString().toUpperCase();
    }

    public static String setLastUpdateTime(Context context, long lastUpdate) {
        Date lastUpdateTime = new Date(lastUpdate);
        return DateFormat.getTimeFormat(context).format(lastUpdateTime);
    }

    public static String unixTimeToFormatTime(Context context, long unixTime) {
        long unixTimeToMillis = unixTime * 1000;
        return DateFormat.getTimeFormat(context).format(unixTimeToMillis);
    }

    public static void copyToClipboard(Context context, String string) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(
                Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(string, string);
        clipboardManager.setPrimaryClip(clipData);
    }
    public static void main(String s[]){
        for(int i=0;1<100;i++)
        System.out.println(randomHex(16).length());
    }
}
