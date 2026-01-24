package com.smartexam.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputEditText;
import com.smartexam.R;
import com.smartexam.adapters.QuestionAdapter;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.Question;
import com.smartexam.models.Subject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class QuestionBankActivity extends AppCompatActivity implements QuestionAdapter.OnQuestionActionListener {

    private TextInputEditText etSearch;
    private Spinner spinnerSubjectFilter;
    private RecyclerView rvQuestions;
    private TextView tvEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;

    private AppDatabase database;
    private QuestionAdapter adapter;
    private List<Question> allQuestions = new ArrayList<>();
    private List<Subject> subjects = new ArrayList<>();
    private int selectedSubjectId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_bank);
        setTitle(R.string.title_activity_question_bank);

        database = AppDatabase.getInstance(this);

        initViews();
        setupSearch();
        loadSubjects();
        loadQuestions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadQuestions();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        spinnerSubjectFilter = findViewById(R.id.spinnerSubjectFilter);
        rvQuestions = findViewById(R.id.rvQuestions);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        rvQuestions.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadQuestions();
            swipeRefreshLayout.setRefreshing(false);
        });

        // Match theme colors
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_slate, R.color.accent_peach);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterQuestions();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadSubjects() {
        Executors.newSingleThreadExecutor().execute(() -> {
            subjects = database.subjectDao().getAllSubjects();
            List<String> subjectNames = new ArrayList<>();
            subjectNames.add("All Subjects");
            for (Subject s : subjects) {
                subjectNames.add(s.getName());
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, subjectNames);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerSubjectFilter.setAdapter(spinnerAdapter);

                spinnerSubjectFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            selectedSubjectId = -1;
                        } else {
                            if (position - 1 < subjects.size()) {
                                selectedSubjectId = subjects.get(position - 1).getId();
                            }
                        }
                        filterQuestions();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            });
        });
    }

    private void loadQuestions() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Question> loadedQuestions = database.questionDao().getAllQuestions();
            runOnUiThread(() -> {
                allQuestions.clear();
                allQuestions.addAll(loadedQuestions);
                filterQuestions();
            });
        });
    }

    private void filterQuestions() {
        List<Question> filtered = new ArrayList<>();
        String query = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase().trim() : "";

        String selectedSubjectName = null;
        if (selectedSubjectId != -1) {
            for (Subject s : subjects) {
                if (s.getId() == selectedSubjectId) {
                    selectedSubjectName = s.getName();
                    break;
                }
            }
        }

        for (Question q : allQuestions) {
            boolean matchesSubject = (selectedSubjectId == -1);
            if (!matchesSubject && selectedSubjectName != null && q.getSubject() != null) {
                matchesSubject = q.getSubject().equalsIgnoreCase(selectedSubjectName);
            }

            boolean matchesSearch = query.isEmpty() ||
                    (q.getQuestionText() != null && q.getQuestionText().toLowerCase().contains(query)) ||
                    (q.getTopic() != null && q.getTopic().toLowerCase().contains(query));

            if (matchesSubject && matchesSearch) {
                filtered.add(q);
            }
        }

        updateUI(filtered);
    }

    private void updateUI(List<Question> questions) {
        if (questions.isEmpty()) {
            rvQuestions.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvQuestions.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }

        if (adapter == null) {
            adapter = new QuestionAdapter(this, questions, this);
            rvQuestions.setAdapter(adapter);
        } else {
            adapter.updateQuestions(questions);
        }
    }

    @Override
    public void onEditClick(Question question) {
        Intent intent = new Intent(this, QuestionFormActivity.class);
        intent.putExtra("QUESTION_ID", question.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Question question) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Question")
                .setMessage("Are you sure you want to delete this question?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        database.questionDao().deleteById(question.getId());
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Question deleted", Toast.LENGTH_SHORT).show();
                            loadQuestions();
                        });
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }
}
