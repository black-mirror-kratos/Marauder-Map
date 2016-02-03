package apps.recon.pawand.hunginair;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * Created by pawanD on 2/3/2016.
 */
public class LocationUpdateService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String cordX;
    private String cordY;
    private String userId;
    private String LOGTAG = "DIXIT";

    /** Called when the service is being created. */
    @Override
    public void onCreate() {
        Log.w(LOGTAG, "onCreate...");


        ParseQuery<ParseObject> query = new ParseQuery<>("UserInfoParse");
        query.fromLocalDatastore();
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException ex) {
                if (ex != null) {
                    final int statusCode = ex.getCode();
                    if (statusCode == ParseException.OBJECT_NOT_FOUND) {
                        // Object did not exist on the parse backend
                        Toast.makeText(getApplicationContext(), "not signed in the app", Toast.LENGTH_SHORT).show();
                        onDestroy();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "object exists", Toast.LENGTH_SHORT).show();
                    userId = object.get("userId").toString();
                    // No exception means the object exists
                }
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(LOGTAG, "onStart...");

        super.onStartCommand(intent, flags, startId);



        // Let it continue running until it is stopped.
        mGoogleApiClient.connect();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.w(LOGTAG, "onDestroy...");
        mGoogleApiClient.disconnect();
        super.onDestroy();
        //stopForeground(true);
        startService(new Intent(this, LocationUpdateService.class));
    }


    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100);
        mLocationRequest.setFastestInterval(50);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(0.1F);

        // create the Intent to use WebViewActivity to handle results
        //Intent mRequestLocationUpdatesIntent = new Intent(this, LocationUpdateService.class);

        // create a PendingIntent
        //PendingIntent mRequestLocationUpdatesPendingIntent = PendingIntent.getService(this.getApplicationContext(), 0,
         //             mRequestLocationUpdatesIntent,
         //           PendingIntent.FLAG_UPDATE_CURRENT);

        // request location updates

        //LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
         //               mLocationRequest,
         //               mRequestLocationUpdatesPendingIntent);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
               mLocationRequest,
               (LocationListener) this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this.getApplicationContext(), "onConnectionSuspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("locationtesting", "accuracy: " + location.getAccuracy() + " lat: " + location.getLatitude() + " lon: " + location.getLongitude());

        cordX = Double.toString(location.getLatitude());
        cordY = Double.toString(location.getLongitude());
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Location");
        query.whereEqualTo("user", userId);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException error) {
                if (error == null) {
                    object.put("user", userId);
                    object.put("cordX", cordX);
                    object.put("cordY", cordY);
                    object.saveInBackground();
                    Toast.makeText(getApplicationContext(), "locationUpdateService/onHandleIntent" + "(cords sent!)", Toast.LENGTH_SHORT).show();
                } else {
                    if (error.getCode() == error.OBJECT_NOT_FOUND)
                    {
                        Toast.makeText(getApplicationContext(), "locationUpdateService/onHandleIntent  "+ error.toString() + "(OBJECT_NOT_FOUND)", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "locationUpdateService/onHandleIntent "+ error.toString() + "(maybe internet issue)", Toast.LENGTH_SHORT).show();
                    }
                    //
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this.getApplicationContext(),"onConnectionFailed",Toast.LENGTH_SHORT).show();
    }
}
