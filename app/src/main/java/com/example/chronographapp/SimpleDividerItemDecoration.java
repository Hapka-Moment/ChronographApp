package com.example.chronographapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
    private Paint paint;

    public SimpleDividerItemDecoration() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#E0E0E0"));
        paint.setStrokeWidth(1f);
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int left = parent.getPaddingLeft() + 32;
        int right = parent.getWidth() - parent.getPaddingRight() - 32;

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);

            if (child == null) continue;

            int top = child.getBottom();
            int bottom = top + 1;

            c.drawLine(left, top, right, bottom, paint);
        }
    }
}