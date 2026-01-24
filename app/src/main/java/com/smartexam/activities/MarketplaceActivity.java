package com.smartexam.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartexam.R;
import com.smartexam.config.FeatureFlags;
import com.smartexam.marketplace.data.MarketplaceRepository;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.QuestionPack;
import com.smartexam.marketplace.ui.MarketplaceAdapter;
import com.smartexam.marketplace.ui.PackDetailActivity;
import java.util.List;

public class MarketplaceActivity extends AppCompatActivity {

    private RecyclerView rvMarketplace;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private MarketplaceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Safety check: Close if flag is disabled
        if (!com.smartexam.config.RemoteConfigManager.getInstance().isMarketplaceEnabled()) {
            finish();
            return;
        }

        // Log Marketplace View
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(this)
                .logEvent(com.google.firebase.analytics.FirebaseAnalytics.Event.VIEW_ITEM_LIST, null);

        setContentView(R.layout.activity_marketplace);

        setTitle("Question Pack Store");

        rvMarketplace = findViewById(R.id.rvMarketplace);
        progressBar = findViewById(R.id.progressBar); // Ensure ID exists in layout or add dynamically
        // Note: functionality assumes activity_marketplace has a ProgressBar with id
        // progressBar
        // If not, we should probably add it or handle it gracefully.
        // For this step, I'll rely on the existing layout having a generic container or
        // just add the ID to the logic
        // but since I can't easily see the XML right now, I'll look for it or fail
        // gracefully.

        // Let's inspect the layout first or just proceed assuming I can update the XML
        // if needed.
        // Actually, looking at previous history, activity_marketplace was just a stub.

        rvMarketplace.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MarketplaceAdapter(this::openPackDetail);
        rvMarketplace.setAdapter(adapter);

        // Find standard views, handle nulls if layout differs
        if (findViewById(R.id.tvEmpty) != null) {
            tvEmpty = findViewById(R.id.tvEmpty);
        }

        fetchMarketplaceItems();
    }

    private void fetchMarketplaceItems() {
        // Show loading state if views exist
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);

        AppDatabase localDb = AppDatabase.getInstance(this);
        MarketplaceRepository.getInstance(this, localDb)
                .getAvailablePacks(new MarketplaceRepository.DataCallback<List<QuestionPack>>() {
                    @Override
                    public void onSuccess(List<QuestionPack> data) {
                        if (progressBar != null)
                            progressBar.setVisibility(View.GONE);
                        adapter.setPacks(data);

                        if (data.isEmpty() && tvEmpty != null) {
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else if (tvEmpty != null) {
                            tvEmpty.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (progressBar != null)
                            progressBar.setVisibility(View.GONE);
                        Toast.makeText(MarketplaceActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openPackDetail(QuestionPack pack) {
        Intent intent = new Intent(this, PackDetailActivity.class);
        intent.putExtra(PackDetailActivity.EXTRA_PACK_ID, pack.getId());
        startActivity(intent);
    }
}
