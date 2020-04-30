package edu.temple.septaapp;

import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import edu.temple.septaapp.tools.DistanceCalculator;
import edu.temple.septaapp.tools.HttpHandler;

import static android.content.ContentValues.TAG;
import static java.lang.Double.valueOf;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    //Location
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationManager locationManager;
    private LocationListener locationListener;
    boolean mLocationPermissionGranted;
    ArrayList<HashMap<String, String>> busStopList;
    ArrayList<HashMap<String, String>> busList;
    ArrayList<HashMap<String,String>> matches;
    Location currentLocation;
    Location mLastKnownLocation;
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    float DEFAULT_ZOOM = 20;
    Marker currentPositionMarker;
    int numBusStops=0;
    int nearbyBuses;
    int numBuses=0;
    Handler handler;
    Handler handler2;
    Runnable runnable;
    Runnable runnable2;
    public ArrayList<Integer> routes= new ArrayList<>(Arrays.asList(3,6,7,14,16,17,18,21,22,23,25,26,27,40,42,46,47,52,55,56,58,60,64,65,66,68,70,75,84,93,96,97,104,108,109,113,114,117,124,129,132,204,310,110,311));




    private void getLocationPermission() {
        // Check location permission
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 123);
        } else {
            mLocationPermissionGranted = true;
            showLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission") // Already checking necessary permission before calling requestLocationUpdates
    private void showLocationUpdates() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, this.locationListener); // GPS
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        busStopList = new ArrayList<>();
        busList = new ArrayList<>();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = getSystemService(LocationManager.class);
        locationListener = new LocationListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onLocationChanged(Location location) {
                Log.e(TAG,"onLocationChanged called");

                focusOnLocation();

                // Fetch new tags for currentLocation
                if (currentPositionMarker!=null){
                    currentPositionMarker.remove();
                }
                currentLocation = location;
                int height = 25;
                int width = 25;
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.reddotmarker);
                Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
                BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);

                currentPositionMarker = (mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(currentLocation.getLatitude(),
                                currentLocation.getLongitude()))
                        .icon(smallMarkerIcon).flat(true).title("Your Location")));


                drawBusStops();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        getLocationPermission();

        // A handler is needed to called the API every x amount of seconds.
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                // Call the API so we can get the updated coordinates.
                if(nearbyBuses>0){
                    Log.e(TAG,"Number of nearby buses: "+nearbyBuses);
                    drawBuses();
                }
                else{
                    new GetBuses().execute();
                }
                handler.postDelayed(this,15000);
                }
            };

        runnable2 = new Runnable() {
            @Override
            public void run() {
                // Call the API so we can get the updated coordinates.
                if(numBusStops==0){
                    new GetBusStops().execute();
                }
                else{
                }
            }
        };


        // The first time this runs we don't need a delay so we immediately post.
        handler.post(runnable);
        handler2 = new Handler();
        handler2.post(runnable2);


//        new java.util.Timer().schedule(
//                new java.util.TimerTask() {
//                    @Override
//                    public void run() {
//                        Log.e(TAG,"running timer task");
//                        if(numBuses>0){
//                            drawBuses();
//                        }
//                        else{
//                            new GetBuses().execute();
//                        }
//                    }
//                },
//                5000
//        );


        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GetBusStops().execute();
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

    }



    private void drawBuses(){

        if(numBuses>0 && mMap!=null){

            Log.e(TAG,"Trying to draw buses");
            Log.e(TAG,"number of buses: " + numBuses);

            int height = 50;
            int width = 50;
            Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.sbus);
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
            BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);
            nearbyBuses = 0;

            for(int f = 0;f<busList.size();f++){
                String lon = busList.get(f).get("latitude");
                String lat = busList.get(f).get("longitude");
                double lond = valueOf(lon);
                double latd = valueOf(lat);

                LatLng busPos = new LatLng(lond,latd);

                DistanceCalculator distanceCalculator = new DistanceCalculator();
                busList.get(f).get("destination");
                ArrayList<String> busStopListStrings = new ArrayList<>();
                for (int i = 0; i < busStopList.size(); i++) {
                    busStopListStrings.add(busStopList.get(i).get("name"));
                }
                for (int i = 0; i < busList.size(); i++) {
                    for (String element : busStopListStrings){
                        if (element.contains(busList.get(i).get("destination"))){
                            Log.e(TAG,"SUCCESS");
                        }
                    }
                }

                if(distanceCalculator.distanceFromTo(latd,lond,currentLocation.getLongitude(),currentLocation.getLatitude())<1){
                    nearbyBuses++;
                    mMap.addMarker(new MarkerOptions()
                            .title(busList.get(f).get("id"))
                            .position(busPos)
                            .icon(smallMarkerIcon)).setSnippet("Destination: "+ busList.get(f).get("destination"));
                }
                }


        }
    }

    private void drawBusStops(){
        if(numBusStops>0 && mMap!=null){
            Log.e(TAG,"Trying to draw bus stops");
            Log.e(TAG,busStopList.get(0).get("name") + "is the first element");

            int height = 50;
            int width = 50;
            Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.busstop);
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
            BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);

            for(int f = 0;f<busStopList.size();f++){
                String lon = busStopList.get(f).get("latitude");
                String lat = busStopList.get(f).get("longitude");
                double lond = valueOf(lon);
                double latd = valueOf(lat);

                String id = new String(busStopList.get(f).get("id"));
                LatLng busStopPos = new LatLng(lond,latd);

                mMap.addMarker(new MarkerOptions()
                        .title(busStopList.get(f).get("name"))
                        .position(busStopPos)
                        .icon(smallMarkerIcon)).setSnippet("Id: "+id);
            }
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getDeviceLocation();
        focusOnLocation();
        drawBusStops();
    }

    private class GetBusStops extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MapsActivity.this,"Json Data is downloading",Toast.LENGTH_LONG).show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();
            String url;
            String jsonStr;

            // Making a request to url and getting response
            if(currentLocation!=null) {
                url = "https://www3.septa.org/hackathon/locations/get_locations.php?lon=" + currentLocation.getLongitude() + "&lat=" + currentLocation.getLatitude()+"&type=bus_stops&radius=.25";
                jsonStr = sh.makeServiceCall(url);

                Log.e(TAG, "Got a json response");
                String substring = jsonStr.substring(1,jsonStr.length()-1);
                if (jsonStr != null) {
//                Log.e(TAG,""+ substring);
                    try {
                        JSONArray busStops = new JSONArray(jsonStr);
                        Log.e(TAG,"Got a JSONArray");

                        // Getting JSON Array node
                        int l = busStops.length();
                        Log.e(TAG,"This many bus stops received: "+l);
                        numBusStops = l;


                        // looping through All Bus Stops
                        for (int i = 0; i < busStops.length(); i++) {
                            JSONObject c = busStops.getJSONObject(i);
                            String location_id = c.getString("location_id");
                            String location_name = c.getString("location_name");
                            Double latitude = c.getDouble("location_lat");
                            Double longitude = c.getDouble("location_lon");
                            Double distance = c.getDouble("distance");
                            String locationType = c.getString("location_type");

                            // tmp hash map for single contact
                            HashMap<String, String> busStop = new HashMap<>();

                            // adding each child node to HashMap key => value
                            busStop.put("id", location_id);
                            busStop.put("name", location_name);
                            busStop.put("latitude", latitude.toString());
                            busStop.put("longitude", longitude.toString());
                            busStop.put("distance", distance.toString());
                            busStop.put("locationType", locationType);

                            // adding contact to contact list
                            busStopList.add(busStop);
                        }
                    } catch (final JSONException e) {
                        Log.e(TAG, "Json parsing error: " + e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        "Json parsing error: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });

                    }

                } else {
                    Log.e(TAG, "Couldn't get json from server.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Couldn't get json from server. Check LogCat for possible errors!",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
            else{
                Log.e(TAG,"Problem getting last known location");
            }

            return null;
        }



        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            drawBusStops();
            Log.e(TAG,"Finished finding bus stops");
        }
    }

    private class GetBuses extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();
            String url;
            String jsonStr;

            if(currentLocation!=null) {
                for(int i=0;i<routes.size();i++){
                    url = "https://www3.septa.org/hackathon/TransitView/trips.php?route="+ routes.get(i);
                    jsonStr = sh.makeServiceCall(url);

                    if (jsonStr != null){
                        try{
                            JSONObject jsonObject = new JSONObject(jsonStr);
                            JSONArray array = jsonObject.getJSONArray("bus");

                            for(int j = 0 ; j < array.length(); j++) {
                                JSONObject c = array.getJSONObject(j);
                                String bus_id = c.getString("VehicleID");
                                Double latitude = c.getDouble("lat");
                                Double longitude = c.getDouble("lng");
                                String destination = c.getString("destination");

                                // tmp hash map for single contact
                                HashMap<String, String> busHashMap = new HashMap<>();

                                // adding each child node to HashMap key => value
                                busHashMap.put("id", bus_id);
                                busHashMap.put("destination", destination);
                                busHashMap.put("latitude", latitude.toString());
                                busHashMap.put("longitude", longitude.toString());

                                // adding contact to contact list
                                busList.add(busHashMap);
                                numBuses++;
                            }


                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
            return null;
        }



        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.e(TAG,"Finished finding buses");
            drawBuses();
        }
    }

    private void focusOnLocation(){
        if (mMap!= null && currentLocation != null) {
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
            mMap.animateCamera(cameraUpdate);
        }
    }
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Log.e(TAG,"Getting current location and moving camera there");

                Task locationResult = mFusedLocationClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            Log.e(TAG,"Location result successful:" + mLastKnownLocation + "is last known location");

                        } else {
                            Log.e(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);

                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


}

