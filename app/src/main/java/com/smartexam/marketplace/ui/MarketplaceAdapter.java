package com.smartexam.marketplace.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartexam.R;
import com.smartexam.models.QuestionPack;
import java.util.ArrayList;
import java.util.List;

public class MarketplaceAdapter extends RecyclerView.Adapter<MarketplaceAdapter.PackViewHolder> {

    private List<QuestionPack> packs = new ArrayList<>();
    private final OnPackClickListener listener;

    public interface OnPackClickListener {
        void onPackClick(QuestionPack pack);
    }

    public MarketplaceAdapter(OnPackClickListener listener) {
        this.listener = listener;
    }

    public void setPacks(List<QuestionPack> newPacks) {
        this.packs = newPacks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_marketplace_pack, parent, false);
        return new PackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackViewHolder holder, int position) {
        QuestionPack pack = packs.get(position);
        holder.bind(pack, listener);
    }

    @Override
    public int getItemCount() {
        return packs.size();
    }

    static class PackViewHolder extends RecyclerView.ViewHolder {
        TextView tvPackTitle, tvPackSubject, tvPackGrade, tvQuestionCount, tvPackPrice;

        public PackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPackTitle = itemView.findViewById(R.id.tvPackTitle);
            tvPackSubject = itemView.findViewById(R.id.tvPackSubject);
            tvPackGrade = itemView.findViewById(R.id.tvPackGrade);
            tvQuestionCount = itemView.findViewById(R.id.tvQuestionCount);
            tvPackPrice = itemView.findViewById(R.id.tvPackPrice);
        }

        public void bind(final QuestionPack pack, final OnPackClickListener listener) {
            tvPackTitle.setText(pack.getTitle());
            tvPackSubject.setText(pack.getSubject());
            tvPackGrade.setText("Grade " + pack.getGrade());
            tvQuestionCount.setText(pack.getQuestionCount() + " Questions");

            // For now, we assume if it's in the local DB purchased_packs table, it's
            // purchased
            // This logic will be improved when integrated with the Repository check
            tvPackPrice.setText(String.format("R%.2f", pack.getPriceCents() / 100.0));
            tvPackPrice.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark, null));

            itemView.setOnClickListener(v -> listener.onPackClick(pack));
        }
    }
}
