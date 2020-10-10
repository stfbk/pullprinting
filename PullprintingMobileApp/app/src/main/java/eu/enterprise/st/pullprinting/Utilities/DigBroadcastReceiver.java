package eu.enterprise.st.pullprinting.Utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;


public class DigBroadcastReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "simplied_coding";

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            Log.d("Broadcast URL",uri.toString());
            //Toast.makeText(context, uri.toString(), Toast.LENGTH_SHORT).getView();
        }
    }
}

