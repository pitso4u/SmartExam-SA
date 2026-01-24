package com.smartexam.utils;

import com.smartexam.models.Question;
import java.util.UUID;

/**
 * Utility class for database-related operations and model manipulations.
 */
public class DbUtils {

    /**
     * Ensures a question has a unique ID and timestamp if missing.
     */
    public static void ensureIdAndTimestamp(Question q) {
        if (q.getId() == null || q.getId().isEmpty()) {
            q.setId(UUID.randomUUID().toString());
        }
        if (q.getCreatedAt() <= 0) {
            q.setCreatedAt(System.currentTimeMillis());
        }
    }
}
