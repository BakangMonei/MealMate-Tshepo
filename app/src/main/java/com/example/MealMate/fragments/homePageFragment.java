package com.example.MealMate.fragments;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.MealMate.R;
import com.example.MealMate.activities.EditActivity;
import com.example.MealMate.model.Item;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class homePageFragment extends Fragment {
    private RecyclerView recyclerView;
    private GridAdapter adapter;
    private List<Item> itemList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private CollectionReference itemsCollection;
    private CollectionReference purchasedItemsCollection;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener sensorEventListener;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private long lastShakeTime = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_page, container, false);

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        itemsCollection = firestore.collection("items");
        purchasedItemsCollection = firestore.collection("purchased_items");

        recyclerView = view.findViewById(R.id.items_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        adapter = new GridAdapter();
        recyclerView.setAdapter(adapter);

        setupSwipeGesture();
        setupShakeGesture();

        loadItemsFromFirestore();
        return view;
    }

    private void setupSwipeGesture() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Item item = itemList.get(position);

                if (direction == ItemTouchHelper.LEFT) {
                    showDeleteConfirmationDialog(item, position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    showMarkAsPurchasedConfirmationDialog(item, position);
                }
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void setupShakeGesture() {
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
                if (acceleration > SHAKE_THRESHOLD) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastShakeTime > 1000) { // Prevent multiple triggers in a short time
                        lastShakeTime = currentTime;
                        if (!itemList.isEmpty()) {
                            // Perform action on the first item in the list
                            Item item = itemList.get(0);
                            showMarkAsPurchasedConfirmationDialog(item, 0);
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
    }

    private void showDeleteConfirmationDialog(Item item, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    itemList.remove(position);
                    adapter.notifyItemRemoved(position);
                    removeItemFromDatabase(item.getId());
                })
                .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyItemChanged(position))
                .setCancelable(false)
                .show();
    }

    private void showMarkAsPurchasedConfirmationDialog(Item item, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Mark as Purchased")
                .setMessage("Are you sure you want to mark this item as purchased?")
                .setPositiveButton("Yes", (dialog, which) -> moveToPurchasedCollection(item, position))
                .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyItemChanged(position))
                .setCancelable(false)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (accelerometer != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    private void removeItemFromDatabase(String itemId) {
        itemsCollection.document(itemId).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Item deleted successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting item: " + e.getMessage()));
    }

    private void moveToPurchasedCollection(Item item, int position) {
        purchasedItemsCollection.document(item.getId()).set(item)
                .addOnSuccessListener(aVoid -> itemsCollection.document(item.getId()).delete()
                        .addOnSuccessListener(aVoidDelete -> {
                            Toast.makeText(getContext(), "Item marked as purchased", Toast.LENGTH_SHORT).show();
                            itemList.remove(position);
                            adapter.notifyItemRemoved(position);
                        }))
                .addOnFailureListener(e -> Log.e("Firestore", "Error moving item: " + e.getMessage()));
    }

    private void loadItemsFromFirestore() {
        itemsCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    itemList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Item item = document.toObject(Item.class);
                        item.setId(document.getId());
                        itemList.add(item);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to read data: " + e.getMessage()));
    }

    // GridAdapter inner class
    private class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {
        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView itemName;
            private final TextView itemDescription;
            private final TextView itemTimeStamp;
            private final TextView itemPrice;
            private final ImageView itemImage;

            ViewHolder(View itemView) {
                super(itemView);
                itemName = itemView.findViewById(R.id.item_name);
                itemPrice = itemView.findViewById(R.id.item_price);
                itemImage = itemView.findViewById(R.id.item_image);
                itemDescription = itemView.findViewById(R.id.item_description);
                itemTimeStamp = itemView.findViewById(R.id.item_timestamp);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull GridAdapter.ViewHolder holder, int position) {
            Item item = itemList.get(position);
            holder.itemName.setText(item.getName());
            holder.itemPrice.setText(item.getPrice());
            holder.itemDescription.setText(item.getDescription());
            holder.itemTimeStamp.setText(item.getTimeStamp());

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Picasso.get().load(item.getImageUrl()).into(holder.itemImage);
            } else {
                holder.itemImage.setImageResource(R.drawable.ic_add_photos);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), EditActivity.class);
                intent.putExtra("id", item.getId());
                intent.putExtra("name", item.getName());
                intent.putExtra("description", item.getDescription());
                intent.putExtra("price", item.getPrice());
                intent.putExtra("purchased", item.isPurchased());
                intent.putExtra("imageUrl", item.getImageUrl());
                intent.putExtra("timestamp", item.getTimeStamp());
                startActivity(intent);
            });
        }

        @NonNull
        @Override
        public GridAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }
    }
}
