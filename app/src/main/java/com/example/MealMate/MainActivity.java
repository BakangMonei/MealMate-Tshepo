package com.example.MealMate;

import androidx.appcompat.app.*;
import androidx.fragment.app.*;

import android.os.*;
import android.widget.*;

import com.example.MealMate.databinding.*;
import com.example.MealMate.fragments.AddItemFragment;
import com.example.MealMate.fragments.ViewFragment;
import com.example.MealMate.fragments.homePageFragment;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    Fragment fragment;
    private FrameLayout frameLayout;
    private float dY;

    ImageButton imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fragment = new homePageFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).commit();
        frameLayout = findViewById(R.id.frame_layout);

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            //Navigation Bar
            if (itemId == R.id.action_HOME) {
                fragment = new homePageFragment();
            } else if (itemId == R.id.action_ADD) {
                fragment = new AddItemFragment();
            } else if (itemId == R.id.action_VIEW) {
                fragment = new ViewFragment();
            } else {
                fragment = new homePageFragment();
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).commit();
            return true;
        });
    }
}