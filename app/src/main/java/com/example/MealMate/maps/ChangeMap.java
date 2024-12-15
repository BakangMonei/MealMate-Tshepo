package com.example.MealMate.maps;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.appcompat.app.*;

import com.example.MealMate.R;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.*;

import java.util.*;

public class ChangeMap extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap googleMap;
    private Marker marker;
    private Button saveButton;

    private FirebaseFirestore firestore;
    private DocumentReference itemDocument;

    private String id;
    private double latitude, longitude;
    private boolean locationChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_map);

        saveButton = findViewById(R.id.saveLocation);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Retrieve item ID from Intent extras
        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        itemDocument = firestore.collection("items").document(id);

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationChanged) {
                    updateLocationInFirestore();
                } else {
                    Toast.makeText(ChangeMap.this, "No location change", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void updateLocationInFirestore() {
        Map<String, Object> locationUpdate = new HashMap<>();
        locationUpdate.put("latTag", latitude);
        locationUpdate.put("lonTag", longitude);

        itemDocument.update(locationUpdate)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ChangeMap.this, "Location updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChangeMap.this, "Failed to update location", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        double initialLatitude = getIntent().getDoubleExtra("lat", 0.0);
        double initialLongitude = getIntent().getDoubleExtra("lon", 0.0);

        LatLng defaultLocation = new LatLng(initialLatitude, initialLongitude);
        marker = googleMap.addMarker(new MarkerOptions().position(defaultLocation).title("Item Location"));

        float zoomLevel = 14.0f;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, zoomLevel));

        googleMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        // Remove the previous marker if exists
        if (marker != null) {
            marker.remove();
        }
        // Add a new marker at the clicked location
        marker = googleMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
        latitude = latLng.latitude;
        longitude = latLng.longitude;
        locationChanged = true;
    }
}
