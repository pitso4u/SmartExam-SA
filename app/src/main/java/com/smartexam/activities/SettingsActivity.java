package com.smartexam.activities;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.smartexam.R;
import com.smartexam.models.TeacherSettings;
import com.smartexam.preferences.TeacherSettingsRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText etTeacherName, etPhoneNumber, etEmail, etAddress,
            etSchoolName, etAcademicYear, etLearnerCount;
    private Spinner spSubscriptionPlan;
    private SwitchMaterial switchDarkTheme;
    private ImageView ivLogoPreview;
    private Button btnSelectLogo;
    private MaterialButton btnSave;

    private ActivityResultLauncher<String> logoPickerLauncher;
    private TeacherSettingsRepository settingsRepository;
    private String currentLogoPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsRepository = new TeacherSettingsRepository(this);
        initViews();
        setupLogoPicker();
        setupSubscriptionSpinner();
        loadSavedSettings();
    }

    private void initViews() {
        etTeacherName = findViewById(R.id.etTeacherName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        etSchoolName = findViewById(R.id.etSchoolName);
        etAcademicYear = findViewById(R.id.etAcademicYear);
        etLearnerCount = findViewById(R.id.etLearnerCount);
        spSubscriptionPlan = findViewById(R.id.spSubscriptionPlan);
        switchDarkTheme = findViewById(R.id.switchDarkTheme);
        ivLogoPreview = findViewById(R.id.ivLogoPreview);
        btnSelectLogo = findViewById(R.id.btnSelectLogo);
        btnSave = findViewById(R.id.btnSaveSettings);

        btnSelectLogo.setOnClickListener(v -> logoPickerLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void setupLogoPicker() {
        logoPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                handleLogoSelection(uri);
            }
        });
    }

    private void setupSubscriptionSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.subscription_plans, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubscriptionPlan.setAdapter(adapter);
    }

    private void loadSavedSettings() {
        TeacherSettings settings = settingsRepository.getSettings();
        etTeacherName.setText(settings.getTeacherName());
        etPhoneNumber.setText(settings.getPhoneNumber());
        etEmail.setText(settings.getEmailAddress());
        etAddress.setText(settings.getAddress());
        etSchoolName.setText(settings.getSchoolName());
        etAcademicYear.setText(settings.getAcademicYear());
        etLearnerCount.setText(settings.getLearnerCount());
        switchDarkTheme.setChecked(settings.isDarkThemeEnabled());

        currentLogoPath = settings.getSchoolLogoPath();
        updateLogoPreview();
        selectPlanInSpinner(settings.getSubscriptionPlan());
    }

    private void selectPlanInSpinner(String plan) {
        if (TextUtils.isEmpty(plan) || spSubscriptionPlan.getAdapter() == null) {
            return;
        }
        for (int i = 0; i < spSubscriptionPlan.getAdapter().getCount(); i++) {
            Object item = spSubscriptionPlan.getAdapter().getItem(i);
            if (item != null && plan.equalsIgnoreCase(item.toString())) {
                spSubscriptionPlan.setSelection(i);
                break;
            }
        }
    }

    private void handleLogoSelection(Uri uri) {
        try {
            File storageDir = new File(getFilesDir(), "logos");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            File logoFile = new File(storageDir, "logo_" + System.currentTimeMillis() + ".jpg");
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                    FileOutputStream outputStream = new FileOutputStream(logoFile)) {
                if (inputStream != null) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, len);
                    }
                }
            }
            currentLogoPath = logoFile.getAbsolutePath();
            updateLogoPreview();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save logo", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLogoPreview() {
        if (!TextUtils.isEmpty(currentLogoPath)) {
            File file = new File(currentLogoPath);
            if (file.exists()) {
                ivLogoPreview.setImageURI(Uri.fromFile(file));
                ivLogoPreview.setVisibility(android.view.View.VISIBLE);
                return;
            }
        }
        ivLogoPreview.setVisibility(android.view.View.GONE);
    }

    private void saveSettings() {
        TeacherSettings settings = new TeacherSettings();
        settings.setTeacherName(getTextValue(etTeacherName));
        settings.setPhoneNumber(getTextValue(etPhoneNumber));
        settings.setEmailAddress(getTextValue(etEmail));
        settings.setAddress(getTextValue(etAddress));
        settings.setSchoolName(getTextValue(etSchoolName));
        settings.setAcademicYear(getTextValue(etAcademicYear));
        settings.setLearnerCount(getTextValue(etLearnerCount));
        settings.setSubscriptionPlan(spSubscriptionPlan.getSelectedItem() != null
                ? spSubscriptionPlan.getSelectedItem().toString()
                : "");
        settings.setDarkThemeEnabled(switchDarkTheme.isChecked());
        settings.setSchoolLogoPath(currentLogoPath);

        settingsRepository.saveSettings(settings);
        applyTheme(settings.isDarkThemeEnabled());
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void applyTheme(boolean useDarkTheme) {
        AppCompatDelegate.setDefaultNightMode(useDarkTheme
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private String getTextValue(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
