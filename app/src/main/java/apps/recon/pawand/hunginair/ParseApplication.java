package apps.recon.pawand.hunginair;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseInstallation;

/**
 * Created by pawanD on 1/26/2016.
 */
public class ParseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this);
        Parse.initialize(this);
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }
}
