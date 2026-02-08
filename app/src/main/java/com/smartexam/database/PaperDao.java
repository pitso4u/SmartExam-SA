package com.smartexam.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.smartexam.models.AssessmentPaper;
import com.smartexam.models.PaperQuestion;
import com.smartexam.models.Question;
import java.util.List;

@Dao
public interface PaperDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AssessmentPaper paper);

    @Update
    void update(AssessmentPaper paper);

    @Query("DELETE FROM assessment_papers WHERE id = :paperId")
    void deleteById(String paperId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPaperQuestions(List<PaperQuestion> paperQuestions);

    @Query("DELETE FROM paper_questions WHERE paperId = :paperId")
    void clearPaperQuestions(String paperId);

    @Query("SELECT * FROM assessment_papers WHERE id = :paperId")
    AssessmentPaper getPaperById(String paperId);

    @Transaction
    @androidx.room.RewriteQueriesToDropUnusedColumns
    @Query("SELECT q.* FROM questions AS q JOIN paper_questions AS pq ON q.id = pq.questionId WHERE pq.paperId = :paperId ORDER BY pq.questionOrder")
    List<Question> getQuestionsForPaper(String paperId);

    @Query("SELECT COUNT(*) FROM assessment_papers")
    int getPaperCount();

    @Query("SELECT * FROM assessment_papers ORDER BY createdAt DESC LIMIT :limit")
    List<AssessmentPaper> getRecentPapers(int limit);

    @Query("SELECT * FROM assessment_papers ORDER BY createdAt DESC")
    List<AssessmentPaper> getAllPapers();
}
