package com.isprid.turnchargelk.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.android.SphericalUtil;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.isprid.turnchargelk.R;
import com.isprid.turnchargelk.model.EvPlace;
import com.isprid.turnchargelk.util.AppConfig;
import com.isprid.turnchargelk.util.Constant;
import com.isprid.turnchargelk.util.EVDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EvPlacesActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private final long INTERVAL = 30000;

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;
    private LatLng mSelectedLatLang = null;
    private CardView infoLayout;
    private Polyline polylineFinal;

    private Button btnDirection;
    private ArrayList<EvPlace> list;
    private final ArrayList<Marker> markersList = new ArrayList<>();
    private TextView text_name, text_contact;
    private TextView text_type, text_address;
    private Marker mSelectedMarker = null;
    private DatabaseReference databaseReference;

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Location");
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car_icon));
                mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

                //move map camera
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                // Zoom in the Google Map
                mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                if(!list.isEmpty()) {
                    getNearestPlace();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Objects.requireNonNull(getSupportActionBar()).setTitle("All Charging Stations");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        databaseReference = FirebaseDatabase.getInstance().getReference(Constant.EV_PLACES);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFrag != null;
        mapFrag.getMapAsync(this);

        infoLayout = findViewById(R.id.info_layout);
        btnDirection = findViewById(R.id.btn_direction);
        TextView text_destination = findViewById(R.id.text_destination);
        text_name = findViewById(R.id.text_name);
        text_type = findViewById(R.id.text_type);
        text_contact = findViewById(R.id.text_contact);
        text_address = findViewById(R.id.text_address);

        text_destination.setVisibility(View.GONE);

        infoLayout.setVisibility(View.GONE);

        btnDirection.setOnClickListener(v -> {
            if (mLastLocation != null && mSelectedLatLang != null) {
                try {
                    btnDirection.setEnabled(false);
                    drawLocation();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("TAG", "mLastLocation != null && mSelectedLatLang != null");
            }
        });
    }

    private void getChargingStation() {
        list = new ArrayList<>();
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    EvPlace evPlace = ds.getValue(EvPlace.class);
                    assert evPlace != null;
                    list.add(evPlace);
                }

                pinLocationsToMap(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // calling on cancelled method when we receive
                // any error or we are not able to get the data.
                Toast.makeText(EvPlacesActivity.this, "Fail to get data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawLocation() {
        String start = mLastLocation.getLatitude() + "," + mLastLocation.getLongitude();
        String end = mSelectedLatLang.latitude + "," + mSelectedLatLang.longitude;

        //Define list to get all latlng for the route
        List<LatLng> path = new ArrayList();


        //Execute Directions API request
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(AppConfig.MAP_API_KEY)
                .build();
        DirectionsApiRequest req = DirectionsApi.getDirections(context, start, end);
        try {
            DirectionsResult res = req.await();

            //Loop through legs and steps to get encoded poly lines of each step
            if (res.routes != null && res.routes.length > 0) {
                DirectionsRoute route = res.routes[0];

                if (route.legs != null) {
                    for (int i = 0; i < route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j = 0; j < leg.steps.length; j++) {
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length > 0) {
                                    for (int k = 0; k < step.steps.length; k++) {
                                        DirectionsStep step1 = step.steps[k];
                                        EncodedPolyline points1 = step1.polyline;
                                        if (points1 != null) {
                                            //Decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                            for (com.google.maps.model.LatLng coord1 : coords1) {
                                                path.add(new LatLng(coord1.lat, coord1.lng));
                                            }
                                        }
                                    }
                                } else {
                                    EncodedPolyline points = step.polyline;
                                    if (points != null) {
                                        //Decode polyline and add points to list of route coordinates
                                        List<com.google.maps.model.LatLng> coords = points.decodePath();
                                        for (com.google.maps.model.LatLng coord : coords) {
                                            path.add(new LatLng(coord.lat, coord.lng));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("TAG", ex.getLocalizedMessage());
        }

        //Draw the polyline
        if (path.size() > 0) {
            PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(8);
            if (polylineFinal != null) {
                polylineFinal.remove();
            }
            polylineFinal = mGoogleMap.addPolyline(opts);
        }
        btnDirection.setEnabled(true);

    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnMapClickListener(this);
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL); // two minute interval
        mLocationRequest.setFastestInterval(INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        }

        getChargingStation();
    }

    private void pinLocationsToMap(ArrayList<EvPlace> list) {
        for (EvPlace evp : list) {
            LatLng latLng = new LatLng(evp.getLatitude(), evp.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(evp.getName());
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.charging_station_map));
            Marker marker = mGoogleMap.addMarker(markerOptions);
            markersList.add(marker);
        }
    }

    private void getNearestPlace() {
        try {
            EvPlace evPlace = calculateNearestStation(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            changeMarkerImage(evPlace);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void changeMarkerImage(EvPlace evPlace) {
        for (Marker marker : markersList) {
            if (marker.getPosition().latitude == evPlace.getLatitude() && marker.getPosition().longitude == evPlace.getLongitude() && marker.getTitle().equals(evPlace.getName())) {
                if (null != mSelectedMarker) {
                    mSelectedMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.charging_station_map));
                }
                mSelectedMarker = marker;
                mSelectedMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.charging_station_yellow));
            }
        }
    }

    private EvPlace calculateNearestStation(double latitude, double longitude) {
        EvPlace min = list.get(0);
        min.setDistance(SphericalUtil.computeDistanceBetween(new LatLng(latitude, longitude), list.get(0).getLocation()));
        Log.e("Nearest", min.getName() + " > " + min.getDistance());
        for (int i = 1; i < list.size(); i++) {
            double distance = SphericalUtil.computeDistanceBetween(new LatLng(latitude, longitude), list.get(i).getLocation());
            list.get(i).setDistance(distance);
            Log.e("Nearest", list.get(i).getName() + " > " + list.get(i).getDistance());
            if (distance < min.getDistance()) {
                min = list.get(i);
            }
        }

        return min;
    }

    private void calculateDistance(LatLng position) {
        if (mLastLocation != null) {
            Location locationB = new Location("point B");

            locationB.setLatitude(position.latitude);
            locationB.setLongitude(position.longitude);

            float distance = mLastLocation.distanceTo(locationB);
            Log.e("Distance>", distance + "m | " + (distance / 1000) + "km");
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(EvPlacesActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the location-related task you need to do.
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mGoogleMap.setMyLocationEnabled(true);
                }

            } else {

                // permission denied, boo! Disable the functionality that depends on this permission.
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.e("TAG", "" + marker.getTitle());
        mSelectedLatLang = marker.getPosition();
        calculateDistance(marker.getPosition());

        if (marker.getTitle().equals("Current Location")) {
            infoLayout.setVisibility(View.GONE);
        } else {
            infoLayout.setVisibility(View.VISIBLE);
            text_type.setVisibility(View.VISIBLE);
            text_contact.setVisibility(View.VISIBLE);
            text_address.setVisibility(View.VISIBLE);

            EvPlace evPlace = getMarkerDetails(marker);
            if (evPlace != null) {
                text_name.setText(evPlace.getName());
                text_type.setText(evPlace.getType());
                text_contact.setText(evPlace.getContact());
                text_address.setText(evPlace.getAddress());
            } else {
                text_name.setVisibility(View.GONE);
                text_type.setVisibility(View.GONE);
                text_contact.setVisibility(View.GONE);
                text_address.setVisibility(View.GONE);
            }
        }
        return false;
    }

    private EvPlace getMarkerDetails(Marker marker) {
        for (EvPlace evp : list) {
            if (evp.getLatitude() == marker.getPosition().latitude && evp.getLongitude() == marker.getPosition().longitude && evp.getName().equals(marker.getTitle())) {
                return evp;
            }
        }
        return null;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        infoLayout.setVisibility(View.GONE);
    }
}