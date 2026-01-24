package com.smartexam.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.Question;
import com.smartexam.models.QuestionType;
import com.smartexam.models.Subject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataGenerator {
        private static final int QUESTIONS_PER_SUBJECT = 10;

        private final Context context;
        private final AppDatabase db;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final Gson gson = new Gson();

        public SampleDataGenerator(Context context) {
                this.context = context;
                this.db = AppDatabase.getInstance(context);
        }

        public void generateSampleData() {
                generateSampleData(null);
        }

        public void generateSampleData(Runnable onComplete) {
                new Thread(() -> {
                        List<SubjectConfig> configs = buildCapsSubjectConfigs();

                        for (SubjectConfig config : configs) {
                                ensureSubjectExists(config.subjectName);

                                int existingCount = db.questionDao().getQuestionCountForSubject(config.subjectName);
                                if (existingCount >= QUESTIONS_PER_SUBJECT) {
                                        continue;
                                }

                                List<Question> generated = createQuestionsForConfig(config, existingCount);
                                if (generated.isEmpty()) {
                                        continue;
                                }

                                db.questionDao().insertAll(generated);
                        }

                        if (onComplete != null) {
                                mainHandler.post(onComplete);
                        }
                }).start();
        }

        private void ensureSubjectExists(String name) {
                if (db.subjectDao().getSubjectByName(name) == null) {
                        db.subjectDao().insert(new Subject(name));
                }
        }

        private List<Question> createQuestionsForConfig(SubjectConfig config, int existingCount) {
                int needed = QUESTIONS_PER_SUBJECT - existingCount;
                List<Question> questions = new ArrayList<>();
                if (needed <= 0) {
                        return questions;
                }

                for (int i = 0; i < needed; i++) {
                        int variantIndex = existingCount + i + 1;
                        String topic = config.topics[(variantIndex - 1) % config.topics.length];
                        questions.add(createQuestionFromTemplate(config, topic, variantIndex));
                }
                return questions;
        }

        private Question createQuestionFromTemplate(SubjectConfig config, String topic, int variantIndex) {
                String questionText = String.format(
                                "%s focus on %s: Outline an explanation relevant for Grade %d (variation %d).",
                                config.subjectName, topic, config.grade, variantIndex);

                switch (config.type) {
                        case MULTIPLE_CHOICE:
                                return createAutoMcq(config, topic, questionText, variantIndex);
                        case TRUE_FALSE:
                                return createTrueFalse(config, topic, questionText, variantIndex);
                        case MATCH_COLUMNS:
                                return createMatchColumns(config, topic, questionText, variantIndex);
                        default:
                                return createFillInOrEssay(config, questionText, topic, variantIndex);
                }
        }

        private Question createFillInOrEssay(SubjectConfig config, String questionText, String topic,
                        int variantIndex) {
                Question q = baseQuestion(config, questionText, topic);
                Map<String, String> content = new HashMap<>();
                content.put("answer", String.format("Sample response for %s about %s (%d)", config.subjectName, topic,
                                variantIndex));
                q.setContent(content);
                q.setType(config.type == QuestionType.ESSAY_SOURCE_BASED ? QuestionType.ESSAY_SOURCE_BASED
                                : QuestionType.FILL_IN_BLANKS);
                return q;
        }

        private Question createTrueFalse(SubjectConfig config, String topic, String questionText, int variantIndex) {
                Question q = baseQuestion(config, questionText, topic);
                q.setType(QuestionType.TRUE_FALSE);
                Map<String, String> content = new HashMap<>();
                boolean answer = variantIndex % 2 == 0;
                content.put("answer", String.valueOf(answer));
                q.setContent(content);
                return q;
        }

        private Question createMatchColumns(SubjectConfig config, String topic, String questionText, int variantIndex) {
                Question q = baseQuestion(config, questionText, topic);
                q.setType(QuestionType.MATCH_COLUMNS);
                Map<String, String> content = new HashMap<>();

                List<String> columnA = new ArrayList<>();
                List<String> columnB = new ArrayList<>();
                Map<String, String> mapping = new HashMap<>();
                for (int i = 0; i < 3; i++) {
                        String left = topic + " concept " + (char) ('A' + i);
                        String right = "Definition " + (variantIndex + i);
                        columnA.add(left);
                        columnB.add(right);
                        mapping.put(left, right);
                }
                content.put("columnA", gson.toJson(columnA));
                content.put("columnB", gson.toJson(columnB));
                content.put("mapping", gson.toJson(mapping));
                q.setContent(content);
                return q;
        }

        private Question createAutoMcq(SubjectConfig config, String topic, String questionText, int variantIndex) {
                Question q = baseQuestion(config, questionText, topic);
                q.setType(QuestionType.MULTIPLE_CHOICE);
                List<String> options = new ArrayList<>();
                options.add("Key fact about " + topic);
                options.add("Common misconception about " + topic);
                options.add("Real-world link for " + topic);
                options.add("Classroom example for " + topic);
                Map<String, String> content = new HashMap<>();
                content.put("options", gson.toJson(options));
                String answerLetter = String.valueOf((char) ('A' + (variantIndex % options.size())));
                content.put("answer", answerLetter);
                q.setContent(content);
                return q;
        }

        private Question baseQuestion(SubjectConfig config, String questionText, String topic) {
                Question q = new Question();
                q.setId(java.util.UUID.randomUUID().toString());
                q.setSubject(config.subjectName);
                q.setGrade(config.grade);
                q.setTopic(topic);
                q.setQuestionText(questionText);
                q.setType(config.type);
                q.setMarks(config.marks);
                q.setDifficulty(config.difficulty);

                // Random CAPS cognitive level for variation
                com.smartexam.models.CognitiveLevel[] levels = com.smartexam.models.CognitiveLevel.values();
                q.setCognitiveLevel(levels[(int) (Math.random() * levels.length)]);

                q.setContent(new HashMap<>());
                q.setCreatedAt(System.currentTimeMillis());
                q.setVersion(1);
                return q;
        }

        private List<SubjectConfig> buildCapsSubjectConfigs() {
                List<SubjectConfig> configs = new ArrayList<>();

                // Foundation Phase (Grades R-3)
                configs.add(new SubjectConfig("Home Language (Grades R-3)", 2, QuestionType.FILL_IN_BLANKS,
                                "Easy", 2, new String[] { "Listening & Speaking", "Reading & Phonics", "Writing",
                                                "Storytelling" }));
                configs.add(new SubjectConfig("First Additional Language (Grades R-3)", 2, QuestionType.FILL_IN_BLANKS,
                                "Easy", 2,
                                new String[] { "Greetings", "Vocabulary", "Simple sentences", "Reading aloud" }));
                configs.add(new SubjectConfig("Mathematics (Grades R-3)", 2, QuestionType.MULTIPLE_CHOICE,
                                "Easy", 2, new String[] { "Number sense", "Patterns", "Measurement", "Shapes" }));
                configs.add(new SubjectConfig("Life Skills (Grades R-3)", 2, QuestionType.FILL_IN_BLANKS,
                                "Easy", 2, new String[] { "Beginning Knowledge", "Creative Arts", "Physical Education",
                                                "Personal & Social Well-being" }));

                // Intermediate Phase (Grades 4-6)
                configs.add(new SubjectConfig("Home Language (Grades 4-6)", 5, QuestionType.FILL_IN_BLANKS,
                                "Medium", 3,
                                new String[] { "Reading strategies", "Writing process", "Language structures" }));
                configs.add(new SubjectConfig("First Additional Language (Grades 4-6)", 5, QuestionType.FILL_IN_BLANKS,
                                "Medium", 3, new String[] { "Comprehension", "Dialogue", "Grammar", "Listening" }));
                configs.add(new SubjectConfig("Mathematics (Grades 4-6)", 5, QuestionType.MULTIPLE_CHOICE,
                                "Medium", 3, new String[] { "Fractions", "Decimals", "Measurement", "Geometry" }));
                configs.add(new SubjectConfig("Natural Sciences & Technology", 5, QuestionType.MULTIPLE_CHOICE,
                                "Medium", 3, new String[] { "Matter", "Energy", "Life processes", "Systems" }));
                configs.add(new SubjectConfig("Social Sciences (Grades 4-6)", 5, QuestionType.MATCH_COLUMNS,
                                "Medium", 3,
                                new String[] { "History timelines", "Geography maps", "Heritage", "Resources" }));
                configs.add(new SubjectConfig("Life Skills (Grades 4-6)", 5, QuestionType.FILL_IN_BLANKS,
                                "Medium", 3,
                                new String[] { "Physical Education", "Creative Arts", "Health", "Citizenship" }));

                // Senior Phase (Grade 7)
                configs.add(new SubjectConfig("Home Language (Grade 7)", 7, QuestionType.FILL_IN_BLANKS,
                                "Medium", 4,
                                new String[] { "Literature study", "Essay writing", "Language conventions" }));
                configs.add(new SubjectConfig("First Additional Language (Grade 7)", 7, QuestionType.FILL_IN_BLANKS,
                                "Medium", 4, new String[] { "Prepared speech", "Comprehension", "Listening skills" }));
                configs.add(new SubjectConfig("Mathematics (Grade 7)", 7, QuestionType.MULTIPLE_CHOICE,
                                "Medium", 4, new String[] { "Algebra", "Integers", "Probability", "Geometry" }));
                configs.add(new SubjectConfig("Natural Sciences (Grade 7)", 7, QuestionType.MULTIPLE_CHOICE,
                                "Medium", 4,
                                new String[] { "Photosynthesis", "States of matter", "Electric circuits" }));
                configs.add(new SubjectConfig("Social Sciences History (Grade 7)", 7, QuestionType.ESSAY_SOURCE_BASED,
                                "Medium", 4,
                                new String[] { "Kingdoms of Africa", "Colonialism", "Industrial Revolution" }));
                configs.add(new SubjectConfig("Social Sciences Geography (Grade 7)", 7, QuestionType.MATCH_COLUMNS,
                                "Medium", 4, new String[] { "Map skills", "Weather", "Settlement", "Resources" }));
                configs.add(new SubjectConfig("Technology (Grade 7)", 7, QuestionType.FILL_IN_BLANKS,
                                "Medium", 4, new String[] { "Design process", "Structures", "Systems" }));
                configs.add(new SubjectConfig("Creative Arts (Grade 7)", 7, QuestionType.FILL_IN_BLANKS,
                                "Medium", 4, new String[] { "Drama", "Music", "Visual art", "Dance" }));
                configs.add(new SubjectConfig("Economic and Management Sciences (Grade 7)", 7, QuestionType.TRUE_FALSE,
                                "Medium", 4, new String[] { "Needs vs wants", "Budgeting", "Entrepreneurship" }));
                configs.add(new SubjectConfig("Life Orientation (Grade 7)", 7, QuestionType.FILL_IN_BLANKS,
                                "Medium", 4,
                                new String[] { "Goal setting", "Health", "Relationships", "Citizenship" }));

                // FET Phase (Grades 10-12)
                configs.add(new SubjectConfig("English Home Language", 11, QuestionType.ESSAY_SOURCE_BASED,
                                "Hard", 5, new String[] { "Poetry analysis", "Novel study", "Transactional writing" }));
                configs.add(new SubjectConfig("Afrikaans First Additional Language", 11, QuestionType.FILL_IN_BLANKS,
                                "Hard", 5, new String[] { "Leesbegrip", "Skryfwerk", "Taalstrukture" }));
                configs.add(new SubjectConfig("isiZulu First Additional Language", 11, QuestionType.FILL_IN_BLANKS,
                                "Hard", 5, new String[] { "Ukufunda", "Isizulu grammar", "Inkulumo" }));
                configs.add(new SubjectConfig("Mathematics", 11, QuestionType.MULTIPLE_CHOICE,
                                "Hard", 5, new String[] { "Functions", "Trigonometry", "Probability", "Calculus" }));
                configs.add(new SubjectConfig("Mathematical Literacy", 11, QuestionType.MULTIPLE_CHOICE,
                                "Medium", 5,
                                new String[] { "Financial maths", "Measurement", "Data handling", "Maps" }));
                configs.add(new SubjectConfig("Life Sciences", 11, QuestionType.MULTIPLE_CHOICE,
                                "Hard", 5, new String[] { "DNA & Genetics", "Human systems", "Ecology", "Evolution" }));
                configs.add(new SubjectConfig("Physical Sciences", 11, QuestionType.MULTIPLE_CHOICE,
                                "Hard", 5, new String[] { "Mechanics", "Electricity", "Chemistry", "Waves" }));
                configs.add(new SubjectConfig("History", 11, QuestionType.ESSAY_SOURCE_BASED,
                                "Hard", 5, new String[] { "Apartheid", "World Wars", "Cold War", "Pan-Africanism" }));
                configs.add(new SubjectConfig("Geography", 11, QuestionType.MATCH_COLUMNS,
                                "Hard", 5, new String[] { "Geomorphology", "Climatology", "GIS", "Development" }));
                configs.add(new SubjectConfig("Accounting", 11, QuestionType.FILL_IN_BLANKS,
                                "Hard", 5,
                                new String[] { "Ledger accounts", "Financial statements", "VAT", "Costing" }));
                configs.add(new SubjectConfig("Business Studies", 11, QuestionType.TRUE_FALSE,
                                "Medium", 5,
                                new String[] { "Business functions", "CSR", "Legislation", "Entrepreneurship" }));
                configs.add(new SubjectConfig("Economics", 11, QuestionType.FILL_IN_BLANKS,
                                "Hard", 5,
                                new String[] { "Microeconomics", "Macroeconomics", "Market structures", "Trade" }));
                configs.add(new SubjectConfig("Computer Applications Technology", 11, QuestionType.MATCH_COLUMNS,
                                "Medium", 5,
                                new String[] { "Word processing", "Spreadsheets", "Databases", "Networks" }));
                configs.add(new SubjectConfig("Tourism", 11, QuestionType.FILL_IN_BLANKS,
                                "Medium", 5, new String[] { "Attractions", "Customer service", "Sustainable travel",
                                                "Travel docs" }));
                configs.add(new SubjectConfig("Consumer Studies", 11, QuestionType.FILL_IN_BLANKS,
                                "Medium", 5,
                                new String[] { "Nutrition", "Consumer rights", "Fabric care", "Entrepreneurship" }));
                configs.add(new SubjectConfig("Life Orientation (FET)", 11, QuestionType.FILL_IN_BLANKS,
                                "Medium", 5,
                                new String[] { "Career choices", "Health & wellness", "Social responsibility" }));

                return configs;
        }

        private static class SubjectConfig {
                final String subjectName;
                final int grade;
                final QuestionType type;
                final String difficulty;
                final int marks;
                final String[] topics;

                SubjectConfig(String subjectName, int grade, QuestionType type,
                                String difficulty, int marks, String[] topics) {
                        this.subjectName = subjectName;
                        this.grade = grade;
                        this.type = type;
                        this.difficulty = difficulty;
                        this.marks = marks;
                        this.topics = topics;
                }
        }
}
