package com.smartexam.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.smartexam.R;
import com.smartexam.models.TeacherSettings;
import com.smartexam.preferences.TeacherSettingsRepository;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";
    
    // Authentication fields
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    
    // Teacher profile fields
    private TextInputEditText etTeacherName, etPhoneNumber, etSchoolName, etAcademicYear;
    
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvLogin;
    
    private FirebaseAuth mAuth;
    private TeacherSettingsRepository settingsRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        settingsRepository = new TeacherSettingsRepository(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        // Authentication fields
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        
        // Teacher profile fields
        etTeacherName = findViewById(R.id.etTeacherName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etSchoolName = findViewById(R.id.etSchoolName);
        etAcademicYear = findViewById(R.id.etAcademicYear);
        
        // Buttons and progress
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegistration());
        tvLogin.setOnClickListener(v -> {
            // For now, just show a toast - we can implement login later
            Toast.makeText(this, "Login feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void attemptRegistration() {
        // Reset errors
        etEmail.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);
        etTeacherName.setError(null);

        // Get values
        String email = getTextValue(etEmail);
        String password = getTextValue(etPassword);
        String confirmPassword = getTextValue(etConfirmPassword);
        String teacherName = getTextValue(etTeacherName);
        String phoneNumber = getTextValue(etPhoneNumber);
        String schoolName = getTextValue(etSchoolName);
        String academicYear = getTextValue(etAcademicYear);

        boolean cancel = false;
        android.view.View focusView = null;

        // Check for password match
        if (!TextUtils.isEmpty(password) && !password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            focusView = etConfirmPassword;
            cancel = true;
        }

        // Check for valid password
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            focusView = etPassword;
            cancel = true;
        }

        // Check for valid email
        if (TextUtils.isEmpty(email) || !isEmailValid(email)) {
            etEmail.setError("Please enter a valid email address");
            focusView = etEmail;
            cancel = true;
        }

        // Check for teacher name
        if (TextUtils.isEmpty(teacherName)) {
            etTeacherName.setError("Teacher name is required");
            focusView = etTeacherName;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            createFirebaseAccount(email, password, teacherName, phoneNumber, schoolName, academicYear);
        }
    }

    private void createFirebaseAccount(String email, String password, String teacherName, 
                                     String phoneNumber, String schoolName, String academicYear) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase account creation successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        
                        // Save teacher profile
                        saveTeacherProfile(teacherName, phoneNumber, schoolName, academicYear, email);
                        
                    } else {
                        Log.w(TAG, "Firebase account creation failed", task.getException());
                        showProgress(false);
                        Toast.makeText(RegistrationActivity.this, 
                            "Registration failed: " + task.getException().getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                }
            });
    }

    private void saveTeacherProfile(String teacherName, String phoneNumber, String schoolName, 
                                   String academicYear, String email) {
        TeacherSettings settings = new TeacherSettings();
        settings.setTeacherName(teacherName);
        settings.setPhoneNumber(phoneNumber);
        settings.setSchoolName(schoolName);
        settings.setAcademicYear(academicYear);
        settings.setEmailAddress(email);
        settings.setLearnerCount("0");
        settings.setAddress("");
        settings.setSubscriptionPlan("Free");
        settings.setDarkThemeEnabled(false);
        settings.setSchoolLogoPath("");

        settingsRepository.saveSettings(settings);
        
        Log.d(TAG, "Teacher profile saved successfully");
        
        showProgress(false);
        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
        
        // Navigate to main app
        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private String getTextValue(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        btnRegister.setEnabled(!show);
    }
}
