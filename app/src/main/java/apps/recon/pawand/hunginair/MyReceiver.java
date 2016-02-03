package apps.recon.pawand.hunginair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by pawanD on 2/3/2016.
 */
public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.w("PAwaqn", "hhhhh");
        Toast.makeText(context, "Action: " + intent.getAction(), Toast.LENGTH_SHORT).show();

        Intent intent1 = new Intent(context,LocationUpdateService.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intent1);
    }

}
