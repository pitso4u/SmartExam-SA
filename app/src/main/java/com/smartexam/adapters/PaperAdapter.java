package com.smartexam.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartexam.R;
import com.smartexam.models.AssessmentPaper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaperAdapter extends RecyclerView.Adapter<PaperAdapter.PaperViewHolder> {

    private List<AssessmentPaper> papers;
    private final OnPaperClickListener listener;

    public interface OnPaperClickListener {
        void onPaperClick(AssessmentPaper paper);
    }

    public PaperAdapter(List<AssessmentPaper> papers, OnPaperClickListener listener) {
        this.papers = papers;
        this.listener = listener;
    }

    public void updateData(List<AssessmentPaper> newPapers) {
        this.papers = newPapers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent,
                false);
        return new PaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaperViewHolder holder, int position) {
        AssessmentPaper paper = papers.get(position);
        holder.tvTitle.setText(paper.getTitle());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        holder.tvDate.setText("Generated: " + sdf.format(new Date(paper.getCreatedAt())));

        holder.itemView.setOnClickListener(v -> listener.onPaperClick(paper));
    }

    @Override
    public int getItemCount() {
        return papers.size();
    }

    static class PaperViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate;

        PaperViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(android.R.id.text1);
            tvDate = itemView.findViewById(android.R.id.text2);
        }
    }
}
