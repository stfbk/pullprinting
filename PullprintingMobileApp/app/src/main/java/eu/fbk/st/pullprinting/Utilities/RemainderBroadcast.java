package eu.fbk.st.pullprinting.Utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import eu.fbk.st.pullprinting.R;


public class RemainderBroadcast extends BroadcastReceiver {

    private static final String CHANNEL_ID = "simplied_coding";

    @Override
    public void onReceive(Context context, Intent intent) {
        displayPushNotification(context,"Check your printer","Your documents are ready now");
    }


    public void displayPushNotification(Context context, String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(0, builder.build());

    }
}

