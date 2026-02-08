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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smartexam.R;
import java.util.HashMap;
import java.util.Map;

public class FirebaseConnectionTestActivity extends AppCompatActivity {
    
    private static final String TAG = "FirebaseTest";
    private TextView tvStatus;
    private TextView tvResults;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_test);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvResults = findViewById(R.id.tvResults);
        
        Button btnTestAuth = findViewById(R.id.btnTestAuth);
        Button btnTestFirestore = findViewById(R.id.btnTestFirestore);
        Button btnWriteTestData = findViewById(R.id.btnWriteTestData);
        Button btnReadTestData = findViewById(R.id.btnReadTestData);
        Button btnClearResults = findViewById(R.id.btnClearResults);
    }
    
    private void setupListeners() {
        findViewById(R.id.btnTestAuth).setOnClickListener(v -> testFirebaseAuth());
        findViewById(R.id.btnTestFirestore).setOnClickListener(v -> testFirestoreConnection());
        findViewById(R.id.btnWriteTestData).setOnClickListener(v -> writeTestData());
        findViewById(R.id.btnReadTestData).setOnClickListener(v -> readTestData());
        findViewById(R.id.btnClearResults).setOnClickListener(v -> clearResults());
    }
    
    private void testFirebaseAuth() {
        updateStatus("Testing Firebase Auth...");
        
        // Test anonymous sign-in
        mAuth.signInAnonymously()
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String successMsg = "✅ Auth successful!\nUser ID: " + 
                                         (user != null ? user.getUid() : "null") +
                                         "\nUser is anonymous: " + 
                                         (user != null ? user.isAnonymous() : "false");
                        updateStatus("Auth Test Complete");
                        appendResult(successMsg);
                        Log.d(TAG, "Anonymous auth success: " + user.getUid());
                    } else {
                        String errorMsg = "❌ Auth failed: " + task.getException().getMessage();
                        updateStatus("Auth Test Failed");
                        appendResult(errorMsg);
                        Log.e(TAG, "Anonymous auth failed", task.getException());
                    }
                }
            });
    }
    
    private void testFirestoreConnection() {
        updateStatus("Testing Firestore Connection...");
        
        // Test basic connectivity by trying to read a collection
        db.collection("test_connection")
            .limit(1)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        String successMsg = "✅ Firestore connection successful!\n" +
                                         "Can read collections from project: smartexam-sa";
                        updateStatus("Firestore Test Complete");
                        appendResult(successMsg);
                        Log.d(TAG, "Firestore connection test successful");
                    } else {
                        String errorMsg = "❌ Firestore connection failed: " + 
                                         task.getException().getMessage();
                        updateStatus("Firestore Test Failed");
                        appendResult(errorMsg);
                        Log.e(TAG, "Firestore connection failed", task.getException());
                    }
                }
            });
    }
    
    private void writeTestData() {
        updateStatus("Writing test data to Firestore...");
        
        // Create a test document
        Map<String, Object> testData = new HashMap<>();
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("testType", "smartexam_connection_test");
        testData.put("deviceInfo", android.os.Build.MODEL);
        testData.put("appVersion", "1.0");
        
        db.collection("connection_tests")
            .add(testData)
            .addOnSuccessListener(documentReference -> {
                String successMsg = "✅ Data write successful!\n" +
                                 "Document ID: " + documentReference.getId() +
                                 "\nWritten to collection: connection_tests";
                updateStatus("Write Test Complete");
                appendResult(successMsg);
                Log.d(TAG, "Test data written with ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                String errorMsg = "❌ Data write failed: " + e.getMessage();
                updateStatus("Write Test Failed");
                appendResult(errorMsg);
                Log.e(TAG, "Test data write failed", e);
            });
    }
    
    private void readTestData() {
        updateStatus("Reading test data from Firestore...");
        
        db.collection("connection_tests")
            .orderBy("timestamp")
            .limit(5)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        StringBuilder result = new StringBuilder();
                        result.append("✅ Data read successful!\n");
                        result.append("Found ").append(task.getResult().size()).append(" test documents:\n\n");
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            result.append("📄 Doc ID: ").append(document.getId()).append("\n");
                            result.append("   Type: ").append(document.getString("testType")).append("\n");
                            result.append("   Device: ").append(document.getString("deviceInfo")).append("\n");
                            result.append("   Timestamp: ").append(document.getLong("timestamp")).append("\n\n");
                        }
                        
                        updateStatus("Read Test Complete");
                        appendResult(result.toString());
                        Log.d(TAG, "Read " + task.getResult().size() + " test documents");
                    } else {
                        String errorMsg = "❌ Data read failed: " + task.getException().getMessage();
                        updateStatus("Read Test Failed");
                        appendResult(errorMsg);
                        Log.e(TAG, "Test data read failed", task.getException());
                    }
                }
            });
    }
    
    private void updateStatus(String status) {
        tvStatus.setText("Status: " + status);
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    }
    
    private void appendResult(String result) {
        String currentResults = tvResults.getText().toString();
        String newResults = currentResults + "\n\n" + result;
        tvResults.setText(newResults);
    }
    
    private void clearResults() {
        tvResults.setText("Results will appear here...");
        tvStatus.setText("Status: Ready");
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            appendResult("👤 User already signed in: " + currentUser.getUid() + 
                        (currentUser.isAnonymous() ? " (anonymous)" : ""));
        }
    }
}
