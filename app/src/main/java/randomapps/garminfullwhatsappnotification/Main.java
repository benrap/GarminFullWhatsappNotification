package randomapps.garminfullwhatsappnotification;

import android.app.Notification;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Main implements IXposedHookLoadPackage {

    boolean LOG = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.garmin.android.apps.connectmobile")) return;

        if (LOG) XposedBridge.log("in garmin connect");

        findAndHookMethod("com.garmin.android.gncs.GNCSSmartNotificationsModule", lpparam.classLoader, "handlePostedNotification", StatusBarNotification.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                if (sbn.getPackageName().equals("com.whatsapp")) {
                    Notification notif = sbn.getNotification();
                    String content = extractContentFromWhatsappNotification(notif);
                    if (LOG) XposedBridge.log("got content:\n" + content);
                    Notification replacementNotification = notif.clone();
                    replacementNotification.extras.putString("android.text", content);

                    StatusBarNotification replacementSbn = new StatusBarNotification(sbn.getPackageName(), sbn.getPackageName(), sbn.getId(), sbn.getTag(), sbn.getUserId(), 0, 0, replacementNotification, sbn.getUser(), sbn.getPostTime());

                    param.args[0] = replacementSbn;
                }
            }
        });
    }

    private String extractContentFromWhatsappNotification(Notification notif) {
        Bundle extras = notif.extras;
        Parcelable b[] = (Parcelable[]) extras.get(Notification.EXTRA_MESSAGES);
        String content = "";
        if (b != null) {
            boolean group = true;
            if (((Bundle) b[0]).getString("sender").equals("\u200b")) {
                group = false;
            }
            for (Parcelable tmp : b) {
                Bundle msgBundle = (Bundle) tmp;
                if (group) {
                    content += msgBundle.getString("sender") + ": " + msgBundle.getString("text") + "\n";
                } else {
                    content += msgBundle.getString("text") + "\n";
                }
            }
        } else {
            content += extras.getString("android.text");
        }
        return content;
    }
}