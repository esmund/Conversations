package eu.siacs.conversations;

import android.view.View;

import eu.siacs.conversations.entities.Contact;

/**
 * Created by esm on 6/1/17.
 */

public class ContactSpan {
    private Contact contact;
    private View listItemView;
    private RoundedBackgroundSpan roundedBackgroundSpan;
    private boolean isHighlighted;

    public ContactSpan(){

    }
    public ContactSpan(Contact contact,View v){
        this.contact = contact;
        this.listItemView = v;
    }

    public void clearSpan(){

    }
}
