package io.evercam.android.wear;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WearableListItemLayout extends LinearLayout implements WearableListView.Item
{

    private final float mFadedTextAlpha;
    private final int mFadedCircleColor;
    private final int mChosenCircleColor;
    private ImageView mCircle;
    private float mScale;
    private TextView mName;

    public WearableListItemLayout(Context context)
    {
        this(context, null);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        mFadedTextAlpha = getResources().getInteger(R.integer.action_text_faded_alpha) / 100f;
        mFadedCircleColor = getResources().getColor(R.color.grey);
        mChosenCircleColor = getResources().getColor(R.color.blue);
    }

    // Get references to the icon and text in the item layout definition
    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        // These are defined in the layout file for list items
        // (see next section)
        mCircle = (ImageView) findViewById(R.id.circle);
        mName = (TextView) findViewById(R.id.name);
    }

    // Provide scaling values for WearableListView animations
    @Override
    public float getProximityMinValue()
    {
        return 1f;
    }

    @Override
    public float getProximityMaxValue()
    {
        return 1.6f;
    }

    @Override
    public float getCurrentProximityValue()
    {
        return mScale;
    }

    // Scale the icon for WearableListView animations
    @Override
    public void setScalingAnimatorValue(float scale)
    {
        mScale = scale;
        mCircle.setScaleX(scale);
        mCircle.setScaleY(scale);
    }

    // Change color of the icon, remove fading from the text
    @Override
    public void onScaleUpStart()
    {
        mName.setAlpha(1f);
        ((GradientDrawable) mCircle.getDrawable()).setColor(mChosenCircleColor);
    }

    // Change the color of the icon, fade the text
    @Override
    public void onScaleDownStart()
    {
        ((GradientDrawable) mCircle.getDrawable()).setColor(mFadedCircleColor);
        mName.setAlpha(mFadedTextAlpha);
    }
}