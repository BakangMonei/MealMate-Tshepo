package com.example.MealMate.activities;

import static android.content.ContentValues.TAG;

import android.content.*;
import android.os.*;
import android.util.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.appcompat.app.*;

import com.example.MealMate.MainActivity;
import com.example.MealMate.R;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.*;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private Button loginButton;
    private TextView createAccountButton;

    private EditText emailEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Authorization
        firebaseAuth = FirebaseAuth.getInstance();

        // Assign the components to their xml components
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        createAccountButton = findViewById(R.id.createAccountButton);

        // Check if the user is already authenticated
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, navigate to the home activity
            openHomeActivity();
            finish(); // Optional: Close the current activity
        }

        // Login Button onClick Listener
        loginButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Upon successful login, do this
                signInWithEmailAndPassword(email, password);
            }
        });

        // Create Account Button onClick Listener
        createAccountButton.setOnClickListener(view -> {
            // Launch the SignupActivity
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    // Method to sign in a user with Email and Password
    private void signInWithEmailAndPassword(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "signInWithEmail:success");
                    Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                    openHomeActivity();
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                    Toast.makeText(LoginActivity.this, "Authentication failed. ", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Method to open the HomeActivity
    private void openHomeActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: finish the current activity
    }

    // Method to handle logout
    private void handleLogout() {
        firebaseAuth.signOut();
        // Optionally, navigate the user to the login screen or perform other actions
    }
}
