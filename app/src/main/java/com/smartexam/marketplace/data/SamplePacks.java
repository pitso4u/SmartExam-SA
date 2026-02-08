package com.smartexam.marketplace.data;

import com.smartexam.models.QuestionPack;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample question packs for the marketplace
 */
public class SamplePacks {
    
    public static List<QuestionPack> getSamplePacks() {
        List<QuestionPack> packs = new ArrayList<>();
        
        // Mathematics Pack
        QuestionPack mathPack = new QuestionPack();
        mathPack.setId("math_grade10_2024");
        mathPack.setTitle("Grade 10 Mathematics");
        mathPack.setDescription("Comprehensive mathematics questions covering algebra, geometry, and statistics for Grade 10 students.");
        mathPack.setSubject("Mathematics");
        mathPack.setGrade(10);
        mathPack.setQuestionCount(150);
        mathPack.setPriceCents(4999); // 49.99 * 100
        mathPack.setTerm(1);
        mathPack.setTotalMarks(150);
        mathPack.setPublished(true);
        mathPack.setCreatedAt(System.currentTimeMillis());
        mathPack.setPurchased(false);
        packs.add(mathPack);
        
        // Physical Sciences Pack
        QuestionPack sciencePack = new QuestionPack();
        sciencePack.setId("physics_grade11_2024");
        sciencePack.setTitle("Grade 11 Physical Sciences");
        sciencePack.setDescription("Physics and chemistry questions designed for Grade 11 curriculum with practical applications.");
        sciencePack.setSubject("Physical Sciences");
        sciencePack.setGrade(11);
        sciencePack.setQuestionCount(200);
        sciencePack.setPriceCents(5999); // 59.99 * 100
        sciencePack.setTerm(1);
        sciencePack.setTotalMarks(200);
        sciencePack.setPublished(true);
        sciencePack.setCreatedAt(System.currentTimeMillis());
        sciencePack.setPurchased(false);
        packs.add(sciencePack);
        
        // English Language Pack
        QuestionPack englishPack = new QuestionPack();
        englishPack.setId("english_grade12_2024");
        englishPack.setTitle("Grade 12 English Language");
        englishPack.setDescription("Advanced English language questions covering comprehension, grammar, and literature analysis.");
        englishPack.setSubject("English");
        englishPack.setGrade(12);
        englishPack.setQuestionCount(120);
        englishPack.setPriceCents(3999); // 39.99 * 100
        englishPack.setTerm(1);
        englishPack.setTotalMarks(120);
        englishPack.setPublished(true);
        englishPack.setCreatedAt(System.currentTimeMillis());
        englishPack.setPurchased(false);
        packs.add(englishPack);
        
        // Life Sciences Pack
        QuestionPack lifeSciencePack = new QuestionPack();
        lifeSciencePack.setId("life_science_grade10_2024");
        lifeSciencePack.setTitle("Grade 10 Life Sciences");
        lifeSciencePack.setDescription("Biology and life sciences questions covering cells, genetics, and human anatomy.");
        lifeSciencePack.setSubject("Life Sciences");
        lifeSciencePack.setGrade(10);
        lifeSciencePack.setQuestionCount(180);
        lifeSciencePack.setPriceCents(4499); // 44.99 * 100
        lifeSciencePack.setTerm(1);
        lifeSciencePack.setTotalMarks(180);
        lifeSciencePack.setPublished(true);
        lifeSciencePack.setCreatedAt(System.currentTimeMillis());
        lifeSciencePack.setPurchased(false);
        packs.add(lifeSciencePack);
        
        // Accounting Pack
        QuestionPack accountingPack = new QuestionPack();
        accountingPack.setId("accounting_grade12_2024");
        accountingPack.setTitle("Grade 12 Accounting");
        accountingPack.setDescription("Comprehensive accounting questions covering financial statements, partnerships, and companies.");
        accountingPack.setSubject("Accounting");
        accountingPack.setGrade(12);
        accountingPack.setQuestionCount(160);
        accountingPack.setPriceCents(5499); // 54.99 * 100
        accountingPack.setTerm(1);
        accountingPack.setTotalMarks(160);
        accountingPack.setPublished(true);
        accountingPack.setCreatedAt(System.currentTimeMillis());
        accountingPack.setPurchased(false);
        packs.add(accountingPack);
        
        // Business Studies Pack
        QuestionPack businessPack = new QuestionPack();
        businessPack.setId("business_grade11_2024");
        businessPack.setTitle("Grade 11 Business Studies");
        businessPack.setDescription("Business management, entrepreneurship, and marketing questions for Grade 11 students.");
        businessPack.setSubject("Business Studies");
        businessPack.setGrade(11);
        businessPack.setQuestionCount(140);
        businessPack.setPriceCents(4299); // 42.99 * 100
        businessPack.setTerm(1);
        businessPack.setTotalMarks(140);
        businessPack.setPublished(true);
        businessPack.setCreatedAt(System.currentTimeMillis());
        businessPack.setPurchased(false);
        packs.add(businessPack);
        
        return packs;
    }
    
    /**
     * Get a specific pack by ID
     */
    public static QuestionPack getPackById(String packId) {
        for (QuestionPack pack : getSamplePacks()) {
            if (pack.getId().equals(packId)) {
                return pack;
            }
        }
        return null;
    }
}
