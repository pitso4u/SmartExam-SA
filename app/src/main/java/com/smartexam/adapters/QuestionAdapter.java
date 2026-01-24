package com.smartexam.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartexam.R;
import com.smartexam.models.Question;

import java.util.ArrayList;
import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {
    private Context context;
    private List<Question> questions;
    private OnQuestionActionListener listener;

    public interface OnQuestionActionListener {
        void onEditClick(Question question);

        void onDeleteClick(Question question);
    }

    public QuestionAdapter(Context context, List<Question> questions, OnQuestionActionListener listener) {
        this.context = context;
        this.questions = questions != null ? questions : new ArrayList<>();
        this.listener = listener;
    }

    public void updateQuestions(List<Question> newQuestions) {
        this.questions = newQuestions != null ? newQuestions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questions.get(position);

        holder.tvTopic.setText(question.getTopic() != null ? question.getTopic() : "No Topic");
        holder.tvQuestionText.setText(question.getQuestionText() != null ? question.getQuestionText() : "");
        holder.tvMarks.setText(String.format("%d Marks", question.getMarks()));

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null)
                listener.onEditClick(question);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null)
                listener.onDeleteClick(question);
        });
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvQuestionText, tvMarks;
        Button btnEdit, btnDelete;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            tvMarks = itemView.findViewById(R.id.tvMarks);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
