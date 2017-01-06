package eu.siacs.conversations;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

/**
 * Created by esm on 5/1/17.
 */

public class ToContactEditText extends EditText {

    public static boolean overrideDel = false;

    public ToContactEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ToContactEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ToContactEditText(Context context) {
        super(context);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new ToContactConnection(super.onCreateInputConnection(outAttrs),
                true);
    }

    private class ToContactConnection extends InputConnectionWrapper {

        public ToContactConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {


            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                if(overrideDel){
                    Log.d("debug","delete overridden");
                    overrideDel = false;
                    return false;
                }
                else {
                    Log.d("debug","delete not overridden");
                    return super.sendKeyEvent(event);
                }
            }

            //For all other keys
            if(event.getAction() == KeyEvent.ACTION_DOWN && overrideDel){
                Log.d("debug","delete overridden");
                return false;
            }
            return super.sendKeyEvent(event);
        }

    }

}
