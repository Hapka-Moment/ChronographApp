package com.example.chronographapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ShotHistoryAdapter extends RecyclerView.Adapter<ShotHistoryAdapter.ViewHolder> {

    private List<ShotData> shotList;
    private OnShotClickListener onShotClickListener;

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

        // В новом дизайне отображаем номера с начала
        holder.shotNumberText.setText(String.format("#%d", shot.getShotNumber()));
        holder.timestampText.setText(shot.getTimestamp());
        holder.velocityText.setText(String.format("%.1f м/с", shot.getVelocity()));
        holder.energyText.setText(String.format("%.2f Дж", shot.getEnergy()));

        // Настройка цвета скорости
        setVelocityColor(holder.velocityText, shot.getVelocity());

        // Обработчики кликов
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

    private void setVelocityColor(TextView velocityText, float velocity) {
        // Используем цвета из glass палитры
        int colorResource = R.color.glass_blue; // по умолчанию
        if (velocity > 180) {
            colorResource = R.color.glass_red;
        } else if (velocity > 160) {
            colorResource = R.color.glass_orange;
        } else if (velocity > 140) {
            colorResource = R.color.glass_green;
        } else {
            colorResource = R.color.glass_blue;
        }

        // Устанавливаем цвет через ресурс
        velocityText.setTextColor(velocityText.getContext().getColor(colorResource));
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
            shotList.add(shot);
            notifyItemInserted(shotList.size() - 1);
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
        TextView shotNumberText;
        TextView timestampText;
        TextView velocityText;
        TextView energyText;

        ViewHolder(View itemView) {
            super(itemView);
            shotNumberText = itemView.findViewById(R.id.shotNumberText);
            timestampText = itemView.findViewById(R.id.timestampText);
            velocityText = itemView.findViewById(R.id.velocityText);
            energyText = itemView.findViewById(R.id.energyText);
        }
    }
}