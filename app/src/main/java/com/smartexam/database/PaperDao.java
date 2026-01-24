package com.smartexam.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.smartexam.models.AssessmentPaper;
import com.smartexam.models.PaperQuestion;
import com.smartexam.models.Question;
import java.util.List;

@Dao
public interface PaperDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AssessmentPaper paper);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPaperQuestions(List<PaperQuestion> paperQuestions);

    @Transaction
    @androidx.room.RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM questions INNER JOIN paper_questions ON questions.id = paper_questions.questionId WHERE paper_questions.paperId = :paperId ORDER BY paper_questions.questionOrder")
    List<Question> getQuestionsForPaper(String paperId);

    @Query("SELECT * FROM assessment_papers WHERE id = :id")
    AssessmentPaper getPaperById(String id);

    @Query("DELETE FROM assessment_papers WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM assessment_papers ORDER BY createdAt DESC")
    List<AssessmentPaper> getAllPapers();

    @Query("SELECT * FROM assessment_papers ORDER BY createdAt DESC LIMIT :limit")
    List<AssessmentPaper> getRecentPapers(int limit);

    @Query("SELECT COUNT(*) FROM assessment_papers")
    int getPaperCount();
}
