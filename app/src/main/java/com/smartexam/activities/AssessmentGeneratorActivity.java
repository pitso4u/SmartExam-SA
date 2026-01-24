package com.smartexam.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.app.DatePickerDialog;
import android.text.format.DateFormat;
import android.widget.EditText;
import com.google.android.material.button.MaterialButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartexam.R;
import com.smartexam.adapters.SelectableQuestionAdapter;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.AssessmentPaper;
import com.smartexam.models.PaperQuestion;
import com.smartexam.models.Question;
import com.smartexam.models.Subject;
import com.smartexam.models.TeacherSettings;
import com.smartexam.preferences.TeacherSettingsRepository;
import com.smartexam.utils.DbUtils;
import com.smartexam.utils.PDFGenerator;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssessmentGeneratorActivity extends AppCompatActivity {

    private EditText etPaperTitle, etTargetGrade, etTargetMarks, etExamDate;
    private Spinner spTargetSubject;
    private TextView tvStatus, tvEmptyQuestions, tvSelectionSummary;
    private ProgressBar progressGenerating;
    private Button btnGenerate, btnOpenTestPdf, btnOpenMemoPdf, btnClearSelection;
    private MaterialButton btnDeletePaper, btnEditPaper;
    private RecyclerView rvQuestions;
    private AppDatabase db;
    private PDFGenerator pdfGenerator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<Subject> subjects;
    private String lastTestPath;
    private String lastMemoPath;
    private SelectableQuestionAdapter questionAdapter;
    private TeacherSettingsRepository settingsRepository;
    private AssessmentPaper editingPaper;
    private long selectedExamDate = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment_generator);

        db = AppDatabase.getInstance(this);
        pdfGenerator = new PDFGenerator();
        settingsRepository = new TeacherSettingsRepository(this);

        etPaperTitle = findViewById(R.id.etPaperTitle);
        spTargetSubject = findViewById(R.id.spTargetSubject);
        etTargetGrade = findViewById(R.id.etTargetGrade);
        etTargetMarks = findViewById(R.id.etTargetMarks);
        etExamDate = findViewById(R.id.etExamDate);
        tvStatus = findViewById(R.id.tvStatus);
        tvEmptyQuestions = findViewById(R.id.tvEmptyQuestions);
        tvSelectionSummary = findViewById(R.id.tvSelectionSummary);
        progressGenerating = findViewById(R.id.progressGenerating);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnOpenTestPdf = findViewById(R.id.btnOpenTestPdf);
        btnOpenMemoPdf = findViewById(R.id.btnOpenMemoPdf);
        btnClearSelection = findViewById(R.id.btnClearSelection);
        rvQuestions = findViewById(R.id.rvQuestions);
        btnDeletePaper = findViewById(R.id.btnDeletePaper);
        btnEditPaper = findViewById(R.id.btnEditPaper);

        setupQuestionList();

        setupSubjectSpinner();
        setupGradeWatcher();
        setupExamDatePicker();
        bindExistingPaperIfAny();

        btnGenerate.setOnClickListener(v -> generateAssessment());
        btnOpenTestPdf.setOnClickListener(v -> openPdf(lastTestPath));
        btnOpenMemoPdf.setOnClickListener(v -> openPdf(lastMemoPath));
        btnClearSelection.setOnClickListener(v -> questionAdapter.clearSelection());
        btnDeletePaper.setOnClickListener(v -> deletePaper());
        btnEditPaper.setOnClickListener(v -> prefillForEditing());
    }

    private void setupSubjectSpinner() {
        executor.execute(() -> {
            subjects = db.subjectDao().getAllSubjects();
            runOnUiThread(() -> {
                ArrayAdapter<Subject> subAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, subjects);
                subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spTargetSubject.setAdapter(subAdapter);
                spTargetSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        refreshQuestionList();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // no-op
                    }
                });
            });
        });
    }

    private void setupExamDatePicker() {
        etExamDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selectedExamDate);
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, dayOfMonth, 0, 0, 0);
                        selectedExamDate = picked.getTimeInMillis();
                        etExamDate.setText(DateFormat.format("dd MMM yyyy", selectedExamDate));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });
        etExamDate.setText(DateFormat.format("dd MMM yyyy", selectedExamDate));
    }

    private void bindExistingPaperIfAny() {
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra("EXTRA_EDIT_PAPER_ID")) {
            toggleEditButtons(false);
            return;
        }
        String paperId = intent.getStringExtra("EXTRA_EDIT_PAPER_ID");
        if (paperId == null) {
            toggleEditButtons(false);
            return;
        }
        executor.execute(() -> {
            AssessmentPaper paper = db.paperDao().getPaperById(paperId);
            if (paper == null) {
                runOnUiThread(() -> toggleEditButtons(false));
                return;
            }
            editingPaper = paper;
            selectedExamDate = paper.getExamDate();
            List<Question> paperQuestions = db.paperDao().getQuestionsForPaper(paperId);
            runOnUiThread(() -> applyPaperToUi(paper, paperQuestions));
        });
    }

    private void applyPaperToUi(AssessmentPaper paper, List<Question> paperQuestions) {
        etPaperTitle.setText(paper.getTitle());
        etTargetGrade.setText(String.valueOf(paper.getGrade()));
        etTargetMarks.setText(String.valueOf(paper.getTotalMarks()));
        etExamDate.setText(DateFormat.format("dd MMM yyyy", selectedExamDate));
        if (subjects != null) {
            for (int i = 0; i < subjects.size(); i++) {
                if (subjects.get(i).getId() == paper.getSubjectId()) {
                    spTargetSubject.setSelection(i);
                    break;
                }
            }
        }
        questionAdapter.setQuestions(paperQuestions);
        toggleEditButtons(true);
    }

    private void toggleEditButtons(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        btnDeletePaper.setVisibility(vis);
        btnEditPaper.setVisibility(vis);
    }

    private void deletePaper() {
        if (editingPaper == null) {
            Toast.makeText(this, "No paper loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            db.paperDao().deleteById(editingPaper.getId());
            runOnUiThread(() -> {
                Toast.makeText(this, "Paper deleted", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void prefillForEditing() {
        if (editingPaper == null) {
            Toast.makeText(this, "No paper loaded", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupGradeWatcher() {
        etTargetGrade.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refreshQuestionList();
            }
        });
    }

    private void setupQuestionList() {
        questionAdapter = new SelectableQuestionAdapter((count, marks) -> {
            tvSelectionSummary.setText(String.format("Selected: %d questions • %d marks", count, marks));
            btnClearSelection.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        });
        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        rvQuestions.setAdapter(questionAdapter);
    }

    private void refreshQuestionList() {
        Subject selectedSubject = (Subject) spTargetSubject.getSelectedItem();
        String gradeText = etTargetGrade.getText().toString().trim();

        if (selectedSubject == null || gradeText.isEmpty()) {
            showEmptyQuestionsMessage(getString(R.string.message_select_filters));
            questionAdapter.setQuestions(Collections.emptyList());
            return;
        }

        int grade;
        try {
            grade = Integer.parseInt(gradeText);
        } catch (NumberFormatException ex) {
            showEmptyQuestionsMessage(getString(R.string.message_invalid_grade));
            questionAdapter.setQuestions(Collections.emptyList());
            return;
        }

        tvEmptyQuestions.setVisibility(View.VISIBLE);
        tvEmptyQuestions.setText(R.string.message_loading_questions);

        executor.execute(() -> {
            List<Question> pool = db.questionDao()
                    .getQuestionsForSubject(selectedSubject.getName(), grade);

            runOnUiThread(() -> {
                questionAdapter.setQuestions(pool);
                if (pool.isEmpty()) {
                    showEmptyQuestionsMessage(getString(R.string.message_no_questions));
                } else {
                    tvEmptyQuestions.setVisibility(View.GONE);
                }
            });
        });
    }

    private void showEmptyQuestionsMessage(String message) {
        tvEmptyQuestions.setVisibility(View.VISIBLE);
        tvEmptyQuestions.setText(message);
    }

    private void generateAssessment() {
        String title = etPaperTitle.getText().toString().trim();
        Subject selectedSubject = (Subject) spTargetSubject.getSelectedItem();
        String gradeStr = etTargetGrade.getText().toString().trim();
        String marksStr = etTargetMarks.getText().toString().trim();

        if (title.isEmpty() || selectedSubject == null || gradeStr.isEmpty()) {
            Toast.makeText(this, "Please provide the title, subject and grade", Toast.LENGTH_SHORT).show();
            return;
        }

        int grade;
        Integer targetMarks = null;
        try {
            grade = Integer.parseInt(gradeStr);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Grade and marks must be numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!marksStr.isEmpty()) {
            try {
                targetMarks = Integer.parseInt(marksStr);
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Total marks must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        List<Question> selectedQuestions = questionAdapter.getSelectedQuestions();
        int selectedMarks = questionAdapter.getSelectedMarksTotal();

        if (selectedQuestions.isEmpty()) {
            Toast.makeText(this, "Select at least one question", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetMarks != null && selectedMarks != targetMarks) {
            Toast.makeText(this,
                    "Warning: Selected marks (" + selectedMarks + ") differ from target (" + targetMarks + ")",
                    Toast.LENGTH_SHORT)
                    .show();
        }

        TeacherSettings teacherSettings = settingsRepository.getSettings();
        pdfGenerator.configureSchoolDetails(
                teacherSettings.getSchoolName(),
                teacherSettings.getTeacherName(),
                teacherSettings.getSchoolLogoPath());

        setLoading(true);
        showStatus("Generating assessment...", false);
        toggleResultButtons(false);

        executor.execute(() -> {
            try {
                File dir = getExternalFilesDir(null);
                String testPath = new File(dir, "Test_" + System.currentTimeMillis() + ".pdf").getAbsolutePath();
                String memoPath = new File(dir, "Memo_" + System.currentTimeMillis() + ".pdf").getAbsolutePath();

                pdfGenerator.generateTest(testPath, title, selectedSubject.getName(), grade, selectedQuestions);
                pdfGenerator.generateMemo(memoPath, title, selectedQuestions);

                AssessmentPaper paper = new AssessmentPaper();
                paper.setTitle(title);
                paper.setSubjectId(selectedSubject.getId());
                paper.setGrade(grade);
                paper.setTotalMarks(selectedMarks);
                paper.setFilePath(testPath);

                db.paperDao().insert(paper);

                // Track questions used in paper
                List<PaperQuestion> paperQuestions = new ArrayList<>();
                for (int i = 0; i < selectedQuestions.size(); i++) {
                    paperQuestions.add(new PaperQuestion(paper.getId(), selectedQuestions.get(i).getId(), i + 1));
                }
                db.paperDao().insertPaperQuestions(paperQuestions);

                lastTestPath = testPath;
                lastMemoPath = memoPath;

                runOnUiThread(() -> {
                    setLoading(false);
                    showStatus("Success! Paper saved.", false);
                    toggleResultButtons(true);
                    Toast.makeText(this, "Assessment generated successfully", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    setLoading(false);
                    showStatus("Error generating PDF", true);
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        btnGenerate.setEnabled(!loading);
        progressGenerating.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showStatus(String message, boolean isError) {
        tvStatus.setVisibility(View.VISIBLE);
        int color = ContextCompat.getColor(this,
                isError ? android.R.color.holo_red_dark : R.color.teal_700);
        tvStatus.setTextColor(color);
        tvStatus.setText(message);
    }

    private void toggleResultButtons(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        btnOpenTestPdf.setVisibility(visibility);
        btnOpenMemoPdf.setVisibility(visibility);
    }

    private void openPdf(String path) {
        if (path == null) {
            Toast.makeText(this, "Generate an assessment first", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(this, "No PDF viewer available", Toast.LENGTH_SHORT).show();
        }
    }
}
