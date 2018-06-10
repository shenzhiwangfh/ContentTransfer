package com.tct.transfer.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class FileInfoView extends View {

    public FileInfoView(Context context) {
        this(context, null);
    }

    public FileInfoView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileInfoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }
}
