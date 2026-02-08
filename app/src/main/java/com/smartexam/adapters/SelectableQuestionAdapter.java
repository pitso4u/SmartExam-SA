package com.smartexam.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartexam.R;
import com.smartexam.models.Question;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectableQuestionAdapter extends RecyclerView.Adapter<SelectableQuestionAdapter.QuestionViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount, int selectedMarks);
    }

    private final List<Question> questions = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private final OnSelectionChangedListener listener;
    private boolean isSelectionEnabled = true;

    public SelectableQuestionAdapter(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void setSelectionEnabled(boolean enabled) {
        this.isSelectionEnabled = enabled;
        notifyDataSetChanged();
    }

    public void setQuestions(List<Question> newQuestions) {
        questions.clear();
        if (newQuestions != null) {
            questions.addAll(newQuestions);
        }
        pruneSelection();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void clearSelection() {
        if (selectedIds.isEmpty()) {
            return;
        }
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void setSelectedQuestionIds(Collection<String> ids) {
        selectedIds.clear();
        if (ids != null && !ids.isEmpty()) {
            Set<String> allowed = new HashSet<>(ids);
            for (Question question : questions) {
                if (allowed.contains(question.getId())) {
                    selectedIds.add(question.getId());
                }
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public List<Question> getSelectedQuestions() {
        List<Question> selected = new ArrayList<>();
        for (Question question : questions) {
            if (selectedIds.contains(question.getId())) {
                selected.add(question);
            }
        }
        return selected;
    }

    public int getSelectedMarksTotal() {
        int total = 0;
        for (Question question : questions) {
            if (selectedIds.contains(question.getId())) {
                total += question.getMarks();
            }
        }
        return total;
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selectable_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question q = questions.get(position);
        holder.tvQuestionText.setText(q.getQuestionText());

        String metadata = "Marks: " + q.getMarks() + " | Difficulty: " + q.getDifficulty();
        holder.tvMetadata.setText(metadata);

        holder.cbSelect.setVisibility(isSelectionEnabled ? View.VISIBLE : View.GONE);
        holder.cbSelect.setChecked(selectedIds.contains(q.getId()));

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionEnabled) {
                toggleSelection(q);
            }
        });
    }

    private void toggleSelection(Question q) {
        if (selectedIds.contains(q.getId())) {
            selectedIds.remove(q.getId());
        } else {
            selectedIds.add(q.getId());
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    private void pruneSelection() {
        Set<String> availableIds = new HashSet<>();
        for (Question question : questions) {
            availableIds.add(question.getId());
        }
        selectedIds.retainAll(availableIds);
    }

    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(getSelectedCount(), getSelectedMarksTotal());
        }
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        final TextView tvQuestionText;
        final TextView tvMetadata;
        final CheckBox cbSelect;

        QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            tvMetadata = itemView.findViewById(R.id.tvMetadata);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}
