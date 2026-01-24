package com.smartexam.activities;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartexam.R;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.Question;
import com.smartexam.models.QuestionType;
import com.smartexam.models.Subject;
import com.smartexam.utils.DbUtils;
import androidx.activity.result.ActivityResultLauncher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuestionFormActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "QUESTION_ID";
    private static final String[] DIFFICULTY_LEVELS = { "Low", "Medium", "High", "Extreme" };

    private AutoCompleteTextView actvSubject;
    private Spinner spType, spDifficulty, spCognitiveLevel;
    private EditText etGrade, etTopic, etMarks, etQuestionText;
    private TextView tvFormTitle;
    private Button btnSave;

    // Dynamic Layouts
    private CardView cvDynamicContent;
    private LinearLayout layoutMcq, layoutTrueFalse, layoutMatch, layoutFillBlank;

    // MCQ inputs
    private EditText etOptionA, etOptionB, etOptionC, etOptionD;
    private RadioGroup rgMcqAnswer;

    // True/False inputs
    private RadioGroup rgTrueFalse;

    // Match inputs
    private EditText etMatchLeft1, etMatchRight1, etMatchLeft2, etMatchRight2;
    private EditText etMatchLeft3, etMatchRight3, etMatchLeft4, etMatchRight4;

    // Fill Blank
    private EditText etFillAnswer;

    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<Subject> subjects = new ArrayList<>();
    private final List<String> subjectNames = new ArrayList<>();
    private ArrayAdapter<String> subjectAdapter;
    private final Gson gson = new Gson();

    private String editingQuestionId = null;
    private Question existingQuestion = null;

    private String selectedImagePath = null; // Stored locally
    private ActivityResultLauncher<String> pickImageLauncher;
    private android.widget.ImageView ivQuestionImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_form);

        // Image Picker
        pickImageLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleImageSelection(uri);
                    }
                });

        db = AppDatabase.getInstance(this);
        initViews();
        setupLogic();
        handleIntent();
    }

    private void initViews() {
        // Core fields
        actvSubject = findViewById(R.id.actvSubject);
        etGrade = findViewById(R.id.etGrade);
        etTopic = findViewById(R.id.etTopic);
        etMarks = findViewById(R.id.etMarks);
        etQuestionText = findViewById(R.id.etQuestionText);
        spType = findViewById(R.id.spType);
        spDifficulty = findViewById(R.id.spDifficulty);
        spCognitiveLevel = findViewById(R.id.spCognitiveLevel);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        btnSave = findViewById(R.id.btnSave);

        subjectAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, subjectNames);
        actvSubject.setAdapter(subjectAdapter);

        // Image UI
        Button btnAddImage = findViewById(R.id.btnAddImage);
        ivQuestionImage = findViewById(R.id.ivQuestionImage);

        btnAddImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Dynamic Containers
        cvDynamicContent = findViewById(R.id.cvDynamicContent);
        layoutMcq = findViewById(R.id.layoutMcq);
        layoutTrueFalse = findViewById(R.id.layoutTrueFalse);
        layoutMatch = findViewById(R.id.layoutMatch);
        layoutFillBlank = findViewById(R.id.layoutFillBlank);

        // MCQ
        etOptionA = findViewById(R.id.etOptionA);
        etOptionB = findViewById(R.id.etOptionB);
        etOptionC = findViewById(R.id.etOptionC);
        etOptionD = findViewById(R.id.etOptionD);
        rgMcqAnswer = findViewById(R.id.rgMcqAnswer);

        // True/False
        rgTrueFalse = findViewById(R.id.rgTrueFalse);

        // Match
        etMatchLeft1 = findViewById(R.id.etMatchLeft1);
        etMatchRight1 = findViewById(R.id.etMatchRight1);
        etMatchLeft2 = findViewById(R.id.etMatchLeft2);
        etMatchRight2 = findViewById(R.id.etMatchRight2);
        etMatchLeft3 = findViewById(R.id.etMatchLeft3);
        etMatchRight3 = findViewById(R.id.etMatchRight3);
        etMatchLeft4 = findViewById(R.id.etMatchLeft4);
        etMatchRight4 = findViewById(R.id.etMatchRight4);

        // Fill
        etFillAnswer = findViewById(R.id.etFillAnswer);

        btnSave.setOnClickListener(v -> saveQuestion());
    }

    private void handleImageSelection(Uri uri) {
        executor.execute(() -> {
            try {
                File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "questions");
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }

                String fileName = "question_" + System.currentTimeMillis() + ".jpg";
                File file = new File(storageDir, fileName);

                try (InputStream inputStream = getContentResolver().openInputStream(uri);
                        FileOutputStream outputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while (inputStream != null && (length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }

                selectedImagePath = file.getAbsolutePath();

                runOnUiThread(() -> {
                    ivQuestionImage.setVisibility(View.VISIBLE);
                    ivQuestionImage.setImageURI(Uri.fromFile(file));
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupLogic() {
        refreshSubjects();

        // Question Types
        ArrayAdapter<QuestionType> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, QuestionType.values());
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        // Difficulties
        ArrayAdapter<String> diffAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, DIFFICULTY_LEVELS);
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDifficulty.setAdapter(diffAdapter);

        // Cognitive Levels
        ArrayAdapter<com.smartexam.models.CognitiveLevel> cogAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, com.smartexam.models.CognitiveLevel.values());
        cogAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCognitiveLevel.setAdapter(cogAdapter);

        // Type Selection Listener
        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateDynamicLayout((QuestionType) spType.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateDynamicLayout(QuestionType type) {
        cvDynamicContent.setVisibility(View.VISIBLE);
        layoutMcq.setVisibility(View.GONE);
        layoutTrueFalse.setVisibility(View.GONE);
        layoutMatch.setVisibility(View.GONE);
        layoutFillBlank.setVisibility(View.GONE);

        switch (type) {
            case MULTIPLE_CHOICE:
                layoutMcq.setVisibility(View.VISIBLE);
                break;
            case TRUE_FALSE:
                layoutTrueFalse.setVisibility(View.VISIBLE);
                break;
            case MATCH_COLUMNS:
                layoutMatch.setVisibility(View.VISIBLE);
                break;
            case FILL_IN_BLANKS: // Fallthrough for simple text answer types
            case CHOOSE_CORRECT_WORD:
                layoutFillBlank.setVisibility(View.VISIBLE);
                break;
            default:
                cvDynamicContent.setVisibility(View.GONE);
                break;
        }
    }

    private void saveQuestion() {
        String subjectName = actvSubject.getText().toString().trim();
        String gradeStr = etGrade.getText().toString().trim();
        String topic = etTopic.getText().toString().trim();
        String marksStr = etMarks.getText().toString().trim();
        String text = etQuestionText.getText().toString().trim();
        QuestionType type = (QuestionType) spType.getSelectedItem();

        if (TextUtils.isEmpty(subjectName) || TextUtils.isEmpty(gradeStr) ||
                TextUtils.isEmpty(topic) || TextUtils.isEmpty(marksStr) || TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Please fill all core fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> contentMap = new HashMap<>();

        // Add Image Path if exists
        if (selectedImagePath != null) {
            contentMap.put("imagePath", selectedImagePath);
        }

        // Validate and gather Type-specific data
        if (!collectTypeData(type, contentMap)) {
            return;
        }

        executor.execute(() -> {
            // Check/Save Subject
            Subject subject = db.subjectDao().getSubjectByName(subjectName);
            if (subject == null) {
                db.subjectDao().insert(new Subject(subjectName));
                refreshSubjects();
            }

            // Create Question
            Question question = new Question();
            if (existingQuestion != null) {
                question.setId(existingQuestion.getId());
                question.setCreatedAt(existingQuestion.getCreatedAt());
            } else {
                question.setId(UUID.randomUUID().toString());
                question.setCreatedAt(System.currentTimeMillis());
            }
            question.setSubject(subjectName);
            question.setGrade(Integer.parseInt(gradeStr));
            question.setTopic(topic);
            question.setMarks(Integer.parseInt(marksStr));
            question.setQuestionText(text);
            question.setType(type);
            question.setDifficulty((String) spDifficulty.getSelectedItem());
            question.setCognitiveLevel((com.smartexam.models.CognitiveLevel) spCognitiveLevel.getSelectedItem());
            question.setContent(contentMap);

            db.questionDao().insert(question);

            runOnUiThread(() -> {
                Toast.makeText(this,
                        existingQuestion != null ? "Question updated" : "Question saved successfully",
                        Toast.LENGTH_SHORT)
                        .show();
                finish();
            });
        });
    }

    private void handleIntent() {
        editingQuestionId = getIntent().getStringExtra(EXTRA_QUESTION_ID);
        if (TextUtils.isEmpty(editingQuestionId)) {
            return;
        }
        tvFormTitle.setText("Edit Question");
        btnSave.setText("Update Question");
        loadQuestionDetails(editingQuestionId);
    }

    private void loadQuestionDetails(String questionId) {
        executor.execute(() -> {
            Question question = db.questionDao().getQuestionById(questionId);
            if (question == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Question not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }
            existingQuestion = question;
            runOnUiThread(() -> populateFormWithQuestion(existingQuestion));
        });
    }

    private void populateFormWithQuestion(Question question) {
        if (question == null)
            return;

        actvSubject.setText(question.getSubject(), false);
        etGrade.setText(String.valueOf(question.getGrade()));
        etTopic.setText(question.getTopic());
        etMarks.setText(String.valueOf(question.getMarks()));
        etQuestionText.setText(question.getQuestionText());

        QuestionType type = question.getType() != null ? question.getType() : QuestionType.MULTIPLE_CHOICE;
        spType.setSelection(type.ordinal());
        updateDynamicLayout(type);

        spDifficulty.setSelection(getDifficultyIndex(question.getDifficulty()));

        if (question.getCognitiveLevel() != null) {
            spCognitiveLevel.setSelection(question.getCognitiveLevel().ordinal());
        }

        Map<String, String> content = question.getContent() != null ? question.getContent() : new HashMap<>();
        selectedImagePath = content.get("imagePath");
        if (!TextUtils.isEmpty(selectedImagePath)) {
            ivQuestionImage.setVisibility(View.VISIBLE);
            ivQuestionImage.setImageURI(Uri.fromFile(new File(selectedImagePath)));
        } else {
            ivQuestionImage.setVisibility(View.GONE);
        }

        switch (type) {
            case MULTIPLE_CHOICE:
                populateMcq(content);
                break;
            case TRUE_FALSE:
                populateTrueFalse(content);
                break;
            case MATCH_COLUMNS:
                populateMatchColumns(content);
                break;
            case FILL_IN_BLANKS:
            case CHOOSE_CORRECT_WORD:
                populateFillBlank(content);
                break;
            default:
                break;
        }
    }

    private int getDifficultyIndex(String difficulty) {
        if (difficulty == null)
            return 0;
        int index = Arrays.asList(DIFFICULTY_LEVELS).indexOf(difficulty);
        return Math.max(index, 0);
    }

    private void populateMcq(Map<String, String> content) {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        List<String> options = new ArrayList<>();
        if (content.containsKey("options")) {
            options = gson.fromJson(content.get("options"), listType);
        }
        if (options.size() >= 4) {
            etOptionA.setText(options.get(0));
            etOptionB.setText(options.get(1));
            etOptionC.setText(options.get(2));
            etOptionD.setText(options.get(3));
        }
        String answer = content.get("answer");
        if (answer != null) {
            switch (answer) {
                case "B":
                    rgMcqAnswer.check(R.id.rbB);
                    break;
                case "C":
                    rgMcqAnswer.check(R.id.rbC);
                    break;
                case "D":
                    rgMcqAnswer.check(R.id.rbD);
                    break;
                default:
                    rgMcqAnswer.check(R.id.rbA);
                    break;
            }
        }
    }

    private void populateTrueFalse(Map<String, String> content) {
        String answer = content.get("answer");
        boolean isTrue = answer == null || Boolean.parseBoolean(answer);
        rgTrueFalse.check(isTrue ? R.id.rbTrue : R.id.rbFalse);
    }

    private void populateMatchColumns(Map<String, String> content) {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        List<String> colA = content.containsKey("columnA") ? gson.fromJson(content.get("columnA"), listType)
                : new ArrayList<>();
        List<String> colB = content.containsKey("columnB") ? gson.fromJson(content.get("columnB"), listType)
                : new ArrayList<>();

        setMatchPair(etMatchLeft1, etMatchRight1, colA, colB, 0);
        setMatchPair(etMatchLeft2, etMatchRight2, colA, colB, 1);
        setMatchPair(etMatchLeft3, etMatchRight3, colA, colB, 2);
        setMatchPair(etMatchLeft4, etMatchRight4, colA, colB, 3);
    }

    private void setMatchPair(EditText left, EditText right, List<String> colA, List<String> colB, int index) {
        if (colA.size() > index) {
            left.setText(colA.get(index));
        }
        if (colB.size() > index) {
            right.setText(colB.get(index));
        }
    }

    private void populateFillBlank(Map<String, String> content) {
        etFillAnswer.setText(content.get("answer"));
    }

    private void refreshSubjects() {
        executor.execute(() -> {
            List<Subject> loadedSubjects = db.subjectDao().getAllSubjects();
            runOnUiThread(() -> {
                subjects.clear();
                subjects.addAll(loadedSubjects);
                subjectNames.clear();
                for (Subject s : subjects) {
                    subjectNames.add(s.getName());
                }
                subjectAdapter.notifyDataSetChanged();
            });
        });
    }

    private boolean collectTypeData(QuestionType type, Map<String, String> map) {
        switch (type) {
            case MULTIPLE_CHOICE:
                String optA = etOptionA.getText().toString().trim();
                String optB = etOptionB.getText().toString().trim();
                String optC = etOptionC.getText().toString().trim();
                String optD = etOptionD.getText().toString().trim();

                if (TextUtils.isEmpty(optA) || TextUtils.isEmpty(optB) ||
                        TextUtils.isEmpty(optC) || TextUtils.isEmpty(optD)) {
                    Toast.makeText(this, "Please fill all options", Toast.LENGTH_SHORT).show();
                    return false;
                }

                int checkedId = rgMcqAnswer.getCheckedRadioButtonId();
                if (checkedId == -1) {
                    Toast.makeText(this, "Select correct answer", Toast.LENGTH_SHORT).show();
                    return false;
                }

                List<String> options = new ArrayList<>();
                options.add(optA);
                options.add(optB);
                options.add(optC);
                options.add(optD);
                map.put("options", gson.toJson(options));

                String ans = "A";
                if (checkedId == R.id.rbB)
                    ans = "B";
                else if (checkedId == R.id.rbC)
                    ans = "C";
                else if (checkedId == R.id.rbD)
                    ans = "D";
                map.put("answer", ans);
                return true;

            case TRUE_FALSE:
                if (rgTrueFalse.getCheckedRadioButtonId() == -1) {
                    Toast.makeText(this, "Select True or False", Toast.LENGTH_SHORT).show();
                    return false;
                }
                boolean isTrue = (rgTrueFalse.getCheckedRadioButtonId() == R.id.rbTrue);
                map.put("answer", String.valueOf(isTrue));
                return true;

            case MATCH_COLUMNS:
                List<String> colA = new ArrayList<>();
                List<String> colB = new ArrayList<>();
                Map<String, String> mapping = new HashMap<>();

                // Helper to add if filled
                addMatchPair(colA, colB, mapping, etMatchLeft1, etMatchRight1);
                addMatchPair(colA, colB, mapping, etMatchLeft2, etMatchRight2);
                addMatchPair(colA, colB, mapping, etMatchLeft3, etMatchRight3);
                addMatchPair(colA, colB, mapping, etMatchLeft4, etMatchRight4);

                if (colA.isEmpty()) {
                    Toast.makeText(this, "Add at least one pair", Toast.LENGTH_SHORT).show();
                    return false;
                }

                map.put("columnA", gson.toJson(colA));
                map.put("columnB", gson.toJson(colB));
                map.put("mapping", gson.toJson(mapping));
                return true;

            case FILL_IN_BLANKS:
            case CHOOSE_CORRECT_WORD:
                String fillAns = etFillAnswer.getText().toString().trim();
                if (TextUtils.isEmpty(fillAns)) {
                    Toast.makeText(this, "Provide the answer", Toast.LENGTH_SHORT).show();
                    return false;
                }
                map.put("answer", fillAns);
                return true;

            default:
                return true;
        }
    }

    private void addMatchPair(List<String> colA, List<String> colB, Map<String, String> map, EditText left,
            EditText right) {
        String l = left.getText().toString().trim();
        String r = right.getText().toString().trim();
        if (!TextUtils.isEmpty(l) && !TextUtils.isEmpty(r)) {
            colA.add(l);
            colB.add(r);
            map.put(l, r); // Simple mapping left -> right
        }
    }
}
