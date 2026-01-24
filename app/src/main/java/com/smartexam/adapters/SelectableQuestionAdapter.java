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

    public SelectableQuestionAdapter(OnSelectionChangedListener listener) {
        this.listener = listener;
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
        Question question = questions.get(position);
        holder.tvTopic.setText(question.getTopic() != null ? question.getTopic() : asPlaceholder("No topic"));
        holder.tvQuestionText.setText(question.getQuestionText() != null ? question.getQuestionText() : "");
        holder.tvMetadata.setText(metadata.toString());

        if (question.getCognitiveLevel() != null) {
            holder.tvCognitiveLevel.setVisibility(View.VISIBLE);
            holder.tvCognitiveLevel.setText(question.getCognitiveLevel().name());
        } else {
            holder.tvCognitiveLevel.setVisibility(View.GONE);
        }

        if (question.getType() != null) {
            holder.tvQuestionType.setVisibility(View.VISIBLE);
            holder.tvQuestionType.setText(question.getType().name());
        } else {
            holder.tvQuestionType.setVisibility(View.GONE);
        }

        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(selectedIds.contains(question.getId()));
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIds.add(question.getId());
            } else {
                selectedIds.remove(question.getId());
            }
            notifySelectionChanged();
        });

        holder.itemView.setOnClickListener(v -> holder.cbSelect.performClick());
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
        final TextView tvTopic;
        final TextView tvQuestionText;
        final TextView tvMarks;
        final TextView tvMetadata;
        final TextView tvCognitiveLevel;
        final TextView tvQuestionType;
        final CheckBox cbSelect;

        QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            tvMarks = itemView.findViewById(R.id.tvMarks);
            tvMetadata = itemView.findViewById(R.id.tvMetadata);
            tvCognitiveLevel = itemView.findViewById(R.id.tvCognitiveLevel);
            tvQuestionType = itemView.findViewById(R.id.tvQuestionType);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }

    private String asPlaceholder(String value) {
        return "(" + value + ")";
    }
}
