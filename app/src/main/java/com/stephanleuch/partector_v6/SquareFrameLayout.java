package com.stephanleuch.partector_v6;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

// special FrameLayout which is always a square
// appears to be used for the circle in which the LDSA value is shown.
public class SquareFrameLayout extends FrameLayout {

    public SquareFrameLayout(Context context){
        super(context);
    }

    public SquareFrameLayout(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        //noinspection SuspiciousNameCombination
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
