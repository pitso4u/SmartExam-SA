package com.smartexam.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartexam.R;
import com.smartexam.adapters.PaperAdapter;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.AssessmentPaper;
import com.smartexam.sync.SyncManager;
import com.smartexam.utils.SampleDataGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private TextView tvQuestionCount, tvPaperCount, tvSubjectCount, tvEmptyState;
    private RecyclerView rvRecentPapers;
    private PaperAdapter paperAdapter;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Production Tools
        com.smartexam.config.RemoteConfigManager.getInstance().fetchAndActivate();
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(this)
                .logEvent(com.google.firebase.analytics.FirebaseAnalytics.Event.APP_OPEN, null);

        db = AppDatabase.getInstance(this);
        syncManager = new SyncManager(db);

        initViews();
        setupListeners();
        checkFirstTimeUser();
        checkEmptyDatabase();
    }

    private String buildMockPackPayload() {
        String questionId = "sync-" + UUID.randomUUID();
        return "[{" +
                "\"id\":\"" + questionId + "\"," +
                "\"subject\":\"Mathematics\"," +
                "\"grade\":9," +
                "\"topic\":\"Linear Equations\"," +
                "\"type\":\"MULTIPLE_CHOICE\"," +
                "\"marks\":5," +
                "\"difficulty\":\"Medium\"," +
                "\"questionText\":\"Solve: 2x + 3 = 11\"," +
                "\"content\":{\"options\":\"[\\\"4\\\",\\\"5\\\",\\\"6\\\",\\\"7\\\"]\",\"answer\":\"A\"}" +
                "}]";
    }

    private void initViews() {
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvPaperCount = findViewById(R.id.tvPaperCount);
        tvSubjectCount = findViewById(R.id.tvSubjectCount);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvRecentPapers = findViewById(R.id.rvRecentPapers);

        rvRecentPapers.setLayoutManager(new LinearLayoutManager(this));
        paperAdapter = new PaperAdapter(new ArrayList<>(), this::openPaper);
        rvRecentPapers.setAdapter(paperAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnCreateQuestion).setOnClickListener(v -> {
            startActivity(new Intent(this, QuestionFormActivity.class));
        });

        findViewById(R.id.btnQuestionBank).setOnClickListener(v -> {
            startActivity(new Intent(this, QuestionBankActivity.class));
        });

        findViewById(R.id.btnGenerateTest).setOnClickListener(v -> {
            startActivity(new Intent(this, AssessmentGeneratorActivity.class));
        });

        if (com.smartexam.config.RemoteConfigManager.getInstance().isMarketplaceEnabled()) {
            findViewById(R.id.btnMarketplace).setOnClickListener(v -> {
                startActivity(new Intent(this, MarketplaceActivity.class));
            });
        } else {
            findViewById(R.id.btnMarketplace).setVisibility(View.GONE);
        }

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
    }

    private void checkFirstTimeUser() {
        // Check if user is registered
        com.smartexam.preferences.TeacherSettingsRepository settingsRepository = 
            new com.smartexam.preferences.TeacherSettingsRepository(this);
        com.smartexam.models.TeacherSettings settings = settingsRepository.getSettings();
        
        if (settings.getTeacherName().isEmpty()) {
            // First time user - show registration
            startActivity(new Intent(this, RegistrationActivity.class));
            finish();
        }
    }

    private void checkEmptyDatabase() {
        new Thread(() -> {
            int count = db.questionDao().getQuestionCount();
            if (count == 0) {
                runOnUiThread(() -> {
                    // Show empty state message instead of sample data button
                    tvEmptyState.setText("No questions yet. Create your first question to get started!");
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        new Thread(() -> {
            int qCount = db.questionDao().getQuestionCount();
            int pCount = db.paperDao().getPaperCount();
            int sCount = db.subjectDao().getSubjectCount();
            List<AssessmentPaper> recentPapers = db.paperDao().getRecentPapers(5);

            runOnUiThread(() -> {
                tvQuestionCount.setText(String.valueOf(qCount));
                tvPaperCount.setText(String.valueOf(pCount));
                tvSubjectCount.setText(String.valueOf(sCount));

                paperAdapter.updateData(recentPapers);
                boolean hasPapers = recentPapers != null && !recentPapers.isEmpty();
                rvRecentPapers.setVisibility(hasPapers ? View.VISIBLE : View.GONE);
                tvEmptyState.setVisibility(hasPapers ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    private void openPaper(AssessmentPaper paper) {
        Intent intent = new Intent(this, PaperDetailActivity.class);
        intent.putExtra(PaperDetailActivity.EXTRA_PAPER_ID, paper.getId());
        startActivity(intent);
    }
}
