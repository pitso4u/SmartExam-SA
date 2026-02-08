package com.smartexam.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.smartexam.R;

public class SimpleAuthTestActivity extends AppCompatActivity {
    
    private static final String TAG = "SimpleAuthTest";
    private TextView tvStatus;
    private TextView tvUserInfo;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_auth_test);
        
        mAuth = FirebaseAuth.getInstance();
        
        initViews();
        setupListeners();
        checkCurrentUser();
    }
    
    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvUserInfo = findViewById(R.id.tvUserInfo);
        
        Button btnAnonymousSignIn = findViewById(R.id.btnAnonymousSignIn);
        Button btnSignOut = findViewById(R.id.btnSignOut);
    }
    
    private void setupListeners() {
        findViewById(R.id.btnAnonymousSignIn).setOnClickListener(v -> signInAnonymously());
        findViewById(R.id.btnSignOut).setOnClickListener(v -> signOut());
    }
    
    private void checkCurrentUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            updateUI(user);
        } else {
            updateUI(null);
        }
    }
    
    private void signInAnonymously() {
        updateStatus("Signing in anonymously...");
        
        mAuth.signInAnonymously()
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Anonymous sign-in successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                        Toast.makeText(SimpleAuthTestActivity.this, "✅ Sign-in successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "Anonymous sign-in failed", task.getException());
                        updateUI(null);
                        Toast.makeText(SimpleAuthTestActivity.this, "❌ Sign-in failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
    }
    
    private void signOut() {
        mAuth.signOut();
        updateUI(null);
        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
    }
    
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            tvStatus.setText("Status: ✅ Signed In");
            tvUserInfo.setText(
                "User ID: " + user.getUid() + "\n" +
                "Is Anonymous: " + user.isAnonymous() + "\n" +
                "Provider: " + user.getProviderId()
            );
        } else {
            tvStatus.setText("Status: ❌ Not Signed In");
            tvUserInfo.setText("No user information available");
        }
    }
    
    private void updateStatus(String status) {
        tvStatus.setText("Status: " + status);
    }
}
