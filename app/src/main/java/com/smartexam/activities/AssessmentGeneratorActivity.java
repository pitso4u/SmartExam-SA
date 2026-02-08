package com.smartexam.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.smartexam.utils.PDFGenerator;
import java.io.ByteArrayOutputStream;

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
    private boolean isEditing = false;

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
        btnEditPaper.setOnClickListener(v -> {
            if (isEditing) {
                savePaperChanges();
            } else {
                prefillForEditing();
            }
        });
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
        btnGenerate.setVisibility(View.GONE);
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
        setFieldsEnabled(false);
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
            return;
        }
        isEditing = true;
        setFieldsEnabled(true);
        btnEditPaper.setText("Save");
        btnEditPaper.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_save));
    }

    private void savePaperChanges() {
        String title = etPaperTitle.getText().toString().trim();
        Subject selectedSubject = (Subject) spTargetSubject.getSelectedItem();
        String gradeStr = etTargetGrade.getText().toString().trim();
        String marksStr = etTargetMarks.getText().toString().trim();

        if (title.isEmpty() || selectedSubject == null || gradeStr.isEmpty()) {
            Toast.makeText(this, "Title, subject and grade are required", Toast.LENGTH_SHORT).show();
            return;
        }

        int grade;
        int totalMarks = 0;
        try {
            grade = Integer.parseInt(gradeStr);
            if (!marksStr.isEmpty()) {
                totalMarks = Integer.parseInt(marksStr);
            }
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Grade and marks must be numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Question> selectedQuestions = questionAdapter.getSelectedQuestions();
        if (selectedQuestions.isEmpty()) {
            Toast.makeText(this, "Please select at least one question", Toast.LENGTH_SHORT).show();
            return;
        }

        editingPaper.setTitle(title);
        editingPaper.setSubjectId(selectedSubject.getId());
        editingPaper.setGrade(grade);
        editingPaper.setTotalMarks(totalMarks);
        editingPaper.setExamDate(selectedExamDate);

        List<PaperQuestion> paperQuestions = new ArrayList<>();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            Question q = selectedQuestions.get(i);
            paperQuestions.add(new PaperQuestion(editingPaper.getId(), q.getId(), i));
        }

        executor.execute(() -> {
            db.runInTransaction(() -> {
                db.paperDao().update(editingPaper);
                db.paperDao().clearPaperQuestions(editingPaper.getId());
                db.paperDao().insertPaperQuestions(paperQuestions);
            });
            runOnUiThread(() -> {
                isEditing = false;
                setFieldsEnabled(false);
                btnEditPaper.setText("Edit");
                btnEditPaper.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_edit));
                Toast.makeText(this, "Paper updated successfully", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        etPaperTitle.setEnabled(enabled);
        spTargetSubject.setEnabled(enabled);
        etTargetGrade.setEnabled(enabled);
        etTargetMarks.setEnabled(enabled);
        etExamDate.setEnabled(enabled);
        questionAdapter.setSelectionEnabled(enabled);
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

        if (title.isEmpty() || selectedSubject == null || gradeStr.isEmpty()) {
            Toast.makeText(this, "Please provide the title, subject and grade", Toast.LENGTH_SHORT).show();
            return;
        }

        int grade;
        try {
            grade = Integer.parseInt(gradeStr);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Grade must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Question> selectedQuestions = questionAdapter.getSelectedQuestions();
        if (selectedQuestions.isEmpty()) {
            Toast.makeText(this, "Please select at least one question", Toast.LENGTH_SHORT).show();
            return;
        }

        progressGenerating.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Generating assessment...");
        btnGenerate.setEnabled(false);

        executor.execute(() -> {
            try {
                TeacherSettings settings = settingsRepository.getSettings();
                pdfGenerator.configureSchoolDetails(settings.getSchoolName(), settings.getTeacherName(), settings.getSchoolLogoPath());

                // Load company logo from drawable resources
                byte[] logoBytes = loadCompanyLogoBytes();

                String safeTitle = title.replaceAll("[^a-zA-Z0-9_\\s-]", "").replace(" ", "_");
                File documentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (documentsDir != null && !documentsDir.exists()) {
                    documentsDir.mkdirs();
                }
                String testFileName = "Test_" + safeTitle + "_" + System.currentTimeMillis() + ".pdf";
                String memoFileName = "Memo_" + safeTitle + "_" + System.currentTimeMillis() + ".pdf";

                File testFile = new File(documentsDir, testFileName);
                File memoFile = new File(documentsDir, memoFileName);

                lastTestPath = testFile.getAbsolutePath();
                lastMemoPath = memoFile.getAbsolutePath();

                pdfGenerator.generateTest(lastTestPath, title, selectedSubject.getName(), grade, selectedQuestions, logoBytes);
                pdfGenerator.generateMemo(lastMemoPath, title, selectedQuestions, logoBytes);

                AssessmentPaper newPaper = new AssessmentPaper();
                newPaper.setTitle(title);
                newPaper.setSubjectId(selectedSubject.getId());
                newPaper.setGrade(grade);
                newPaper.setTotalMarks(questionAdapter.getSelectedMarksTotal());
                newPaper.setExamDate(selectedExamDate);
                newPaper.setFilePath(lastTestPath);

                List<PaperQuestion> paperQuestions = new ArrayList<>();
                for (int i = 0; i < selectedQuestions.size(); i++) {
                    paperQuestions.add(new PaperQuestion(newPaper.getId(), selectedQuestions.get(i).getId(), i));
                }

                db.runInTransaction(() -> {
                    db.paperDao().insert(newPaper);
                    db.paperDao().insertPaperQuestions(paperQuestions);
                });

                runOnUiThread(() -> {
                    progressGenerating.setVisibility(View.GONE);
                    tvStatus.setText("Successfully generated!");
                    btnGenerate.setEnabled(true);
                    btnOpenTestPdf.setVisibility(View.VISIBLE);
                    btnOpenMemoPdf.setVisibility(View.VISIBLE);
                    Toast.makeText(AssessmentGeneratorActivity.this, "Assessment generated!", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressGenerating.setVisibility(View.GONE);
                    tvStatus.setText("Error generating PDF.");
                    btnGenerate.setEnabled(true);
                    Toast.makeText(AssessmentGeneratorActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openPdf(String path) {
        if (path == null) {
            Toast.makeText(this, "File not available", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(path);
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app to view PDF", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load company logo from drawable resources and convert to byte array
     */
    private byte[] loadCompanyLogoBytes() {
        try {
            // Load drawable as bitmap
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.smartexamsalogo);
            if (bitmap == null) {
                return null;
            }

            // Convert bitmap to PNG byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            stream.close();

            return byteArray;
        } catch (Exception e) {
            // Silently fail if logo can't be loaded
            return null;
        }
    }
}
