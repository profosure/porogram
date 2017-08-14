/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package com.porogram.profosure1.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import com.porogram.profosure1.messenger.AndroidUtilities;
import com.porogram.profosure1.ui.ActionBar.Theme;

public class DividerCell extends View {

    public DividerCell(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(16) + 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawLine(getPaddingLeft(), AndroidUtilities.dp(8), getWidth() - getPaddingRight(), AndroidUtilities.dp(8), Theme.dividerPaint);
    }
}
