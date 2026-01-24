package com.smartexam.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "paper_questions", primaryKeys = { "paperId", "questionId" }, foreignKeys = {
        @ForeignKey(entity = AssessmentPaper.class, parentColumns = "id", childColumns = "paperId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Question.class, parentColumns = "id", childColumns = "questionId", onDelete = ForeignKey.CASCADE)
}, indices = { @Index("paperId"), @Index("questionId") })
public class PaperQuestion {
    @NonNull
    private String paperId;
    @NonNull
    private String questionId;
    private int questionOrder;

    public PaperQuestion(@NonNull String paperId, @NonNull String questionId, int questionOrder) {
        this.paperId = paperId;
        this.questionId = questionId;
        this.questionOrder = questionOrder;
    }

    @NonNull
    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(@NonNull String paperId) {
        this.paperId = paperId;
    }

    @NonNull
    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(@NonNull String questionId) {
        this.questionId = questionId;
    }

    public int getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(int questionOrder) {
        this.questionOrder = questionOrder;
    }
}
