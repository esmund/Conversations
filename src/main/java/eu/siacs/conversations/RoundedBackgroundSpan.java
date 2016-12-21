package eu.siacs.conversations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import eu.siacs.conversations.ui.StartConversationActivity;

/**
 * Created by esm on 20/12/16.
 */

public class RoundedBackgroundSpan extends ReplacementSpan{

    private static int CORNER_RADIUS = 20;
    private int backgroundColor = 0;
    private int textColor = 0;

    public RoundedBackgroundSpan(Context context) {
        super();
        backgroundColor = context.getResources().getColor(R.color.link_text_material_light);
        textColor = ((StartConversationActivity)context).getPrimaryTextColor();
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        RectF rect = new RectF(x, top, x + measureText(paint, text, start, end), bottom);
        paint.setColor(backgroundColor);
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint);
        paint.setColor(textColor);
        canvas.drawText(text, start, end, x, y, paint);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end));
    }

    public void paintText(){
        this.backgroundColor = 80000000;
    }
    private float measureText(Paint paint, CharSequence text, int start, int end) {
        return paint.measureText(text, start, end);
    }
}
