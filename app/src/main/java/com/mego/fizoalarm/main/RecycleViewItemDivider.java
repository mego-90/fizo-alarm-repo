package com.mego.fizoalarm.main;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Mohammad Fayez Mansour
 *
 * You Can Set line padding by The Methods
 * To Change its Start and End points.
 *
 * You Can Change The Divider From ListDivider
 * To Shape defined in XML Shape File and Pass its ResID
 * in The specified Constructor.
 */

public class RecycleViewItemDivider extends RecyclerView.ItemDecoration {

    private static final int[] ATTRS = new int[] { android.R.attr.listDivider };

    private Drawable mDivider;

    private int lineLeftPadding = 0;

    private int lineRightPadding = 0;


    public RecycleViewItemDivider(Context context) {
        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        mDivider = a.getDrawable(0);
        a.recycle();
    }

    public RecycleViewItemDivider(Context context, int resId) {
        mDivider = ContextCompat.getDrawable(context, resId);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int left = parent.getPaddingLeft() + lineLeftPadding;
        final int right = parent.getWidth() - parent.getPaddingRight() - lineRightPadding;

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
            final int top = child.getBottom() + params.bottomMargin;
            final int bottom = top + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
    }

    public int getLineLeftPadding() {
        return lineLeftPadding;
    }

    public void setLineLeftPadding(int lineLeftPadding) {
        this.lineLeftPadding = lineLeftPadding;
    }

    public int getLineRightPadding() {
        return lineRightPadding;
    }

    public void setLineRightPadding(int lineRightPadding) {
        this.lineRightPadding = lineRightPadding;
    }


}