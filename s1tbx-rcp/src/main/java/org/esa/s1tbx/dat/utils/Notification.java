package org.esa.s1tbx.dat.utils;

import com.alee.managers.notification.NotificationIcon;
import com.alee.managers.notification.NotificationManager;

/**
 * creates popup notifications to the user
 */
public class Notification {

    private static Notification _instance = null;

    public static Notification instance() {
        if(_instance == null) {
            _instance = new Notification();
        }
        return _instance;
    }

    private Notification() {

    }

    public void msg(final String text) {
        NotificationManager.showNotification(text);
    }

    public void msgIcon(final String text) {
        NotificationManager.showNotification ( text, NotificationIcon.tip.getIcon () );
    }
}
