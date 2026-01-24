package com.smartexam.logic;

import com.smartexam.models.Question;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestBuilder {

    /**
     * Selects questions randomly from a pool to match a target mark total.
     * 
     * @param pool The list of available questions.
     * @param targetMarks The desired total marks for the test.
     * @return A list of selected questions.
     */
    public List<Question> buildTest(List<Question> pool, int targetMarks) {
        List<Question> selected = new ArrayList<>();
        int currentMarks = 0;

        // Shuffle the pool for randomness
        List<Question> shuffledPool = new ArrayList<>(pool);
        Collections.shuffle(shuffledPool);

        for (Question q : shuffledPool) {
            if (currentMarks + q.getMarks() <= targetMarks) {
                selected.add(q);
                currentMarks += q.getMarks();
            }

            if (currentMarks == targetMarks) {
                break;
            }
        }

        return selected;
    }

    /**
     * Calculates the total marks of a given set of questions.
     */
    public int calculateTotalMarks(List<Question> questions) {
        return questions.stream()
                .mapToInt(Question::getMarks)
                .sum();
    }
}
