package eu.siacs.conversations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import eu.siacs.conversations.ui.StartConversationActivity;

import static android.R.color.white;

/**
 * Created by esm on 20/12/16.
 */

public class RoundedBackgroundSpan extends ReplacementSpan{

    private static int CORNER_RADIUS = 20;
    private int backgroundColor = 0;
    private int textColor = 0;
    private Context context;
    private boolean isHighlighted;
    private boolean editedSpan;
    private int spanEnd;
    private int spanStart;

    public RoundedBackgroundSpan(Context context) {
        super();
        this.context = context;
        //backgroundColor = context.getResources().getColor(R.color.link_text_material_light);
        backgroundColor = white;
        textColor = ((StartConversationActivity)context).getPrimaryTextColor();
        isHighlighted = false;
        editedSpan = false;

    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        RectF rect;
        rect = new RectF(x, top, x + measureText(paint, text, start, end), bottom);
        this.spanEnd = end;
        this.spanStart = start;

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

        if(!isHighlighted) {
            this.backgroundColor = context.getResources().getColor(R.color.link_text_material_dark);
        }
        else {
            this.backgroundColor = white;
        }

        isHighlighted = !isHighlighted;
    }

    public int getSpanEnd(){
        return this.spanEnd;
    }
    public int getSpanStart(){
        return this.spanStart;
    }
    public String getString(){
        return this.getString();
    }

    private float measureText(Paint paint, CharSequence text, int start, int end) {
        return paint.measureText(text, start, end);
    }

}
