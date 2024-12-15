package com.example.MealMate.activities;

import androidx.appcompat.app.*;

import android.annotation.*;
import android.content.*;
import android.net.*;
import android.os.*;

import androidx.core.app.*;
import androidx.core.content.*;

import android.Manifest;
import android.content.pm.*;

import android.view.*;
import android.widget.*;

import com.example.MealMate.R;
import com.google.firebase.firestore.*;

public class ShareItemActivity extends AppCompatActivity {
    Button btn_send;
    EditText et_contact, et_message;

    @SuppressLint("MissingInflatedId")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_item);

        btn_send = (Button) findViewById(R.id.phoneButton);
        et_contact = (EditText) findViewById(R.id.enter_phonenumber);

        PermissionToConnect();

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchItemDetailsAndShare();
            }
        });
    }

    private void fetchItemDetailsAndShare() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("items").document("your_document_id") // Replace with the actual document ID
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String description = documentSnapshot.getString("description");
                        String price = documentSnapshot.getString("price");
                        String location = documentSnapshot.getString("location");
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        Double latitude = documentSnapshot.getDouble("latitude");
                        Double longitude = documentSnapshot.getDouble("longitude");

                        String googleMapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;

                        // Create the message
                        String msg = "Hey, check out this item!\n" +
                                "Name: " + name + "\n" +
                                "Description: " + description + "\n" +
                                "Price: " + price + "\n" +
                                "Location: " + location + "\n" +
                                "Google Maps: " + googleMapsLink + "\n" +
                                "Image: " + imageUrl;

                        shareViaSMS(msg);
                    } else {
                        Toast.makeText(ShareItemActivity.this, "Item not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ShareItemActivity.this, "Failed to fetch item details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void shareViaSMS(String message) {
        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
        sendIntent.setData(Uri.parse("smsto:")); // Opens the messaging app's contact picker
        sendIntent.putExtra("sms_body", message); // Set the message text

        // Check if there's an app that can handle the intent
        if (sendIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(sendIntent);
        } else {
            // If no messaging app is found
            Toast.makeText(ShareItemActivity.this, "No messaging app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void PermissionToConnect() {
        if (ContextCompat.checkSelfPermission(ShareItemActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(ShareItemActivity.this, Manifest.permission.SEND_SMS)) {
                ActivityCompat.requestPermissions(ShareItemActivity.this, new String[]{Manifest.permission.SEND_SMS}, 1);
            } else {
                ActivityCompat.requestPermissions(ShareItemActivity.this, new String[]{Manifest.permission.SEND_SMS}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(ShareItemActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Access granted", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
