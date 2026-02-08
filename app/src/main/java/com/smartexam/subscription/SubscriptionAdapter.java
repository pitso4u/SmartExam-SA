package com.smartexam.subscription;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartexam.R;
import java.util.List;

/**
 * Adapter for subscription plans in the subscription activity.
 */
public class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.ViewHolder> {
    
    private final List<SubscriptionPlan> plans;
    private final OnSubscriptionSelectedListener listener;
    
    public interface OnSubscriptionSelectedListener {
        void onSubscriptionSelected(SubscriptionPlan plan);
    }
    
    public SubscriptionAdapter(List<SubscriptionPlan> plans, OnSubscriptionSelectedListener listener) {
        this.plans = plans;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_subscription_plan, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubscriptionPlan plan = plans.get(position);
        
        holder.tvTitle.setText(plan.getTitle());
        holder.tvPrice.setText(plan.getPrice());
        holder.tvDescription.setText(plan.getDescription());
        holder.tvBillingPeriod.setText(plan.getBillingPeriod());
        
        holder.btnSubscribe.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSubscriptionSelected(plan);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return plans.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvPrice;
        TextView tvDescription;
        TextView tvBillingPeriod;
        Button btnSubscribe;
        
        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvPrice = view.findViewById(R.id.tvPrice);
            tvDescription = view.findViewById(R.id.tvDescription);
            tvBillingPeriod = view.findViewById(R.id.tvBillingPeriod);
            btnSubscribe = view.findViewById(R.id.btnSubscribe);
        }
    }
}
