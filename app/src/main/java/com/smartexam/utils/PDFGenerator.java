package com.smartexam.utils;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.smartexam.models.Question;
import com.smartexam.models.QuestionType;
import com.smartexam.subscription.SubscriptionManager;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PDFGenerator {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL);
    private static final Font FOOTER_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

    private static final String DEFAULT_SCHOOL_NAME = "SmartExam Academy";

    private String schoolName = DEFAULT_SCHOOL_NAME;
    private String teacherName = "";
    private String schoolLogoPath = null;

    public void configureSchoolDetails(String schoolName, String teacherName, String schoolLogoPath) {
        this.schoolName = isNullOrEmpty(schoolName) ? DEFAULT_SCHOOL_NAME : schoolName.trim();
        this.teacherName = isNullOrEmpty(teacherName) ? "" : teacherName.trim();
        this.schoolLogoPath = !isNullOrEmpty(schoolLogoPath) ? schoolLogoPath.trim() : null;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = isNullOrEmpty(schoolName) ? DEFAULT_SCHOOL_NAME : schoolName.trim();
    }

    public void generateTest(String filePath, String title, String subject, int grade, List<Question> questions, byte[] logoBytes)
            throws DocumentException, IOException {

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        CompanyLogoEvent event = new CompanyLogoEvent(logoBytes);
        writer.setPageEvent(event);
        document.open();

        addHeader(document, title, subject, grade, questions);
        addQuestions(document, questions, false);

        document.close();
    }

    public void generateMemo(String filePath, String testTitle, List<Question> questions, byte[] logoBytes)
            throws DocumentException, IOException {
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        CompanyLogoEvent event = new CompanyLogoEvent(logoBytes);
        writer.setPageEvent(event);
        document.open();

        Paragraph pTitle = new Paragraph("MARKING GUIDELINE: " + testTitle.toUpperCase(), TITLE_FONT);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(pTitle);
        document.add(new Paragraph(" "));

        addQuestions(document, questions, true);

        document.close();
    }

    /**
     * Check if watermark should be applied based on subscription status
     */
    private boolean shouldApplyWatermark() {
        try {
            SubscriptionManager subscriptionManager = SubscriptionManager.getInstance();
            return !subscriptionManager.canPrintClean();
        } catch (Exception e) {
            // If subscription check fails, apply watermark for safety
            return true;
        }
    }

    private void addHeader(Document document, String title, String subject, int grade, List<Question> questions)
            throws DocumentException {
        Image logo = loadLogoImage();
        if (logo != null) {
            logo.scaleToFit(80, 80);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
            document.add(new Paragraph(" "));
        }

        Paragraph pSchool = new Paragraph(schoolName, TITLE_FONT);
        pSchool.setAlignment(Element.ALIGN_CENTER);
        document.add(pSchool);

        if (!isNullOrEmpty(teacherName)) {
            Paragraph pTeacher = new Paragraph("Teacher: " + teacherName, NORMAL_FONT);
            pTeacher.setAlignment(Element.ALIGN_CENTER);
            document.add(pTeacher);
        }

        Paragraph pTitle = new Paragraph(title.toUpperCase(), HEADER_FONT);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(pTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        table.addCell(createCell("Subject: " + subject, Element.ALIGN_LEFT));
        table.addCell(createCell("Grade: " + grade, Element.ALIGN_RIGHT));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        table.addCell(createCell("Date: " + sdf.format(new Date()), Element.ALIGN_LEFT));
        table.addCell(createCell("Total Marks: " + calculateTotal(questions), Element.ALIGN_RIGHT));

        document.add(table);
        document.add(new Paragraph("______________________________________________________________________________"));
        document.add(new Paragraph(" "));
    }

    private Image loadLogoImage() {
        if (isNullOrEmpty(schoolLogoPath)) {
            return null;
        }
        try {
            File file = new File(schoolLogoPath);
            if (!file.exists()) {
                return null;
            }
            Image logo = Image.getInstance(schoolLogoPath);
            logo.setBorder(Image.NO_BORDER);
            return logo;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private PdfPCell createCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private void addQuestions(Document document, List<Question> questions, boolean isMemo) throws DocumentException {
        int counter = 1;
        for (Question q : questions) {
            Paragraph qHeader = new Paragraph("QUESTION " + counter + " [" + q.getMarks() + " Marks]", HEADER_FONT);
            qHeader.setSpacingBefore(10);
            document.add(qHeader);

            document.add(new Paragraph(q.getQuestionText(), NORMAL_FONT));

            if (isMemo) {
                String answer = q.getContent() != null ? q.getContent().get("answer") : "N/A";
                document.add(new Paragraph("Answer: " + answer, HEADER_FONT));
            } else {
                renderQuestionSpace(document, q);
            }

            document.add(new Paragraph(" "));
            counter++;
        }
    }

    private void renderQuestionSpace(Document document, Question q) throws DocumentException {
        QuestionType type = q.getType();
        Map<String, String> content = q.getContent();

        if (type == QuestionType.MULTIPLE_CHOICE && content != null) {
            char optionChar = 'A';
            for (int i = 1; i <= 4; i++) {
                String option = content.get("option" + i);
                if (option != null) {
                    Paragraph p = new Paragraph(optionChar + ". " + option, NORMAL_FONT);
                    p.setIndentationLeft(20);
                    document.add(p);
                    optionChar++;
                }
            }
        } else if (type == QuestionType.TRUE_FALSE) {
            document.add(new Paragraph("Answer: [ True / False ]", NORMAL_FONT));
        } else if (type == QuestionType.ESSAY_SOURCE_BASED) {
            for (int i = 0; i < 4; i++) {
                document.add(new Paragraph(
                        "______________________________________________________________________________"));
            }
        } else {
            document.add(
                    new Paragraph("______________________________________________________________________________"));
        }
    }

    private int calculateTotal(List<Question> questions) {
        return questions.stream().mapToInt(Question::getMarks).sum();
    }

    /**
     * Page event handler to add company logo to bottom right corner
     */
    private static class CompanyLogoEvent extends PdfPageEventHelper {
        private static final float LOGO_WIDTH = 50f;
        private static final float LOGO_HEIGHT = 50f;
        private static final float MARGIN_RIGHT = 20f;
        private static final float MARGIN_BOTTOM = 20f;

        private byte[] logoBytes;

        public CompanyLogoEvent(byte[] logoBytes) {
            this.logoBytes = logoBytes;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                Image logo = loadCompanyLogo();
                if (logo != null) {
                    logo.scaleToFit(LOGO_WIDTH, LOGO_HEIGHT);
                    logo.setAbsolutePosition(
                        document.right() - LOGO_WIDTH - MARGIN_RIGHT,
                        document.bottom() + MARGIN_BOTTOM
                    );
                    PdfContentByte canvas = writer.getDirectContent();
                    canvas.addImage(logo);
                }
            } catch (Exception e) {
                // Silently fail if logo can't be added
            }
        }

        private Image loadCompanyLogo() {
            if (logoBytes == null || logoBytes.length == 0) {
                return null;
            }
            try {
                return Image.getInstance(logoBytes);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
