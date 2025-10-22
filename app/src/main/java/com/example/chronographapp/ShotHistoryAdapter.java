package com.example.chronographapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ShotHistoryAdapter extends RecyclerView.Adapter<ShotHistoryAdapter.ViewHolder> {

    private List<ShotData> shotList;
    private OnShotClickListener onShotClickListener;

    private final int COLOR_HIGH_VELOCITY = Color.parseColor("#D32F2F");
    private final int COLOR_MEDIUM_VELOCITY = Color.parseColor("#FF9800");
    private final int COLOR_LOW_VELOCITY = Color.parseColor("#4CAF50");

    public interface OnShotClickListener {
        void onShotClick(int position, ShotData shot);
        void onShotLongClick(int position, ShotData shot);
    }

    public ShotHistoryAdapter(List<ShotData> shotList) {
        this.shotList = shotList;
    }

    public void setOnShotClickListener(OnShotClickListener listener) {
        this.onShotClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shot_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShotData shot = shotList.get(position);

        int displayNumber = shotList.size() - position;
        holder.shotNumberText.setText(String.format("#%d", displayNumber));
        holder.timestampText.setText(shot.getTimestamp());
        holder.velocityText.setText(String.format("%.1f м/с", shot.getVelocity()));
        holder.energyText.setText(String.format("%.2f Дж", shot.getEnergy()));

        setVelocityColor(holder.velocityText, shot.getVelocity());
        setupAppearance(holder, position);
        setupClickListeners(holder, shot, position);
    }

    private void setVelocityColor(TextView velocityText, float velocity) {
        if (velocity > 180) {
            velocityText.setTextColor(COLOR_HIGH_VELOCITY);
        } else if (velocity > 160) {
            velocityText.setTextColor(COLOR_MEDIUM_VELOCITY);
        } else {
            velocityText.setTextColor(COLOR_LOW_VELOCITY);
        }
    }

    private void setupAppearance(ViewHolder holder, int position) {
        if (position % 2 == 0) {
            holder.container.setBackgroundColor(Color.parseColor("#F8F9FA"));
        } else {
            holder.container.setBackgroundColor(Color.WHITE);
        }
    }

    private void setupClickListeners(ViewHolder holder, ShotData shot, int position) {
        holder.itemView.setOnClickListener(v -> {
            if (onShotClickListener != null) {
                onShotClickListener.onShotClick(position, shot);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onShotClickListener != null) {
                onShotClickListener.onShotLongClick(position, shot);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return shotList != null ? shotList.size() : 0;
    }

    public void updateData(List<ShotData> newShotList) {
        this.shotList = newShotList;
        notifyDataSetChanged();
    }

    public void addShot(ShotData shot) {
        if (shotList != null) {
            shotList.add(0, shot);
            notifyItemInserted(0);
        }
    }

    public void removeShot(int position) {
        if (shotList != null && position >= 0 && position < shotList.size()) {
            shotList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clearData() {
        if (shotList != null) {
            int size = shotList.size();
            shotList.clear();
            if (size > 0) {
                notifyItemRangeRemoved(0, size);
            }
        }
    }

    public ShotData getShotAt(int position) {
        if (shotList != null && position >= 0 && position < shotList.size()) {
            return shotList.get(position);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView shotNumberText;
        TextView timestampText;
        TextView velocityText;
        TextView energyText;

        ViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
            shotNumberText = itemView.findViewById(R.id.shotNumberText);
            timestampText = itemView.findViewById(R.id.timestampText);
            velocityText = itemView.findViewById(R.id.velocityText);
            energyText = itemView.findViewById(R.id.energyText);
        }
    }
}