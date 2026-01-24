package com.smartexam.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartexam.R;
import com.smartexam.models.Question;
import java.util.ArrayList;
import java.util.List;

public class PaperQuestionAdapter extends RecyclerView.Adapter<PaperQuestionAdapter.QuestionViewHolder> {

    private List<Question> questions = new ArrayList<>();

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paper_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questions.get(position);
        holder.tvQuestionNumber.setText("Question " + (position + 1));
        holder.tvQuestionText.setText(question.getQuestionText());
        holder.tvQuestionMarks.setText(question.getMarks() + " marks");
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    public void submitList(List<Question> data) {
        questions = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionNumber;
        TextView tvQuestionText;
        TextView tvQuestionMarks;

        QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestionNumber = itemView.findViewById(R.id.tvQuestionNumber);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            tvQuestionMarks = itemView.findViewById(R.id.tvQuestionMarks);
        }
    }
}
