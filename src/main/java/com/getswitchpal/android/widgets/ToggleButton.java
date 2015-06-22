package com.getswitchpal.android.widgets;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Overrides the default ToggleButton behavior
 */
public class ToggleButton extends android.widget.ToggleButton {

    public ToggleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ToggleButton(Context context) {
        super(context);
    }

    @Override
    public void toggle() {
        // Do nothing, we will change the state later
        //super.toggle();
    }
}
