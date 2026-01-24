package com.smartexam.marketplace.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.smartexam.R;
import com.smartexam.marketplace.data.MarketplaceRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.QuestionPack;
import java.util.List;

public class PackDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PACK_ID = "EXTRA_PACK_ID";

    private TextView tvTitle, tvSubject, tvGrade, tvDescription, tvAlreadyPurchased;
    private Button btnBuyPack;
    private QuestionPack currentPack;
    private AppDatabase localDb;
    private MarketplaceRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pack_detail);

        String packId = getIntent().getStringExtra(EXTRA_PACK_ID);
        if (packId == null) {
            finish();
            return;
        }

        initViews();
        localDb = AppDatabase.getInstance(this);
        repository = MarketplaceRepository.getInstance(this, localDb);
        loadPackDetails(packId);
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvSubject = findViewById(R.id.tvDetailSubject);
        tvGrade = findViewById(R.id.tvDetailGrade);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvAlreadyPurchased = findViewById(R.id.tvAlreadyPurchased);
        btnBuyPack = findViewById(R.id.btnBuyPack);

        btnBuyPack.setOnClickListener(v -> initiatePurchase());
    }

    private void loadPackDetails(String packId) {
        // Fetch from Firestore
        FirebaseFirestore.getInstance().collection("question_packs").document(packId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentPack = documentSnapshot.toObject(QuestionPack.class);
                    if (currentPack != null) {
                        bindData(currentPack);
                        checkPurchaseStatus();
                    } else {
                        Toast.makeText(this, "Pack not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void checkPurchaseStatus() {
        repository.isPackPurchased(currentPack.getId(), new MarketplaceRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean purchased) {
                runOnUiThread(() -> {
                    if (purchased) {
                        btnBuyPack.setVisibility(View.GONE);
                        tvAlreadyPurchased.setVisibility(View.VISIBLE);
                    } else {
                        btnBuyPack.setVisibility(View.VISIBLE);
                        tvAlreadyPurchased.setVisibility(View.GONE);
                        btnBuyPack.setText("Buy for " + String.format("R%.2f", currentPack.getPriceCents() / 100.0));
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("PackDetail", "Error checking purchase: " + error);
            }
        });
    }

    private void bindData(QuestionPack pack) {
        tvTitle.setText(pack.getTitle());
        tvSubject.setText(pack.getSubject());
        tvGrade.setText("Grade " + pack.getGrade());
        tvDescription.setText(pack.getDescription());
    }

    private void updatePurchaseState() {
        if (currentPack.isPurchased()) {
            btnBuyPack.setVisibility(View.GONE);
            tvAlreadyPurchased.setVisibility(View.VISIBLE);
        } else {
            btnBuyPack.setVisibility(View.VISIBLE);
            tvAlreadyPurchased.setVisibility(View.GONE);
            btnBuyPack.setText("Buy for " + currentPack.getFormattedPrice());
        }
    }

    private void initiatePurchase() {
        if (currentPack == null)
            return;

        btnBuyPack.setEnabled(false);
        btnBuyPack.setText("Processing...");

        // In a production app, here we would launch the Stripe Payment Sheet.
        // For now, we simulate a successful transaction and record it.
        String mockTransactionId = "txn_" + System.currentTimeMillis();

        repository.recordPurchase(currentPack.getId(), mockTransactionId,
                new MarketplaceRepository.DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        runOnUiThread(() -> {
                            btnBuyPack.setEnabled(true);
                            Toast.makeText(PackDetailActivity.this, "Purchase Successful! Syncing...",
                                    Toast.LENGTH_LONG).show();
                            checkPurchaseStatus();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnBuyPack.setEnabled(true);
                            Toast.makeText(PackDetailActivity.this, "Purchase Failed: " + error, Toast.LENGTH_SHORT)
                                    .show();
                        });
                    }
                });
    }
}
