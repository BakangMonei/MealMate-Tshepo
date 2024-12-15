package com.example.MealMate.fragments;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.activity.result.*;
import androidx.activity.result.contract.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.MealMate.activities.MapActivity;
import com.example.MealMate.R;
import com.example.MealMate.databinding.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class AddItemFragment extends Fragment {
    private FragmentAddItemBinding binding;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;
    private byte[] imageData;
    private String locationAddress;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddItemBinding.inflate(inflater, container, false);
        initializeFirebase();
        setupClickListeners();
        return binding.getRoot();
    }

    private void initializeFirebase() {
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("item_images");
    }

    private void setupClickListeners() {
        binding.imageContainer.setOnClickListener(v -> openImagePicker());
        binding.locationContainer.setOnClickListener(v -> openLocationPicker());
        binding.buttonSubmit.setOnClickListener(v -> validateAndUploadItem());
    }

    private void openImagePicker() {
        galleryLauncher.launch("image/*");
    }

    private void openLocationPicker() {
        Intent intent = new Intent(getContext(), MapActivity.class);
        locationLauncher.launch(intent);
    }

    private void validateAndUploadItem() {
        String name = binding.fieldName.getText().toString().trim();
        String description = binding.fieldDescription.getText().toString().trim();
        String price = binding.fieldPrice.getText().toString().trim();

        if (!validateInputs(name, description, price)) {
            return;
        }

        uploadImage(name, description, price);
    }

    private boolean validateInputs(String name, String description, String price) {
        if (imageData == null) {
            showError("Please select an image");
            return false;
        }
        if (locationAddress == null || latitude == 0.0 || longitude == 0.0) {
            showError("Please select a location");
            return false;
        }
        if (name.isEmpty() || description.isEmpty() || price.isEmpty()) {
            showError("Please fill in all fields");
            return false;
        }
        return true;
    }

    private void uploadImage(String name, String description, String price) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonSubmit.setEnabled(false);

        String imageName = UUID.randomUUID().toString();
        StorageReference fileRef = storageReference.child(imageName + ".jpg");

        fileRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveItemToFirestore(name, description, price, uri.toString()))
                        .addOnFailureListener(e -> handleError("Failed to get download URL")))
                .addOnFailureListener(e -> handleError("Failed to upload image"));
    }

    private void saveItemToFirestore(String name, String description, String price, String imageUrl) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("description", description);
        item.put("price", price);
        item.put("imageUrl", imageUrl);
        item.put("location", locationAddress);
        item.put("latitude", latitude);
        item.put("longitude", longitude);

        // Format the current time as HH:MM:SS
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());
        item.put("timestamp", currentTime);

        firestore.collection("items")
                .add(item)
                .addOnSuccessListener(documentReference -> showSuccessDialog())
                .addOnFailureListener(e -> handleError("Failed to save item"))
                .addOnCompleteListener(task -> {
                    if (isAdded() && getActivity() != null) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.buttonSubmit.setEnabled(true);
                    }
                });
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImageResult
    );

    private void handleImageResult(Uri uri) {
        if (uri != null) {
            try {
                Bitmap bitmap;
                if (Build.VERSION.SDK_INT < 28) {
                    bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                } else {
                    ImageDecoder.Source source = ImageDecoder.createSource(requireActivity().getContentResolver(), uri);
                    bitmap = ImageDecoder.decodeBitmap(source);
                }
                binding.itemImage.setImageBitmap(bitmap);
                binding.imageHint.setVisibility(View.GONE);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                imageData = baos.toByteArray();
            } catch (IOException e) {
                handleError("Failed to process image");
            }
        }
    }

    private final ActivityResultLauncher<Intent> locationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    latitude = result.getData().getDoubleExtra("lat", 0);
                    longitude = result.getData().getDoubleExtra("lon", 0);
                    locationAddress = result.getData().getStringExtra("location_address");

                    binding.locationText.setText(locationAddress);
                    binding.locationHint.setVisibility(View.GONE);
                }
            }
    );

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccessDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Success")
                .setMessage("Item added successfully")
                .setPositiveButton("OK", (dialog, which) -> safeNavigateToHome())
                .setCancelable(false)
                .show();
    }

    private void handleError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.buttonSubmit.setEnabled(true);
        showError(message);
    }

    private void safeNavigateToHome() {
        try {
            if (isAdded() && getActivity() != null && !isDetached()) {
                NavHostFragment.findNavController(AddItemFragment.this)
                        .navigate(R.id.action_addItemFragment_to_homePageFragment);
            }
        } catch (IllegalStateException e) {
            Log.e("AddItemFragment", "Navigation failed", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
