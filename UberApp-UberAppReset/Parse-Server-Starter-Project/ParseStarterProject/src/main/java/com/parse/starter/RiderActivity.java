package com.parse.starter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Button callBtn;
    Boolean requestActive= false;
    Handler handler = new Handler();
    TextView infoTextView;
    Boolean driverActive = false;
    public void logOut(View view){
        ParseUser.logOut();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
    public void checkForUpdates(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("user", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0){
                    driverActive = true;
                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("username", objects.get(0).getString("driverUsername"));
                    query.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if (e == null && objects.size() > 0){
                                final ParseGeoPoint driverLoc = objects.get(0).getParseGeoPoint("location");
                                if (ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                    if (lastKnownLocation != null){
                                        ParseGeoPoint userLoc = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                        Double distanceInKm = driverLoc.distanceInKilometersTo(userLoc);
                                        if (distanceInKm < 0.01){
                                            infoTextView.setText("Your driver is here!");
                                            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
                                            query.whereEqualTo("user", ParseUser.getCurrentUser().getUsername());
                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if  (e == null){
                                                        for (ParseObject object : objects){
                                                            object.deleteInBackground();
                                                        }
                                                    }
                                                }
                                            });
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    callBtn.setVisibility(View.VISIBLE);
                                                    callBtn.setText("Call Ride");
                                                    requestActive = false;
                                                    driverActive = false;
                                                    infoTextView.setText("");
                                                }
                                            }, 5000);
                                        } else{
                                            Double distanceOneDP = (double) Math.round(distanceInKm*10)/10;
                                            infoTextView.setText("Driver is on the way! "+distanceOneDP+"km(s) away!");
                                            //callBtn.setVisibility(View.INVISIBLE);
                                            LatLng driverLocationLatLng = new LatLng(driverLoc.getLatitude(), driverLoc.getLongitude());
                                            LatLng requestLocationLatLng = new LatLng(userLoc.getLatitude(), userLoc.getLongitude());
                                            ArrayList<Marker> markers = new ArrayList<>();
                                            mMap.clear();

                                            markers.add(mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Driver Location")));
                                            markers.add(mMap.addMarker(new MarkerOptions().position(requestLocationLatLng).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))));
                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            for (Marker marker: markers){
                                                builder.include(marker.getPosition());
                                            }
                                            LatLngBounds bounds = builder.build();
                                            int padding = 60; // offset from edges of the map in pixels
                                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                            mMap.animateCamera(cu);
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    checkForUpdates();
                                                }
                                            }, 2000);
                                        }

                                    }}
                                }
                            }
                    });

                }
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        checkForUpdates();
//                    }
//                }, 2000);
            }
        });
    }
    public void callRide(View view){

        if (requestActive){
            Log.i("Info", "Cancel Ride");
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null){
                        if (objects.size() > 0){
                            for (ParseObject object : objects){
                                object.deleteInBackground();
                            }
                            requestActive = false;
                            callBtn.setText("Call Ride");
                        }
                    }
                }
            });
        } else {
            Log.i("Info", "Call Ride");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null){
                    ParseObject request = new ParseObject("Request");
                    request.put("username", ParseUser.getCurrentUser().getUsername());
                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    request.put("location", parseGeoPoint);
                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null){
                                requestActive = true;
                                callBtn.setText("Cancel Ride");
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        checkForUpdates();
                                    }
                                }, 2000);
                            }
                        }
                    });
                }else {
                    Toast.makeText(this, "Could not find location. Please try again", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.googleMap);
        mapFragment.getMapAsync(this);
        callBtn = (Button) findViewById(R.id.callRideBtn);
        infoTextView = findViewById(R.id.infoTextView);
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){
                    if (objects.size() > 0){
                        requestActive = true;
                        callBtn.setText("Cancle Ride");
                        checkForUpdates();
                    }
                }
            }
        });
    }
    public void updateMap(Location location){
        if (driverActive != true){
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateMap(lastKnownLocation);

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
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location){
                updateMap(location);
            }
            @Override
            public void onStatusChanged(String s, int i , Bundle bundle){

            }
            @Override
            public void onProviderEnabled(String s) {

            }
            @Override
            public void onProviderDisabled(String s){

            }
        };
//        if (Build.VERSION.SDK_INT < 23){
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//        } else {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null){
                updateMap(lastKnownLocation);
            }

        }
        //}

    }
}