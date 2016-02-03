package apps.recon.pawand.hunginair;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, android.hardware.SensorEventListener {

    public static String userId;
    private String userName;
    private String userEmail;
    private String userGender;
    private String userBirthday;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap googleMap;
    private LocationRequest mLocationRequest;
    SensorManager mSensorManager;
    Sensor mRotVectSensor;
    private LatLng userLocation;
    float[] mRotationMatrix = new float[16];
    float mDeclination;
    float angle;
    int trailLength = 5;
    ArrayList<Marker> markerTrailList = new ArrayList<Marker>(trailLength);
    ArrayList<Marker> markerRemoveList = new ArrayList<Marker>();
    Marker UL = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            loadNewInstance();
            //startLocationUpdateServiceCaller();
        }
        else {
            getFragmentManager().findFragmentById(android.R.id.content);
        }
        initEverything();
    }


    public void loadNewInstance(){
        ParseQuery<ParseObject> query = new ParseQuery<>("UserInfoParse");
        query.fromLocalDatastore();
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException ex) {
                if (ex != null) {
                    final int statusCode = ex.getCode();
                    if (statusCode == ParseException.OBJECT_NOT_FOUND) {
                        // Object did not exist on the parse backend
                        FacebookLogin fbFragment = new FacebookLogin();
                        fbFragment.setArguments(getIntent().getExtras());
                        getFragmentManager().beginTransaction().add(android.R.id.content, fbFragment).commit();
                        Toast.makeText(getApplicationContext(), "object does not exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "object exists", Toast.LENGTH_SHORT).show();
                    userId = object.get("userId").toString();
                    userName = object.get("userName").toString();
                    userEmail = object.get("userEmail").toString();
                    userGender = object.get("userGender").toString();
                    // No exception means the object exists
                }
            }
        });
    }

    public void initEverything(){
        initMap();
        initGoogleClient();
        initRotationSensor();
    }

    public void initMap(){
        try {
            if (googleMap == null) {
                googleMap = ((MapFragment) getFragmentManager().
                        findFragmentById(R.id.map)).getMap();
            }
            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initGoogleClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void initRotationSensor(){
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mRotVectSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mRotVectSensor, 40000);
    }

    @Override
    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }



    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, (LocationListener) this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"onConnectionSuspended",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        userLocation = new LatLng(location.getLatitude(),location.getLongitude());
        if(UL!=null) UL.remove();
        UL = googleMap.addMarker(new MarkerOptions().
                position(userLocation).title(userName).flat(false)
                .icon(BitmapDescriptorFactory.fromBitmap(writeTextOnDrawable(R.mipmap.arrow, "You")))
                .alpha((float) 1.0));

        /*if(markerTrailList.size()==trailLength) {markerTrailList.remove(0);markerTrailList.add(UL);}
        else {markerTrailList.add(UL);}

        for(int i =0;i<markerTrailList.size();i++){
            if(i!=4)googleMap.addMarker(new MarkerOptions().
                    position(markerTrailList.get(i).getPosition()).title("").flat(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.arrow))
                    .alpha((float)(i/10.0)));
        }*/

        checkanddrawMarkersFromParse();

        GeomagneticField field = new GeomagneticField(
                (float)location.getLatitude(),
                (float)location.getLongitude(),
                (float)location.getAltitude(),
                System.currentTimeMillis()
        );

        // getDeclination returns degrees
        mDeclination = field.getDeclination();
    }

    public void checkanddrawMarkersFromParse(){
        ParseQuery<ParseObject> query = new ParseQuery<>("UserInfoParse");
        query.fromLocalDatastore();
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException ex) {
                if (ex != null) {
                    final int statusCode = ex.getCode();
                    if (statusCode == ParseException.OBJECT_NOT_FOUND) {
                        // Object did not exist on the parse backend
                        Toast.makeText(getApplicationContext(), "object does not exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "object exists", Toast.LENGTH_SHORT).show();
                    drawMarkersFromParse();
                    // No exception means the object exists
                }
            }
        });
    }

    public void drawMarkersFromParse(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Location");
        query.whereNotEqualTo("user", userName);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objectList, ParseException error) {
                if (error == null) {
                    for (Marker PL : markerRemoveList) {
                        PL.remove();
                    }
                    markerRemoveList.clear();
                    for (ParseObject object : objectList) {
                        String title = object.get("userName").toString();
                        String Lat = object.get("cordX").toString();
                        String Lng = object.get("cordY").toString();
                        Marker PL = googleMap.addMarker(new MarkerOptions().
                                position(new LatLng(Double.valueOf(Lat), Double.valueOf(Lng)))
                                .icon(BitmapDescriptorFactory.fromBitmap(writeTextOnDrawable(R.mipmap.arrow, title)))
                                .title(title).flat(false));
                        markerRemoveList.add(PL);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                    //
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this,"onConnectionFailed",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mRotVectSensor, 40000);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(
                    mRotationMatrix, event.values);
            float[] orientation = new float[3];
            SensorManager.getOrientation(mRotationMatrix, orientation);
            if (Math.abs(Math.toDegrees(orientation[0]) - angle) > 0.8) {
                float bearing = (float) Math.toDegrees(orientation[0]) + mDeclination;
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(userLocation)
                        .zoom(18)
                        .tilt(30)
                        .bearing(bearing)                // Sets the orientation of the camera to east
                        .build();                   // Creates a CameraPosition from the builder
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            angle = (float)Math.toDegrees(orientation[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private Bitmap writeTextOnDrawable(int drawableId, String text) {

        Bitmap bm = BitmapFactory.decodeResource(getResources(), drawableId)
                .copy(Bitmap.Config.ARGB_8888, true);

        Typeface tf = Typeface.create("Helvetica", Typeface.BOLD);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(tf);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(convertToPixels(getApplicationContext(), 11));

        Rect textRect = new Rect();
        paint.getTextBounds(text, 0, text.length(), textRect);

        Canvas canvas = new Canvas(bm);

        //If the text is bigger than the canvas , reduce the font size
        if(textRect.width() >= (canvas.getWidth() - 4))     //the padding on either sides is considered as 4, so as to appropriately fit in the text
            paint.setTextSize(convertToPixels(getApplicationContext(), 7));        //Scaling needs to be used for different dpi's

        //Calculate the positions
        int xPos = (canvas.getWidth() / 2) - 2;     //-2 is for regulating the x position offset

        //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2)) ;

        canvas.drawText(text, xPos, yPos, paint);

        return  bm;
    }


    public static int convertToPixels(Context context, int nDP) {
        final float conversionScale = context.getResources().getDisplayMetrics().density;

        return (int) ((nDP * conversionScale) + 0.5f) ;

    }
}
