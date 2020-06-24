package com.parse.starter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {
    ListView requestListView ;
    ArrayList<String> requests = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    ArrayList<Double> requestLatitude = new ArrayList<>();
    ArrayList<Double> requestLongitude = new ArrayList<>();
    LocationManager locationManager;
    LocationListener locationListener;
    ArrayList<String> usernames = new ArrayList<>();
    public void updateListView(Location loc){
        if (loc != null){

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
            final ParseGeoPoint geoPointLoc = new ParseGeoPoint(loc.getLatitude(),loc.getLongitude());
            query.whereNear("location", geoPointLoc);
            query.whereDoesNotExist("driverUsername");
            query.setLimit(10);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null){
                        requests.clear();
                        requestLatitude.clear();
                        requestLongitude.clear();
                        if (objects.size()>0){
                            for (ParseObject object: objects){
                                ParseGeoPoint requestLoc = (ParseGeoPoint) object.get("location");
                                if (requestLoc != null){
                                    Double distanceInKm = geoPointLoc.distanceInKilometersTo(requestLoc);
                                    Double distanceOneDP = (double) Math.round(distanceInKm*10)/10;
                                    requests.add(distanceOneDP.toString()+ "km(s)");
                                    requestLatitude.add(requestLoc.getLatitude());
                                    requestLongitude.add(requestLoc.getLongitude());
                                    usernames.add(object.getString("username"));
                                }

                            }
                            arrayAdapter.notifyDataSetChanged();
                        } else {
                            requests.add("No active requests nearby");

                        }arrayAdapter.notifyDataSetChanged();

                    }
                }
            });


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
                updateListView(lastKnownLocation);

            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);
        setTitle("Nearby Requests");
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);
        requests.clear();
        requests.add("Getting nearby requests...");

        requestListView = (ListView) findViewById(R.id.requestListView);
        requestListView.setAdapter(arrayAdapter);
        requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (ContextCompat.checkSelfPermission(ViewRequestsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (requestLatitude.size()>i && requestLongitude.size() > i && usernames.size() > i && lastKnownLocation!= null){
                        Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                        intent.putExtra("requestLatitude", requestLatitude.get(i));
                        intent.putExtra("requestLongitude", requestLongitude.get(i));
                        intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
                        intent.putExtra("driverLongitude", lastKnownLocation.getLongitude());
                        intent.putExtra("username", usernames.get(i));
                        startActivity(intent);

                    }
                }

            }
        });
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location){

                updateListView(location);
                ParseUser.getCurrentUser().put("location",new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
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
                updateListView(lastKnownLocation);
            }

        }
        //}

    }

}