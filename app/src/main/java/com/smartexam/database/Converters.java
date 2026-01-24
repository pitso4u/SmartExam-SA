package com.smartexam.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartexam.models.CognitiveLevel;
import com.smartexam.models.QuestionType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Converters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromMap(Map<String, String> value) {
        return value == null ? null : gson.toJson(value);
    }

    @TypeConverter
    public static Map<String, String> toMap(String value) {
        if (value == null)
            return null;
        Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        return gson.fromJson(value, mapType);
    }

    @TypeConverter
    public static String fromQuestionType(QuestionType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static QuestionType toQuestionType(String value) {
        if (value == null)
            return null;
        try {
            return QuestionType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @TypeConverter
    public static String fromCognitiveLevel(CognitiveLevel level) {
        return level == null ? null : level.name();
    }

    @TypeConverter
    public static CognitiveLevel toCognitiveLevel(String value) {
        if (value == null)
            return null;
        try {
            return CognitiveLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @TypeConverter
    public static String fromStringList(List<String> value) {
        return value == null ? null : gson.toJson(value);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null)
            return null;
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        return gson.fromJson(value, listType);
    }
}
