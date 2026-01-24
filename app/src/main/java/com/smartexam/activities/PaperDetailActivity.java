package com.smartexam.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.PageRange;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.smartexam.R;
import com.smartexam.adapters.PaperQuestionAdapter;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.AssessmentPaper;
import com.smartexam.models.Question;
import com.smartexam.models.Subject;
import com.smartexam.utils.DbUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaperDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PAPER_ID = "EXTRA_PAPER_ID";

    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView tvPaperTitle;
    private TextView tvPaperMeta;
    private TextView tvPaperDate;
    private RecyclerView rvPaperQuestions;
    private MaterialButton btnSavePdf;
    private MaterialButton btnPrintPdf;
    private MaterialButton btnSharePdf;
    private PaperQuestionAdapter questionAdapter;

    private AssessmentPaper assessmentPaper;
    private Subject subject;
    private List<Question> questions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paper_detail);

        String paperId = getIntent().getStringExtra(EXTRA_PAPER_ID);
        if (TextUtils.isEmpty(paperId)) {
            Toast.makeText(this, "Missing paper details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = AppDatabase.getInstance(this);
        initViews();
        loadPaperData(paperId);
    }

    private void initViews() {
        tvPaperTitle = findViewById(R.id.tvPaperTitle);
        tvPaperMeta = findViewById(R.id.tvPaperMeta);
        tvPaperDate = findViewById(R.id.tvPaperDate);
        rvPaperQuestions = findViewById(R.id.rvPaperQuestions);
        btnSavePdf = findViewById(R.id.btnSavePdf);
        btnPrintPdf = findViewById(R.id.btnPrintPdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);

        questionAdapter = new PaperQuestionAdapter();
        rvPaperQuestions.setLayoutManager(new LinearLayoutManager(this));
        rvPaperQuestions.setAdapter(questionAdapter);

        btnSavePdf.setOnClickListener(v -> savePdfToDownloads());
        btnPrintPdf.setOnClickListener(v -> printPdf());
        btnSharePdf.setOnClickListener(v -> sharePdf());
    }

    private void loadPaperData(String paperId) {
        executor.execute(() -> {
            AssessmentPaper paper = db.paperDao().getPaperById(paperId);
            if (paper == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Paper not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            List<Question> questionList = db.paperDao().getQuestionsForPaper(paperId);

            Subject fetchedSubject = db.subjectDao().getSubjectById(paper.getSubjectId());

            assessmentPaper = paper;
            questions = questionList;
            subject = fetchedSubject;

            runOnUiThread(this::renderPaperDetails);
        });
    }

    private void renderPaperDetails() {
        if (assessmentPaper == null) {
            return;
        }

        tvPaperTitle.setText(assessmentPaper.getTitle());
        String subjectName = subject != null ? subject.getName() : "Subject";
        tvPaperMeta
                .setText(String.format(Locale.getDefault(), "Grade %d • %s", assessmentPaper.getGrade(), subjectName));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        tvPaperDate.setText(String.format(Locale.getDefault(), "Generated on %s",
                sdf.format(new Date(assessmentPaper.getCreatedAt()))));

        questionAdapter.submitList(questions);
    }

    private File getPaperFile() {
        if (assessmentPaper == null || TextUtils.isEmpty(assessmentPaper.getFilePath())) {
            Toast.makeText(this, "PDF path missing", Toast.LENGTH_SHORT).show();
            return null;
        }
        File file = new File(assessmentPaper.getFilePath());
        if (!file.exists()) {
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
            return null;
        }
        return file;
    }

    private void savePdfToDownloads() {
        File source = getPaperFile();
        if (source == null) {
            return;
        }

        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File destDir = new File(downloads, "SmartExam");
        if (!destDir.exists() && !destDir.mkdirs()) {
            Toast.makeText(this, "Unable to access Downloads", Toast.LENGTH_SHORT).show();
            return;
        }

        File destination = new File(destDir, source.getName());
        try (FileChannel inChannel = new FileInputStream(source).getChannel();
                FileChannel outChannel = new FileOutputStream(destination).getChannel()) {
            outChannel.transferFrom(inChannel, 0, inChannel.size());
            Toast.makeText(this, "Saved to Downloads/SmartExam", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf() {
        File pdf = getPaperFile();
        if (pdf == null) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdf);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share paper"));
    }

    private void printPdf() {
        File pdf = getPaperFile();
        if (pdf == null) {
            return;
        }

        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, "Print service unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        String jobName = assessmentPaper != null ? assessmentPaper.getTitle() : "SmartExam Paper";
        printManager.print(jobName, new PdfFilePrintAdapter(this, pdf), null);
    }

    private static class PdfFilePrintAdapter extends PrintDocumentAdapter {
        private final Context context;
        private final File file;

        PdfFilePrintAdapter(Context context, File file) {
            this.context = context;
            this.file = file;
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
            if (cancellationSignal != null && cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }

            PrintDocumentInfo info = new PrintDocumentInfo.Builder(file.getName())
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build();
            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                CancellationSignal cancellationSignal, WriteResultCallback callback) {
            try (InputStream inputStream = new FileInputStream(file);
                    FileOutputStream outputStream = new FileOutputStream(destination.getFileDescriptor())) {
                byte[] buffer = new byte[8192];
                int size;
                while ((size = inputStream.read(buffer)) > 0) {
                    if (cancellationSignal != null && cancellationSignal.isCanceled()) {
                        callback.onWriteCancelled();
                        return;
                    }
                    outputStream.write(buffer, 0, size);
                }
                callback.onWriteFinished(new PageRange[] { PageRange.ALL_PAGES });
            } catch (IOException e) {
                callback.onWriteFailed(e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
