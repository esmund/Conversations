package eu.siacs.conversations;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.xmpp.jid.Jid;

/**
 * Created by esm on 6/1/17.
 */

public class ContactSpan {
    private Contact contact;
    private View listItemView;
    private int textColor;
    private RoundedBackgroundSpan roundedBackgroundSpan;
    private boolean isHighlighted;

    public ContactSpan(){

    }
    public ContactSpan(Contact contact,View v,int textColor){
        this.contact = contact;
        this.listItemView = v;
        this.textColor = textColor;
    }

    public void setCheckbox(){
        contact.setChecked(false);
        ((TextView) listItemView.findViewById(R.id.contact_display_name)).setTextColor(textColor);
        ((TextView) listItemView.findViewById(R.id.contact_jid)).setTextColor(textColor);
        ((CheckBox) listItemView.findViewById(R.id.selected_checkbox)).setChecked(false);
    }

    public void setCheckbox(boolean setChecked){
        setCheckbox();
        contact.setChecked(setChecked);
    }

    public void setSpan(RoundedBackgroundSpan roundedBackgroundSpan){
        this.roundedBackgroundSpan = roundedBackgroundSpan;

    }

    public Jid getJid(){
        return contact.getJid();
    }

    public Contact getContact(){
        return this.contact;
    }
}
