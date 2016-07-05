package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Derek on 6/20/2016.
 *
 * Taken from: http://www.ryadel.com/en/android-proportionally-stretch-imageview-fit-whole-screen-width-maintaining-aspect-ratio/
 */
public class ProportionalImageView extends ImageView {
    // this defines the default height to width ratio as 2 to 3
    private float mProportion = 2.0f / 3.0f;
    public float getProportion() { return mProportion; }
    public void setProportion(float newPro) { mProportion = newPro; }

    public ProportionalImageView(Context context) {
        super(context);
    }

    public ProportionalImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProportionalImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getDrawable();
        if (d != null) {
            int w = MeasureSpec.getSize(widthMeasureSpec);

            // this uses the height and width of the original image
            //int h = w * d.getIntrinsicHeight() / d.getIntrinsicWidth();
            //int h = w * 2 / 3;

            int h = Math.round(w * mProportion);
            setMeasuredDimension(w, h);
        }
        else super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }}
