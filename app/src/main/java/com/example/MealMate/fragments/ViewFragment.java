package com.example.MealMate.fragments;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.bumptech.glide.Glide;
import com.example.MealMate.activities.*;
import com.example.MealMate.adapters.*;
import com.example.MealMate.R;
import com.example.MealMate.model.*;
import com.google.android.material.button.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.util.ArrayList;
import java.util.List;

public class ViewFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView clickableImage;
    private Uri selectedImageUri;
    private FirebaseAuth firebaseAuth;
    private RecyclerView recyclerViewPurchasedItems;
    private PurchasedItemsAdapter purchasedItemsAdapter;
    private List<Item> purchasedItemsList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        clickableImage = view.findViewById(R.id.imageViewProfile);
        MaterialButton logoutButton = view.findViewById(R.id.buttonLogout);

        // Set up RecyclerView for purchased items
        recyclerViewPurchasedItems = view.findViewById(R.id.recyclerViewPurchasedItems);
        recyclerViewPurchasedItems.setLayoutManager(new LinearLayoutManager(getContext()));
        purchasedItemsAdapter = new PurchasedItemsAdapter(purchasedItemsList);
        recyclerViewPurchasedItems.setAdapter(purchasedItemsAdapter);

        // Add swipe functionality with confirmation dialogs
        attachItemTouchHelper();

        // Set up image picker and logout button listeners
        clickableImage.setOnClickListener(v -> openGallery());
        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());

        displayLoggedInUserEmail(view);
        loadUserImage();
        loadPurchasedItems();

        return view;
    }

    private void attachItemTouchHelper() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Item item = purchasedItemsList.get(position);

                if (direction == ItemTouchHelper.LEFT) {
                    showMoveToItemsConfirmationDialog(item, position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    showDeleteConfirmationDialog(item, position);
                }
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerViewPurchasedItems);
    }

    private void showMoveToItemsConfirmationDialog(Item item, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Unmark as Purchased")
                .setMessage("Are you sure you want to unmark this item?")
                .setPositiveButton("Yes", (dialog, which) -> moveToItemsCollection(item, position))
                .setNegativeButton("Cancel", (dialog, which) -> purchasedItemsAdapter.notifyItemChanged(position))
                .setCancelable(false)
                .show();
    }

    private void showDeleteConfirmationDialog(Item item, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> deletePurchasedItem(item, position))
                .setNegativeButton("Cancel", (dialog, which) -> purchasedItemsAdapter.notifyItemChanged(position))
                .setCancelable(false)
                .show();
    }

    private void moveToItemsCollection(Item item, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference itemsCollection = db.collection("items");
        CollectionReference purchasedItemsCollection = db.collection("purchased_items");

        // Add item to "items" collection
        itemsCollection.document(item.getId()).set(item)
                .addOnSuccessListener(aVoid -> {
                    // Remove item from "purchased_items" collection
                    purchasedItemsCollection.document(item.getId()).delete()
                            .addOnSuccessListener(aVoidDelete -> {
                                Toast.makeText(getContext(), "Item moved back to 'items'", Toast.LENGTH_SHORT).show();
                                purchasedItemsList.remove(position);
                                purchasedItemsAdapter.notifyItemRemoved(position);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to remove item from 'purchased_items'", Toast.LENGTH_SHORT).show();
                                purchasedItemsAdapter.notifyItemChanged(position);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to move item to 'items'", Toast.LENGTH_SHORT).show();
                    purchasedItemsAdapter.notifyItemChanged(position);
                });
    }

    private void deletePurchasedItem(Item item, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("purchased_items").document(item.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Item deleted", Toast.LENGTH_SHORT).show();
                    purchasedItemsList.remove(position);
                    purchasedItemsAdapter.notifyItemRemoved(position);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete item", Toast.LENGTH_SHORT).show();
                    purchasedItemsAdapter.notifyItemChanged(position);
                });
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Glide.with(requireContext()).load(selectedImageUri).into(clickableImage);
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void logout() {
        firebaseAuth.signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private void displayLoggedInUserEmail(View view) {
        TextView emailTextView = view.findViewById(R.id.textViewEmail);
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            emailTextView.setText(user.getEmail());
        }
    }

    private void loadUserImage() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference imagesRef = storageRef.child("user_images/" + userEmail + ".jpg");

            imagesRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> Glide.with(requireContext()).load(uri).into(clickableImage))
                    .addOnFailureListener(e -> {
                        // Handle failure to load image
                    });
        }
    }

    private void loadPurchasedItems() {
        FirebaseFirestore.getInstance().collection("purchased_items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    purchasedItemsList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Item item = document.toObject(Item.class);
                        purchasedItemsList.add(item);
                    }
                    purchasedItemsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load purchased items", Toast.LENGTH_SHORT).show());
    }
}
