package com.example.MealMate.activities;

import androidx.annotation.*;
import androidx.appcompat.app.*;

import android.annotation.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.util.*;
import android.widget.*;

import com.example.MealMate.MainActivity;
import com.example.MealMate.R;
import com.example.MealMate.maps.ChangeMap;
import com.google.android.material.floatingactionbutton.*;
import com.google.android.material.textfield.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;
import com.squareup.picasso.*;

import java.io.*;
import java.util.*;

public class EditActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    FloatingActionButton markItemAsPurchasedFAB, deleteItemFAB, shareItemFAB, tagItemFAB, openGalleryFAB;
    TextInputEditText itemName, itemDescription, itemPrice, itemLocation;
    ImageView itemImage;
    Button saveChangesButton;

    private Uri imageUri;
    private byte[] imageData;
    private String id, name, description, price, imageUrl;
    private boolean isPurchased;
    String latitude, longitude, location;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private CollectionReference itemsCollection;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        // Initialize views
        markItemAsPurchasedFAB = findViewById(R.id.mark_purchased);
        deleteItemFAB = findViewById(R.id.delete);
        shareItemFAB = findViewById(R.id.forward_item);
        tagItemFAB = findViewById(R.id.map);
        openGalleryFAB = findViewById(R.id.open_gallery);
        itemName = findViewById(R.id.editTextItemName);
        itemDescription = findViewById(R.id.editTextItemDescription);
        itemPrice = findViewById(R.id.editTextItemPrice);
        itemImage = findViewById(R.id.item_image);
        saveChangesButton = findViewById(R.id.btnUpdate);
        itemLocation = findViewById(R.id.editTextLocation);

        // Retrieve intent extras
        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        name = intent.getStringExtra("name");
        description = intent.getStringExtra("description");
        price = intent.getStringExtra("price");
        location = intent.getStringExtra("location");
        isPurchased = intent.getBooleanExtra("purchased", false);
        imageUrl = intent.getStringExtra("imageUrl");
        latitude = intent.getStringExtra("latitude");
        longitude = intent.getStringExtra("longitude");

        // Populate EditText fields with item details
        itemName.setText(name);
        itemDescription.setText(description);
        itemPrice.setText(price);
        itemLocation.setText(location);


        // Load image
        Picasso.get().load(imageUrl).into(itemImage);

        // Initialize Firestore and Firebase Storage
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        itemsCollection = firestore.collection("items");

        // Save Changes button
        saveChangesButton.setOnClickListener(view -> confirmAndUpdateItem());

        // FAB listeners
        markItemAsPurchasedFAB.setOnClickListener(v -> togglePurchasedStatus());
        deleteItemFAB.setOnClickListener(v -> confirmAndDeleteItem());
        shareItemFAB.setOnClickListener(v -> shareItemDetails());
        tagItemFAB.setOnClickListener(v -> tagItemOnMap());
        openGalleryFAB.setOnClickListener(v -> openGallery());

        itemLocation.setOnClickListener(v -> {
            if (latitude != null && longitude != null) {
                Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + location);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void confirmAndUpdateItem() {
        new AlertDialog.Builder(this)
                .setTitle("Update Item")
                .setMessage("Are you sure you want to update this item?")
                .setPositiveButton("Yes", (dialog, which) -> updateItem())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateItem() {
        Map<String, Object> updates = new HashMap<>();
        String updatedName = itemName.getText().toString().trim();
        String updatedDescription = itemDescription.getText().toString().trim();
        String updatedPrice = itemPrice.getText().toString().trim();
        String updatedLocation = itemLocation.getText().toString().trim();

        // Add only the fields that were changed
        if (!updatedName.equals(name)) updates.put("name", updatedName);
        if (!updatedDescription.equals(description)) updates.put("description", updatedDescription);
        if (!updatedPrice.equals(price)) updates.put("price", updatedPrice);
        if (!updatedLocation.equals(location)) updates.put("location", updatedLocation);

        if (!updates.isEmpty()) {
            itemsCollection.document(id).update(updates).addOnSuccessListener(aVoid -> {
                Toast.makeText(EditActivity.this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            }).addOnFailureListener(e -> Log.e("Firestore", "Error updating item: " + e.getMessage()));
        } else {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmAndDeleteItem() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> deleteItem())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteItem() {
        itemsCollection.document(id).delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(EditActivity.this, "Item deleted successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Log.e("Firestore", "Error deleting item: " + e.getMessage()));
    }

    private void togglePurchasedStatus() {
        isPurchased = !isPurchased;
        markItemAsPurchasedFAB.setColorFilter(getResources().getColor(isPurchased ? R.color.Green : R.color.red));
        itemsCollection.document(id).update("purchased", isPurchased).addOnSuccessListener(aVoid -> {
            String message = isPurchased ? "Item marked as purchased" : "Item marked as not purchased";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> Log.e("Firestore", "Error updating purchase status: " + e.getMessage()));
    }

    private void shareItemDetails() {
        String googleMapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;
        String message = String.format("Hey, check out this item:\nName: %s\nDescription: %s\nPrice: %s\nLocation: %s\nCoordinates: %s, %s\nGoogle Maps: %s\nImage: %s",
                name, description, price, location, latitude, longitude, googleMapsLink, imageUrl);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);

        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(shareIntent, "Share Item Details via"));
        } else {
            Toast.makeText(this, "No sharing apps found", Toast.LENGTH_SHORT).show();
        }
    }


    private void tagItemOnMap() {
        Intent intent = new Intent(EditActivity.this, ChangeMap.class);
        intent.putExtra("id", id);
        intent.putExtra("lat", latitude);
        intent.putExtra("lon", longitude);
        intent.putExtra("location", location);
        startActivityForResult(intent, 2); // Request code 2 for map updates
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            compressAndUploadImage(imageUri);
            itemImage.setImageURI(imageUri);
        }
    }

    private void compressAndUploadImage(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(imageStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
            imageData = baos.toByteArray();
            uploadCompressedImage();
        } catch (IOException e) {
            Log.e("Image Upload", "Error compressing image: " + e.getMessage());
        }
    }

    private void uploadCompressedImage() {
        StorageReference storageRef = storage.getReference().child("item_images/" + UUID.randomUUID().toString());
        storageRef.putBytes(imageData).addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
            imageUrl = downloadUrl.toString();
            itemsCollection.document(id).update("imageUrl", imageUrl);
        })).addOnFailureListener(e -> Log.e("Firebase", "Image upload failed: " + e.getMessage()));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
