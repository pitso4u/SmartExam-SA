package com.smartexam.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.smartexam.database.Converters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity(tableName = "questions")
@TypeConverters({ Converters.class })
public class Question {
    @PrimaryKey
    @NonNull
    private String id;
    private String subject;
    private int grade;
    private String topic;
    private String capsTopicId;
    private QuestionType type;
    private CognitiveLevel cognitiveLevel;
    private int marks;
    private String difficulty;
    private String questionText;
    private Map<String, String> content; // JSON blob for flexible structures
    private List<String> tags;
    private String imagePath; // Reference to local or cloud storage
    private String packId; // Reference to the question pack this belongs to
    private int version;
    private boolean isFromMarketplace;
    private long createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getCapsTopicId() {
        return capsTopicId;
    }

    public void setCapsTopicId(String capsTopicId) {
        this.capsTopicId = capsTopicId;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public CognitiveLevel getCognitiveLevel() {
        return cognitiveLevel;
    }

    public void setCognitiveLevel(CognitiveLevel cognitiveLevel) {
        this.cognitiveLevel = cognitiveLevel;
    }

    public int getMarks() {
        return marks;
    }

    public void setMarks(int marks) {
        this.marks = marks;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Map<String, String> getContent() {
        return content;
    }

    public void setContent(Map<String, String> content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getPackId() {
        return packId;
    }

    public void setPackId(String packId) {
        this.packId = packId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isFromMarketplace() {
        return isFromMarketplace;
    }

    public void setFromMarketplace(boolean fromMarketplace) {
        isFromMarketplace = fromMarketplace;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Helper methods for specific CAPS types
     */

    // MCQ Example structure
    public static class MultipleChoiceContent {
        public List<String> options;
        public String answer; // A, B, C, or D
    }

    // True/False with correction
    public static class TrueFalseContent {
        public boolean isTrue;
        public String correction; // Expected if isTrue is false
    }

    // Match Columns
    public static class MatchColumnsContent {
        public List<String> columnA;
        public List<String> columnB;
        public Map<String, String> correctMapping;
    }

    // Choose from Table
    public static class TableContent {
        public List<List<String>> tableData;
        public String subQuestion;
        public String answer;
    }
}
