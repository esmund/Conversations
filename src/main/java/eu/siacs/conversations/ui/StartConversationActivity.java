package eu.siacs.conversations.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.ContactSpan;
import eu.siacs.conversations.R;
import eu.siacs.conversations.RoundedBackgroundSpan;
import eu.siacs.conversations.ToContactEditText;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.Bookmark;
import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.entities.ListItem;
import eu.siacs.conversations.entities.Presence;
import eu.siacs.conversations.services.XmppConnectionService.OnRosterUpdate;
import eu.siacs.conversations.ui.adapter.KnownHostsAdapter;
import eu.siacs.conversations.ui.adapter.ListItemAdapter;
import eu.siacs.conversations.utils.XmppUri;
import eu.siacs.conversations.xmpp.OnUpdateBlocklist;
import eu.siacs.conversations.xmpp.XmppConnection;
import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;

import static eu.siacs.conversations.R.id.account;
import static eu.siacs.conversations.R.string.delete;

public class StartConversationActivity extends XmppActivity implements OnRosterUpdate, OnUpdateBlocklist {
    private final CharSequence delimiter = ",";

    public int conference_context_id;
    public int contact_context_id;
    private Tab mContactsTab;
    private Tab mConferencesTab;
    private ViewPager mViewPager;
    //private ListPagerAdapter mListPagerAdapter;
    private List<ListItem> contacts = new ArrayList<>();
    private ArrayAdapter<ListItem> mContactsAdapter;
    private List<ListItem> conferences = new ArrayList<>();
    private ArrayAdapter<ListItem> mConferenceAdapter;
    private List<String> mActivatedAccounts = new ArrayList<>();
    private List<String> mKnownHosts;
    private List<String> mKnownConferenceHosts;
    private Invite mPendingInvite = null;
    private EditText mSearchEditText;
    private EditText msgField;
    private ToContactEditText toContactEditText;
    private ListView contactListView;
    private AtomicBoolean mRequestedContactsPermission = new AtomicBoolean(false);
    private Button doneBtn;
    private final int REQUEST_SYNC_CONTACTS = 0x3b28cf;
    private final int REQUEST_CREATE_CONFERENCE = 0x3b39da;
    private final int REQUEST_CREATE_CONTACT = 2359;
    private Dialog mCurrentDialog = null;
    private boolean textIsUserInput = true;
    private StringBuilder currentSearchString = new StringBuilder();
    private ArrayList<RoundedBackgroundSpan> spanArrayList = new ArrayList();
    private ArrayList<Contact> clickedContactsList = new ArrayList();
    private ArrayList<View> clickedViewList = new ArrayList<>();
    private ArrayList<ContactSpan> contactSpanArrayList = new ArrayList<>();
    private char lastDelChar;
    private int indexOfSelectedSpan = -1;

    private boolean isHighlighted = false;
    private boolean delimiterDeleted = false;


//    private MenuItem.OnActionExpandListener mOnActionExpandListener = new MenuItem.OnActionExpandListener() {
//
//        @Override
//        public boolean onMenuItemActionExpand(MenuItem item) {
//            mSearchEditText.post(new Runnable() {
//
//                @Override
//                public void run() {
//                    mSearchEditText.requestFocus();
//                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.showSoftInput(mSearchEditText,
//                            InputMethodManager.SHOW_IMPLICIT);
//                }
//            });
//
//            return true;
//        }
//
//        @Override
//        public boolean onMenuItemActionCollapse(MenuItem item) {
//            hideKeyboard();
//            mSearchEditText.setText("");
//            filter(null);
//            return true;
//        }
//
//    };

    private boolean mHideOfflineContacts = false;
//    private TabListener mTabListener = new TabListener() {
//
//        @Override
//        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
//            return;
//        }
//
//        @Override
//        public void onTabSelected(Tab tab, FragmentTransaction ft) {
//            mViewPager.setCurrentItem(tab.getPosition());
//            onTabChanged();
//        }
//
//        @Override
//        public void onTabReselected(Tab tab, FragmentTransaction ft) {
//            return;
//        }
//    };
//    private ViewPager.SimpleOnPageChangeListener mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
//        @Override
//        public void onPageSelected(int position) {
//            if (getActionBar() != null) {
//                getActionBar().setSelectedNavigationItem(position);
//            }
//            onTabChanged();
//        }
//    };

    private TextWatcher mSearchTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable editable) {

            if(textIsUserInput) {
                //Do not search when textview is changed programmatically
                filter(currentSearchString.toString());
                //Log.d("debug","filter contact for "+currentSearchString.toString());
                Log.d("debug","filter = "+currentSearchString.toString());
            }
            else {
                textIsUserInput = true;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            if(count > 0 && textIsUserInput){
                lastDelChar = s.charAt(s.length()-count);
                //Log.d("debug","lastDelChar = "+lastDelChar);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (textIsUserInput ) {
                if(s.toString().length() != 0) {
                    //add the new chars into currentSearchString
                    currentSearchString.append(s.subSequence(start, start + count).toString());
                    if (before > 0) {

                        if (lastDelChar == delimiter.charAt(0)) {
//                        Spannable spanText = toContactEditText.getText();
//                        String toCheck = spanText.subSequence(spanText.getSpanStart(this), spanText.getSpanEnd(this)).toString();
//                        indexOfSelectedSpan=selectedContacts.indexOf((toCheck));
                            //get the previous position
                            indexOfSelectedSpan = spanArrayList.size() - 1;
                            spanArrayList.get(indexOfSelectedSpan).paintText();
                            delimiterDeleted = true;
                        } else {
                            delimiterDeleted = false;
                        }
                    }
                }


                    if (currentSearchString.length() - before > 0) {
                        //textIsUserInput = false;
                        //delete last backspaced char from currentSearchString
                        currentSearchString.delete(currentSearchString.length() - before, currentSearchString.length());
                    } else {
                        //clear currentSearchString if it's empty
                        currentSearchString.delete(0, currentSearchString.length());
                        //currentSearchString.replace(0, currentSearchString.length(), "");
                    }
                }
            }
    };
//
//    private TextView.OnEditorActionListener mSearchDone = new TextView.OnEditorActionListener() {
//        @Override
//        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//            int pos = getActionBar().getSelectedNavigationIndex();
//            if (pos == 0) {
//                if (contacts.size() == 1) {
//                    openConversationForContact((Contact) contacts.get(0));
//                    return true;
//                }
//            } else {
//                if (conferences.size() == 1) {
//                    openConversationsForBookmark((Bookmark) conferences.get(0));
//                    return true;
//                }
//            }
//            hideKeyboard();
//            //mListPagerAdapter.requestFocus(pos);
//            return true;
//        }
//    };
//    private MenuItem mMenuSearchView;


    private View.OnClickListener onDoneClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (spanArrayList.size() > 0) {
                if (spanArrayList.size() == 1) {
                    openConversationForContact(contactSpanArrayList.get(0).getContact(),msgField.getText().toString());
                    clearData();
                } else {

                    Account account = xmppConnectionService.getAccounts().get(0);
                    final String subject = "";
                    List<Jid> jids = new ArrayList<>();

                    Iterator<ContactSpan> contactSpanIterator =  contactSpanArrayList.iterator();
                    while(contactSpanIterator.hasNext()){
                        jids.add(contactSpanIterator.next().getJid());
                    }
                    xmppConnectionService.createAdhocConference(account, subject, jids, mAdhocConferenceCallback);

                    mToast = Toast.makeText(getApplicationContext(), R.string.creating_conference, Toast.LENGTH_LONG);
                    mToast.show();
                    clearData();
                }
            } else{
                Toast.makeText(getApplicationContext(), "Please select a contact", Toast.LENGTH_SHORT).show();
            }
        }};

//    ListItemAdapter.OnTagClickedListener mOnTagClickedListener = new ListItemAdapter.OnTagClickedListener() {
//        @Override
//        public void onTagClicked(String tag) {
//            Log.d("debug","onTagClicked");
//            if (mMenuSearchView != null) {
//                mMenuSearchView.expandActionView();
//                mSearchEditText.setText("");
//                mSearchEditText.append(tag);
//                filter(tag);
//            }
//        }
//    };
    private String mInitialJid;
    private Pair<Integer, Intent> mPostponedActivityResult;
    private UiCallback<Conversation> mAdhocConferenceCallback = new UiCallback<Conversation>() {
        @Override
        public void success(final Conversation conversation) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideToast();
                    String msg = msgField.getText().toString();
                    if(!msg.equals("")) {
                        switchToConversation(conversation,msg,false);
                    }
                    else {
                        switchToConversation(conversation);
                    }

                }
            });
        }

        @Override
        public void error(final int errorCode, Conversation object) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    replaceToast(getString(errorCode));
                }
            });
        }

        @Override
        public void userInputRequried(PendingIntent pi, Conversation object) {

        }
    };
    private Toast mToast;

    protected void hideToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    protected void replaceToast(String msg) {
        hideToast();
        mToast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        mToast.show();
    }

    @Override
    public void onRosterUpdate() {
        this.refreshUi();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_conversation);

//        ActionBar actionBar = getActionBar();
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//
//        mContactsTab = actionBar.newTab().setText(R.string.contacts)
//                .setTabListener(mTabListener);
//        mConferencesTab = actionBar.newTab().setText(R.string.conferences)
//                .setTabListener(mTabListener);
//            actionBar.addTab(mContactsTab);
//            actionBar.addTab(mConferencesTab);
//
        msgField = (EditText)findViewById(R.id.et_msg_field);
        toContactEditText= (ToContactEditText)findViewById(R.id.to_contact_field);

        //mViewPager = (ViewPager) findViewById(R.id.start_conversation_view_pager);

        doneBtn = (Button) findViewById(R.id.button_done);

        //mViewPager.setOnPageChangeListener(mOnPageChangeListener);
        //mListPagerAdapter = new ListPagerAdapter(getFragmentManager());
        //mViewPager.setAdapter(mListPagerAdapter);

        //mConferenceAdapter = new ListItemAdapter(this, conferences);
        mContactsAdapter = new ListItemAdapter(this, contacts);

        //((ListItemAdapter) mContactsAdapter).setOnTagClickedListener(this.mOnTagClickedListener);
        this.mHideOfflineContacts = getPreferences().getBoolean("hide_offline", false);
        doneBtn.setOnClickListener(onDoneClick);

        contactListView = (ListView) findViewById(R.id.start_conversation_list_view);

        contactListView.setAdapter(mContactsAdapter);

        registerForContextMenu(contactListView);

        contactListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int position, long arg3) {

                StringBuilder currentText = new StringBuilder(toContactEditText.getText().toString());
                Spannable spannableText = toContactEditText.getText();
                SpannableStringBuilder spanStringBuilder = new SpannableStringBuilder();
                CheckBox selectorChkbox = (CheckBox)(arg1.findViewById(R.id.selected_checkbox));
                //workaround for making view clickable
                selectorChkbox.setFocusable(false);
                RoundedBackgroundSpan bgSpan = new RoundedBackgroundSpan(StartConversationActivity.this);
                Contact currentContact = (Contact) arg0.getAdapter().getItem(position);
                int indexOfCheckedListItem = clickedViewList.indexOf(arg1);
                boolean hasTypedChars = true;

                if(selectorChkbox.isChecked()){
                    if(isHighlighted) {
                        if(indexOfSelectedSpan == indexOfCheckedListItem) {
                            deleteSpan(indexOfSelectedSpan);
                            contactSpanArrayList.remove(indexOfSelectedSpan);
                            indexOfSelectedSpan = -1;
                        }
                        else {
                            deleteSpan(indexOfCheckedListItem,1);
                            contactSpanArrayList.get(indexOfCheckedListItem).setCheckbox(true);
                            contactSpanArrayList.remove(indexOfCheckedListItem);

                            if(indexOfSelectedSpan > indexOfCheckedListItem){
                                indexOfSelectedSpan --;
                            }
                            isHighlighted = true;

                        }
                    }else {
                        ((TextView) arg1.findViewById(R.id.contact_display_name)).setTextColor(getPrimaryTextColor());
                        ((TextView) arg1.findViewById(R.id.contact_jid)).setTextColor(getPrimaryTextColor());

                        //get the string to remove from edittext view
                        Jid jidToRemove = ((ListItem) arg0.getAdapter().getItem(position)).getJid();
                        textIsUserInput = false;
                        int indexToRemove = checkIndexFromJid(jidToRemove);
                        deleteSpan(indexToRemove,1);
                        contactSpanArrayList.remove(indexToRemove);
                        indexOfSelectedSpan = -1;
                        toContactEditText.setSelection(toContactEditText.length());
                    }

                }
                else {

                    if(isHighlighted){
                        deleteSpan(indexOfSelectedSpan);
                        contactSpanArrayList.get(indexOfSelectedSpan).setCheckbox();
                        contactSpanArrayList.remove(indexOfSelectedSpan);
                        indexOfSelectedSpan = -1;
                    }

                    contactSpanArrayList.add(new ContactSpan(currentContact,arg1,getPrimaryTextColor()));

                    if (currentSearchString.length() > 0) {
                        String temp;
                        //Get only the current typed string
                        String newText = currentText.substring(0, currentText.length() - currentSearchString.length());
                        temp = newText;
                        //remove the typed string from edit field
                        toContactEditText.setText(newText);
                        temp = temp + ((ListItem) arg0.getAdapter().getItem(position)).getJid();

                        spanStringBuilder.append(temp);
                        textIsUserInput = false;
                        spanStringBuilder.append(delimiter);
                        populateEdittext(spanStringBuilder.toString());
                        hasTypedChars = true;


                    } else {

                        spanStringBuilder.append(((ListItem) arg0.getAdapter().getItem(position)).getDisplayName() + "");
                        populateEdittext(toContactEditText.getText()+spanStringBuilder.toString());
                    }



                    //selectedContacts.add(((ListItem) arg0.getAdapter().getItem(position)).getJid().toString());


                    //clear current search string
                    currentSearchString.replace(0, currentSearchString.length(), "");
                    spanStringBuilder.replace(0, spanStringBuilder.length(), "");
                    //set cursor to the end of edit text
                    toContactEditText.setSelection(toContactEditText.length());

                    ((TextView) arg1.findViewById(R.id.contact_display_name)).setTextColor(0xFF3366BB);
                    ((TextView) arg1.findViewById(R.id.contact_jid)).setTextColor(0xFF3366BB);
                    hideKeyboard();
                }
                selectorChkbox.setChecked(!selectorChkbox.isChecked());

                if(!clickedContactsList.contains(currentContact)) {
                    clickedContactsList.add(currentContact);
                }
                if(!clickedViewList.contains(arg1)){
                    clickedViewList.add(arg1);
                }
                currentContact.setChecked(!(currentContact.getIsChecked()));

                //mContactsAdapter.notifyDataSetChanged();
                //openConversationForContact(position);

                if(hasTypedChars){
                    hasTypedChars = false;
                    //Clear the search results
                    filterContacts("");
                }
            }
        });


    }


    @Override
    public boolean onNavigateUp() {
        clearData();

        return super.onNavigateUp();
    }

    private void clearData(){
        Iterator<Contact> contactIterator = clickedContactsList.iterator();
        Iterator<View> checkBoxIterator = clickedViewList.iterator();
        Iterator<ContactSpan> contactSpanIterator = contactSpanArrayList.iterator();
        while(contactSpanIterator.hasNext()){
            contactSpanIterator.next().getContact().setChecked(false);
        }
        while(contactIterator.hasNext()){
            contactIterator.next().setChecked(false);
        }
        while(checkBoxIterator.hasNext()){
            ((CheckBox)checkBoxIterator.next().findViewById(R.id.selected_checkbox)).setChecked(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        final int theme = findTheme();
        if (this.mTheme != theme) {
            recreate();
        } else {
            askForContactsPermissions();
        }
    }

    @Override
    public void onStop() {
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
        }
        super.onStop();
    }

    protected void openConversationForContact(int position) {
        Log.d("debug","openConversationForContact");
        Contact contact = (Contact) contacts.get(position);
        openConversationForContact(contact);
    }

    protected void openConversationForContact(Contact contact) {
        Conversation conversation = xmppConnectionService
                .findOrCreateConversation(contact.getAccount(),
                        contact.getJid(), false);
        switchToConversation(conversation);
    }

    protected void openConversationForContact(Contact contact,String msg) {
        Conversation conversation = xmppConnectionService
                .findOrCreateConversation(contact.getAccount(),
                        contact.getJid(), false);
        switchToConversation(conversation,msg,false);
    }

    protected void openConversationForContact() {
        int position = contact_context_id;
        openConversationForContact(position);
    }

    protected void openConversationForBookmark() {
        openConversationForBookmark(conference_context_id);
    }

    protected void openConversationForBookmark(int position) {
        Bookmark bookmark = (Bookmark) conferences.get(position);
        openConversationsForBookmark(bookmark);
    }

    protected void openConversationsForBookmark(Bookmark bookmark) {
        Jid jid = bookmark.getJid();
        if (jid == null) {
            Toast.makeText(this, R.string.invalid_jid, Toast.LENGTH_SHORT).show();
            return;
        }
        Conversation conversation = xmppConnectionService.findOrCreateConversation(bookmark.getAccount(), jid, true);
        conversation.setBookmark(bookmark);
        if (!conversation.getMucOptions().online()) {
            xmppConnectionService.joinMuc(conversation);
        }
        if (!bookmark.autojoin() && getPreferences().getBoolean("autojoin", true)) {
            bookmark.setAutojoin(true);
            xmppConnectionService.pushBookmarks(bookmark.getAccount());
        }
        switchToConversation(conversation);
    }

    protected void openDetailsForContact() {
        int position = contact_context_id;
        Contact contact = (Contact) contacts.get(position);
        switchToContactDetails(contact);
    }

    protected void toggleContactBlock() {
        final int position = contact_context_id;
        BlockContactDialog.show(this, xmppConnectionService, (Contact) contacts.get(position));
    }

    protected void deleteContact() {
        final int position = contact_context_id;
        final Contact contact = (Contact) contacts.get(position);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setTitle(R.string.action_delete_contact);
        builder.setMessage(getString(R.string.remove_contact_text,
                contact.getJid()));
        builder.setPositiveButton(delete, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                xmppConnectionService.deleteContactOnServer(contact);
                filter(mSearchEditText.getText().toString());
            }
        });
        builder.create().show();
    }

    protected void deleteConference() {
        int position = conference_context_id;
        final Bookmark bookmark = (Bookmark) conferences.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setTitle(R.string.delete_bookmark);
        builder.setMessage(getString(R.string.remove_bookmark_text,
                bookmark.getJid()));
        builder.setPositiveButton(delete, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                bookmark.unregisterConversation();
                Account account = bookmark.getAccount();
                account.getBookmarks().remove(bookmark);
                xmppConnectionService.pushBookmarks(account);
                filter(mSearchEditText.getText().toString());
            }
        });
        builder.create().show();

    }

    @SuppressLint("InflateParams")
    protected void showCreateContactDialog(final String prefilledJid, final Invite invite) {
        EnterJidDialog dialog = new EnterJidDialog(
                this, mKnownHosts, mActivatedAccounts,
                getString(R.string.create_contact), getString(R.string.create),
                prefilledJid, null, invite == null || !invite.hasFingerprints()
        );

        dialog.setOnEnterJidDialogPositiveListener(new EnterJidDialog.OnEnterJidDialogPositiveListener() {
            @Override
            public boolean onEnterJidDialogPositive(Jid accountJid, Jid contactJid) throws EnterJidDialog.JidError {
                if (!xmppConnectionServiceBound) {
                    return false;
                }

                final Account account = xmppConnectionService.findAccountByJid(accountJid);
                if (account == null) {
                    return true;
                }

                final Contact contact = account.getRoster().getContact(contactJid);
                if (contact.showInRoster()) {
                    throw new EnterJidDialog.JidError(getString(R.string.contact_already_exists));
                } else {
                    xmppConnectionService.createContact(contact);
                    if (invite != null && invite.hasFingerprints()) {
                        xmppConnectionService.verifyFingerprints(contact,invite.getFingerprints());
                    }
                    switchToConversation(contact, invite == null ? null : invite.getBody());
                    return true;
                }
            }
        });

        mCurrentDialog = dialog.show();
    }

    @SuppressLint("InflateParams")
    protected void showJoinConferenceDialog(final String prefilledJid) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.join_conference);
        final View dialogView = getLayoutInflater().inflate(R.layout.join_conference_dialog, null);
        final Spinner spinner = (Spinner) dialogView.findViewById(account);
        final AutoCompleteTextView jid = (AutoCompleteTextView) dialogView.findViewById(R.id.jid);
        final TextView jabberIdDesc = (TextView) dialogView.findViewById(R.id.jabber_id);
        jabberIdDesc.setText(R.string.conference_address);
        jid.setHint(R.string.conference_address_example);
        jid.setAdapter(new KnownHostsAdapter(this, R.layout.simple_list_item, mKnownConferenceHosts));
        if (prefilledJid != null) {
            jid.append(prefilledJid);
        }
        populateAccountSpinner(this, mActivatedAccounts, spinner);
        final Checkable bookmarkCheckBox = (CheckBox) dialogView
                .findViewById(R.id.bookmark);
        builder.setView(dialogView);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.join, null);
        final AlertDialog dialog = builder.create();
        dialog.show();
        mCurrentDialog = dialog;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        if (!xmppConnectionServiceBound) {
                            return;
                        }
                        final Account account = getSelectedAccount(spinner);
                        if (account == null) {
                            return;
                        }
                        final Jid conferenceJid;
                        try {
                            conferenceJid = Jid.fromString(jid.getText().toString());
                        } catch (final InvalidJidException e) {
                            jid.setError(getString(R.string.invalid_jid));
                            return;
                        }

                        if (bookmarkCheckBox.isChecked()) {
                            if (account.hasBookmarkFor(conferenceJid)) {
                                jid.setError(getString(R.string.bookmark_already_exists));
                            } else {
                                final Bookmark bookmark = new Bookmark(account, conferenceJid.toBareJid());
                                bookmark.setAutojoin(getPreferences().getBoolean("autojoin", true));
                                String nick = conferenceJid.getResourcepart();
                                if (nick != null && !nick.isEmpty()) {
                                    bookmark.setNick(nick);
                                }
                                account.getBookmarks().add(bookmark);
                                xmppConnectionService.pushBookmarks(account);
                                final Conversation conversation = xmppConnectionService
                                        .findOrCreateConversation(account,
                                                conferenceJid, true);
                                conversation.setBookmark(bookmark);
                                if (!conversation.getMucOptions().online()) {
                                    xmppConnectionService.joinMuc(conversation);
                                }
                                dialog.dismiss();
                                mCurrentDialog = null;
                                switchToConversation(conversation);
                            }
                        } else {
                            final Conversation conversation = xmppConnectionService
                                    .findOrCreateConversation(account,
                                            conferenceJid, true);
                            if (!conversation.getMucOptions().online()) {
                                xmppConnectionService.joinMuc(conversation);
                            }
                            dialog.dismiss();
                            mCurrentDialog = null;
                            switchToConversation(conversation);
                        }
                    }
                });
    }

    private void showCreateConferenceDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.create_conference);
        final View dialogView = getLayoutInflater().inflate(R.layout.create_conference_dialog, null);
        final Spinner spinner = (Spinner) dialogView.findViewById(account);
        //final EditText subject = (EditText) dialogView.findViewById(subject);
        populateAccountSpinner(this, mActivatedAccounts, spinner);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.choose_participants, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!xmppConnectionServiceBound) {
                    return;
                }
                final Account account = getSelectedAccount(spinner);
                if (account == null) {
                    return;
                }
                Intent intent = new Intent(getApplicationContext(), ChooseContactActivity.class);
                intent.putExtra("multiple", true);
                intent.putExtra("show_enter_jid", true);
                //intent.putExtra("subject", subject.getText().toString());
                intent.putExtra(EXTRA_ACCOUNT, account.getJid().toBareJid().toString());
                intent.putExtra(ChooseContactActivity.EXTRA_TITLE_RES_ID, R.string.choose_participants);
                startActivityForResult(intent, REQUEST_CREATE_CONFERENCE);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        mCurrentDialog = builder.create();
        mCurrentDialog.show();
    }

    private Account getSelectedAccount(Spinner spinner) {
        if (!spinner.isEnabled()) {
            return null;
        }
        Jid jid;
        try {
            if (Config.DOMAIN_LOCK != null) {
                jid = Jid.fromParts((String) spinner.getSelectedItem(), Config.DOMAIN_LOCK, null);
            } else {
                jid = Jid.fromString((String) spinner.getSelectedItem());
            }
        } catch (final InvalidJidException e) {
            return null;
        }
        return xmppConnectionService.findAccountByJid(jid);
    }

    protected void switchToConversation(Contact contact, String body) {
        Conversation conversation = xmppConnectionService
                .findOrCreateConversation(contact.getAccount(),
                        contact.getJid(), false);
        switchToConversation(conversation, body, false);
    }

    public static void populateAccountSpinner(Context context, List<String> accounts, Spinner spinner) {
        if (accounts.size() > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.simple_list_item, accounts);
            adapter.setDropDownViewResource(R.layout.simple_list_item);
            spinner.setAdapter(adapter);
            spinner.setEnabled(true);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                    R.layout.simple_list_item,
                    Arrays.asList(new String[]{context.getString(R.string.no_accounts)}));
            adapter.setDropDownViewResource(R.layout.simple_list_item);
            spinner.setAdapter(adapter);
            spinner.setEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.start_conversation, menu);
        MenuItem menuCreateContact = menu.findItem(R.id.action_create_contact);
        //MenuItem menuCreateConference = menu.findItem(R.id.action_conference);
        MenuItem menuHideOffline = menu.findItem(R.id.action_hide_offline);
        menuHideOffline.setChecked(this.mHideOfflineContacts);

        // mMenuSearchView = menu.findItem(R.id.action_search);
        /// mMenuSearchView.setOnActionExpandListener(mOnActionExpandListener);
//        View mSearchView = mMenuSearchView.getActionView();
//        mSearchEditText = (EditText) mSearchView
//                .findViewById(R.id.search_field);

//        mSearchEditText.addTextChangedListener(mSearchTextWatcher);
//        mSearchEditText.setOnEditorActionListener(mSearchDone);

        toContactEditText.addTextChangedListener(mSearchTextWatcher);
        //toContactEditText.setOnEditorActionListener(mSearchDone);
//        toContactEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
//                Log.d("debug","oneditoraction");
//                return false;
//            }
//        });
        menuCreateContact.setVisible(true);
       // menuCreateContact.setVisible(false);

//        if (getActionBar().getSelectedNavigationIndex() == 0) {
//            menuCreateConference.setVisible(false);
//        } else {
//            menuCreateContact.setVisible(false);
//        }
        if (mInitialJid != null) {
            //mMenuSearchView.expandActionView();
            mSearchEditText.append(mInitialJid);
            filter(mInitialJid);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_contact:
                Intent intent = new Intent(getApplicationContext(), CreateContactActivity.class);
                intent.putExtra("uri", xmppConnectionService.getAccounts().get(0).getShareableUri());
                intent.putExtra("displayName", xmppConnectionService.getAccounts().get(0).getDisplayName());
                intent.putExtra("jid", xmppConnectionService.getAccounts().get(0).getJid().toString());
                startActivityForResult(intent,REQUEST_CREATE_CONTACT);
                //showCreateContactDialog(null, null);
                return true;
//            case R.id.action_join_conference:
//                showJoinConferenceDialog(null);
//                return true;
//            case R.id.action_create_conference:
//                showCreateConferenceDialog();
//                return true;
            case R.id.action_scan_qr_code:
                //new IntentIntegrator(this).initiateScan(Arrays.asList("AZTEC","QR_CODE"));
                return true;
            case R.id.action_hide_offline:
                mHideOfflineContacts = !item.isChecked();
                getPreferences().edit().putBoolean("hide_offline", mHideOfflineContacts).commit();
                if (mSearchEditText != null) {
                    filter(mSearchEditText.getText().toString());
                }
                invalidateOptionsMenu();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //textIsUserInput = true;
        if(keyCode == KeyEvent.KEYCODE_DEL && spanArrayList.size() > 0) {
            if(isHighlighted){
                if(indexOfSelectedSpan != -1) {
                    //Log.d("debug","deleting span "+indexOfSelectedSpan);
                    deleteSpan(indexOfSelectedSpan);

                    contactSpanArrayList.get(indexOfSelectedSpan).setCheckbox();
                    contactSpanArrayList.remove(indexOfSelectedSpan);
                }

                isHighlighted = false;
                indexOfSelectedSpan = -1;
            }

            if(delimiterDeleted){
                isHighlighted = true;
                toContactEditText.overrideDel = true;
                delimiterDeleted= false;
                //Log.d("debug","overrideDel true at delimiterDeleted");
            }
            else {
                isHighlighted = false;
            }
            return true;
        }

//        if (keyCode == KeyEvent.KEYCODE_SEARCH && !event.isLongPress()) {
//            openSearch();
//            return true;
//        }
//        int c = event.getUnicodeChar();
//        if (c > 32) {
//            if (mSearchEditText != null && !mSearchEditText.isFocused()) {
//                openSearch();
//                mSearchEditText.append(Character.toString((char) c));
//                return true;
//            }
//        }
        return super.onKeyUp(keyCode, event);
    }

//    private void openSearch() {
//        if (mMenuSearchView != null) {
//            mMenuSearchView.expandActionView();
//        }
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == REQUEST_CREATE_CONTACT){
                String data  = intent.getStringExtra("qrCodeValue");
                Invite invite = new Invite(data);
                if (xmppConnectionServiceBound) {
                    if(!invite.invite()){
                        Toast.makeText(this,"Invalid ID",Toast.LENGTH_SHORT).show();
                    }
                } else if (invite.getJid() != null) {
                    this.mPendingInvite = invite;
                } else {
                    this.mPendingInvite = null;
                }

        }
//        if ((requestCode & 0xFFFF) == IntentIntegrator.REQUEST_CODE) {
//            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
//            if (scanResult != null && scanResult.getFormatName() != null) {
//                String data = scanResult.getContents();
//                Invite invite = new Invite(data);
//                if (xmppConnectionServiceBound) {
//                    invite.invite();
//                } else if (invite.getJid() != null) {
//                    this.mPendingInvite = invite;
//                } else {
//                    this.mPendingInvite = null;
//                }
//            }
//        } else
        if (resultCode == RESULT_OK) {
            if (xmppConnectionServiceBound) {
                this.mPostponedActivityResult = null;
                if (requestCode == REQUEST_CREATE_CONFERENCE) {
                    Account account = extractAccount(intent);
                    final String subject = intent.getStringExtra("subject");
                    List<Jid> jids = new ArrayList<>();
                    if (intent.getBooleanExtra("multiple", false)) {
                        String[] toAdd = intent.getStringArrayExtra("contacts");
                        for (String item : toAdd) {
                            try {
                                jids.add(Jid.fromString(item));
                            } catch (InvalidJidException e) {
                                //ignored
                            }
                        }
                    } else {
                        try {
                            jids.add(Jid.fromString(intent.getStringExtra("contact")));
                        } catch (Exception e) {
                            //ignored
                        }
                    }
                    if (account != null && jids.size() > 0) {
                        xmppConnectionService.createAdhocConference(account, subject, jids, mAdhocConferenceCallback);
                        mToast = Toast.makeText(this, R.string.creating_conference, Toast.LENGTH_LONG);
                        mToast.show();
                    }
                }
            } else {
                this.mPostponedActivityResult = new Pair<>(requestCode, intent);
            }
        }
        super.onActivityResult(requestCode, requestCode, intent);
    }

    private void askForContactsPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                if (mRequestedContactsPermission.compareAndSet(false, true)) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.sync_with_contacts);
                        builder.setMessage(R.string.sync_with_contacts_long);
                        builder.setPositiveButton(R.string.next, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_SYNC_CONTACTS);
                                }
                            }
                        });
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_SYNC_CONTACTS);
                                    }
                                }
                            });
                        }
                        builder.create().show();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 0);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == REQUEST_SYNC_CONTACTS && xmppConnectionServiceBound) {
                    xmppConnectionService.loadPhoneContacts();
                }
            }
    }



    @Override
    protected void onBackendConnected() {
        if (mPostponedActivityResult != null) {
            onActivityResult(mPostponedActivityResult.first, RESULT_OK, mPostponedActivityResult.second);
            this.mPostponedActivityResult = null;
        }
        this.mActivatedAccounts.clear();
        for (Account account : xmppConnectionService.getAccounts()) {
            if (account.getStatus() != Account.State.DISABLED) {
                if (Config.DOMAIN_LOCK != null) {
                    this.mActivatedAccounts.add(account.getJid().getLocalpart());
                } else {
                    this.mActivatedAccounts.add(account.getJid().toBareJid().toString());
                }
            }
        }
        final Intent intent = getIntent();
        final ActionBar ab = getActionBar();
        boolean init = intent != null && intent.getBooleanExtra("init", false);
        boolean noConversations = xmppConnectionService.getConversations().size() == 0;
        if ((init || noConversations) && ab != null) {
            ab.setDisplayShowHomeEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setHomeButtonEnabled(false);
        }
        this.mKnownHosts = xmppConnectionService.getKnownHosts();
        this.mKnownConferenceHosts = xmppConnectionService.getKnownConferenceHosts();
        if (this.mPendingInvite != null) {
            mPendingInvite.invite();
            this.mPendingInvite = null;
            filter(null);
        } else if (!handleIntent(getIntent())) {
            if (mSearchEditText != null) {
                filter(mSearchEditText.getText().toString());
            } else {
                filter(null);
            }
        } else {
            filter(null);
        }
        setIntent(null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    Invite getInviteJellyBean(NdefRecord record) {
        return new Invite(record.toUri());
    }

    protected boolean handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return false;
        }
        switch (intent.getAction()) {
            case Intent.ACTION_SENDTO:
            case Intent.ACTION_VIEW:
                Uri uri = intent.getData();
                if (uri != null) {
                    return new Invite(intent.getData(),false).invite();
                } else {
                    return false;
                }
            case NfcAdapter.ACTION_NDEF_DISCOVERED:
                for (Parcelable message : getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)) {
                    if (message instanceof NdefMessage) {
                        for (NdefRecord record : ((NdefMessage) message).getRecords()) {
                            switch (record.getTnf()) {
                                case NdefRecord.TNF_WELL_KNOWN:
                                    if (Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                            return getInviteJellyBean(record).invite();
                                        } else {
                                            byte[] payload = record.getPayload();
                                            if (payload[0] == 0) {
                                                return new Invite(Uri.parse(new String(Arrays.copyOfRange(
                                                        payload, 1, payload.length)))).invite();
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
        }
        return false;
    }

    private boolean handleJid(Invite invite) {
        Account account = xmppConnectionService.findAccountByJid(invite.getJid());
        if (account != null && !account.isOptionSet(Account.OPTION_DISABLED) && invite.hasFingerprints()) {
            if (xmppConnectionService.verifyFingerprints(account,invite.getFingerprints())) {
                switchToAccount(account);
                finish();
                return true;
            }
        }
        List<Contact> contacts = xmppConnectionService.findContacts(invite.getJid());
        if (invite.isMuc()) {
            Conversation muc = xmppConnectionService.findFirstMuc(invite.getJid());
            if (muc != null) {
                switchToConversation(muc,invite.getBody(),false);
                return true;
            } else {
                showJoinConferenceDialog(invite.getJid().toBareJid().toString());
                return false;
            }
        } else if (contacts.size() == 0) {
            showCreateContactDialog(invite.getJid().toString(), invite);
            return false;
        } else if (contacts.size() == 1) {
            Contact contact = contacts.get(0);
            if (!invite.isSafeSource() && invite.hasFingerprints()) {
                displayVerificationWarningDialog(contact,invite);
            } else {
                if (invite.hasFingerprints()) {
                    xmppConnectionService.verifyFingerprints(contact, invite.getFingerprints());
                }
                switchToConversation(contact, invite.getBody());
            }
            return true;
        } else {
//            if (mMenuSearchView != null) {
//                mMenuSearchView.expandActionView();
//                mSearchEditText.setText("");
//                mSearchEditText.append(invite.getJid().toString());
//                filter(invite.getJid().toString());
//            } else {
//                mInitialJid = invite.getJid().toString();
//            }
            return true;
        }
    }

    private void displayVerificationWarningDialog(final Contact contact, final Invite invite) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.verify_omemo_keys);
        View view = getLayoutInflater().inflate(R.layout.dialog_verify_fingerprints, null);
        final CheckBox isTrustedSource = (CheckBox) view.findViewById(R.id.trusted_source);
        TextView warning = (TextView) view.findViewById(R.id.warning);
        String jid = contact.getJid().toBareJid().toString();
        SpannableString spannable = new SpannableString(getString(R.string.verifying_omemo_keys_trusted_source,jid,contact.getDisplayName()));
        int start = spannable.toString().indexOf(jid);
        if (start >= 0) {
            spannable.setSpan(new TypefaceSpan("monospace"),start,start + jid.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        warning.setText(spannable);
        builder.setView(view);
        builder.setPositiveButton(R.string.confirm, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isTrustedSource.isChecked() && invite.hasFingerprints()) {
                    xmppConnectionService.verifyFingerprints(contact, invite.getFingerprints());
                }
                switchToConversation(contact, invite.getBody());
            }
        });
        builder.setNegativeButton(R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                StartConversationActivity.this.finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                StartConversationActivity.this.finish();
            }
        });
        dialog.show();
    }

    protected void filter(String needle) {
        if (xmppConnectionServiceBound) {
            this.filterContacts(needle);
            this.filterConferences(needle);
        }
    }

    protected void filterContacts(String needle) {
        this.contacts.clear();
        for (Account account : xmppConnectionService.getAccounts()) {
            if (account.getStatus() != Account.State.DISABLED) {
                for (Contact contact : account.getRoster().getContacts()) {
                    Presence.Status s = contact.getShownStatus();
                    if (contact.showInRoster() && contact.match(this, needle)
                            && (!this.mHideOfflineContacts
                            || (needle != null && !needle.trim().isEmpty())
                            || s.compareTo(Presence.Status.OFFLINE) < 0)) {
                        this.contacts.add(contact);
                    }
                }
            }
        }
        Collections.sort(this.contacts);
        mContactsAdapter.notifyDataSetChanged();

    }

    protected void filterConferences(String needle) {
        this.conferences.clear();
        for (Account account : xmppConnectionService.getAccounts()) {
            if (account.getStatus() != Account.State.DISABLED) {
                for (Bookmark bookmark : account.getBookmarks()) {
                    if (bookmark.match(this, needle)) {
                        this.conferences.add(bookmark);
                    }
                }
            }
        }
        Collections.sort(this.conferences);
    }

    private int checkSpanIndexFromText(Spannable spannable,Object o){
        RoundedBackgroundSpan[] bgSpan = spannable.getSpans(0,spannable.length(),RoundedBackgroundSpan.class);
        for(int i = 0; i < bgSpan.length; i++){
            if(bgSpan[i].getSpanStart() == spannable.getSpanStart(o) && bgSpan[i].getSpanEnd() == spannable.getSpanEnd(o)){
                return i;
            }
        }
        return 0;
    }

    private int checkIndexFromJid(Jid jid){

        for(int i = 0; i < contactSpanArrayList.size(); i++){
            if(contactSpanArrayList.get(i).getJid().equals(jid)){
                Log.d("debug","jid "+contactSpanArrayList.get(i).getJid()+" is "+jid+"? index="+i);
                return i;
            }
        }
        return 0;
    }



    private void populateEdittext(final String text){
        //Log.d("debug","populate this text"+text);
        textIsUserInput = false;
        toContactEditText.setText("", TextView.BufferType.SPANNABLE);
        spanArrayList.clear();
        String[] splitStringArr = text.split(",");
        for (String contactStr:splitStringArr) {

            final RoundedBackgroundSpan bgSpan = new RoundedBackgroundSpan(StartConversationActivity.this);
            spanArrayList.add(bgSpan);
            Log.d("spanarray","spanarray added in populate "+spanArrayList.size());

            SpannableStringBuilder spanStringBuilder = new SpannableStringBuilder(contactStr);

            spanStringBuilder.setSpan(bgSpan, 0, contactStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
            spanStringBuilder.setSpan(boldSpan,0,contactStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            //spanStringBuilder.setSpan(new ForegroundColorSpan(Color.BLUE),0,contactStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            spanStringBuilder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    textIsUserInput = false;
                    EditText spanTextView= (EditText) view;
                    Spannable spanText = spanTextView.getText();
                    Editable editable = toContactEditText.getText();
                    int indexOfCurrentSpan = checkSpanIndexFromText(spanText,this);

                    if(isHighlighted && indexOfSelectedSpan!=indexOfCurrentSpan && indexOfSelectedSpan != -1){
                        textIsUserInput = false;
                        editable.insert(spanText.getSpanEnd(spanArrayList.get(indexOfSelectedSpan)),",");
                        //clear the previous span first
                        spanArrayList.get(indexOfSelectedSpan).paintText();
                    }
                    //if removing current highlighted span
                    if(isHighlighted && indexOfSelectedSpan == indexOfCurrentSpan){
                        textIsUserInput = false;
                        editable.insert(spanText.getSpanEnd(this),",");
                        toContactEditText.setCursorVisible(true);
                        isHighlighted = false;
                        toContactEditText.overrideDel = false;

                        //Log.d("debug","overrideDel false at onClick");
                    }else{
                        //if clicking on current span
                        textIsUserInput = false;
                        editable.delete(spanText.getSpanEnd(this),spanText.getSpanEnd(this)+1);
                        toContactEditText.setCursorVisible(false);
                        isHighlighted = true;
                        toContactEditText.overrideDel = true;
                        //Log.d("debug","overrideDel true at onClick");
                        //Log.d("debug","delete ,");
                    }

                    //Log.d("debug","indexOfSelectedSpan ="+indexOfSelectedSpan+" clicked index = "+selectedContacts.indexOf(toCheck));
                    indexOfSelectedSpan = indexOfCurrentSpan;
                    spanArrayList.get(indexOfSelectedSpan).paintText();

                    // Log.d("debug","span from"+spanText.getSpanStart(spanArrayList.get(indexOfSelectedSpan))+" to "+spanText.getSpanEnd(spanArrayList.get(indexOfSelectedSpan)));
                    //Forcefully refresh edittext to allow redraw of new color
                    if(delimiterDeleted){
                        //editable.append(',');
                        delimiterDeleted = false;
                        //Log.d("debug","delimiter added");
                    }
                    textIsUserInput = false;
                    //Log.d("debug","textIsUserInput at onclick = "+textIsUserInput);
                    toContactEditText.setText(editable,TextView.BufferType.SPANNABLE);




                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setUnderlineText(false);
                    super.updateDrawState(ds);
                }


            },0,contactStr.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if(spanStringBuilder.toString().length() > 0) {
                textIsUserInput = false;
                toContactEditText.append(spanStringBuilder);
                textIsUserInput = false;
                toContactEditText.append(",");
            }

        }

        toContactEditText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void deleteSpan (int spanToDel){
        Editable editableText =toContactEditText.getText();
        //delete causes textchange ?
        textIsUserInput = false;
        editableText.delete(spanArrayList.get(spanToDel).getSpanStart(),spanArrayList.get(spanToDel).getSpanEnd());
        textIsUserInput = false;
        toContactEditText.setText(editableText);
        toContactEditText.setMovementMethod(LinkMovementMethod.getInstance());

        isHighlighted=false;
        toContactEditText.setCursorVisible(true);
        toContactEditText.overrideDel = false;

        spanArrayList.remove(spanToDel);
        clickedViewList.remove(spanToDel);
        clickedContactsList.remove(spanToDel);
    }

    private void deleteSpan (int spanToDel,int delChars){
        Editable editableText =toContactEditText.getText();
        //delete causes textchange ?
        textIsUserInput = false;
        editableText.delete(spanArrayList.get(spanToDel).getSpanStart(),spanArrayList.get(spanToDel).getSpanEnd()+delChars);
        textIsUserInput = false;
        toContactEditText.setText(editableText);
        toContactEditText.setMovementMethod(LinkMovementMethod.getInstance());

        isHighlighted=false;
        toContactEditText.setCursorVisible(true);
        toContactEditText.overrideDel = false;

        spanArrayList.remove(spanToDel);
        clickedViewList.remove(spanToDel);
        clickedContactsList.remove(spanToDel);
    }

//    private void onTabChanged() {
//        invalidateOptionsMenu();
//    }

    @Override
    public void OnUpdateBlocklist(final Status status) {
        refreshUi();
    }

    @Override
    protected void refreshUiReal() {
        if (mSearchEditText != null) {
            filter(mSearchEditText.getText().toString());
        }
    }

//    public class ListPagerAdapter extends PagerAdapter {
//        FragmentManager fragmentManager;
//        MyListFragment[] fragments;
//
//        public ListPagerAdapter(FragmentManager fm) {
//            fragmentManager = fm;
//            fragments = new MyListFragment[2];
//        }
//
//        public void requestFocus(int pos) {
//            if (fragments.length > pos) {
//                fragments[pos].getListView().requestFocus();
//            }
//        }
//
//        @Override
//        public void destroyItem(ViewGroup container, int position, Object object) {
//            assert (0 <= position && position < fragments.length);
//            FragmentTransaction trans = fragmentManager.beginTransaction();
//            trans.remove(fragments[position]);
//            trans.commit();
//            fragments[position] = null;
//        }
//
//        @Override
//        public Fragment instantiateItem(ViewGroup container, int position) {
//            Fragment fragment = getItem(position);
//            FragmentTransaction trans = fragmentManager.beginTransaction();
//            trans.add(container.getId(), fragment, "fragment:" + position);
//            trans.commit();
//            return fragment;
//        }
//
//        @Override
//        public int getCount() {
//            return fragments.length;
//        }
//
//        @Override
//        public boolean isViewFromObject(View view, Object fragment) {
//            return ((Fragment) fragment).getView() == view;
//        }
//
//        public Fragment getItem(int position) {
//            assert (0 <= position && position < fragments.length);
//            if (fragments[position] == null) {
//                final MyListFragment listFragment = new MyListFragment();
//                if (position == 1) {
//                    listFragment.setListAdapter(mConferenceAdapter);
//                    listFragment.setContextMenu(R.menu.conference_context);
//                    listFragment.setOnListItemClickListener(new OnItemClickListener() {
//
//                        @Override
//                        public void onItemClick(AdapterView<?> arg0, View arg1,
//                                                int position, long arg3) {
//                            openConversationForBookmark(position);
//                        }
//                    });
//                } else {
//
////                    (findViewById(R.id.selected_checkbox)).setOnClickListener(new View.OnClickListener() {
////                        @Override
////                        public void onClick(View view) {
////                            Log.d("debug","onclickcheckbox");
////                        }
////                    });
//
//
//                    listFragment.setListAdapter(mContactsAdapter);
//                    listFragment.setContextMenu(R.menu.contact_context);
//                    listFragment.setOnListItemClickListener(new OnItemClickListener() {
//
//                        @Override
//                        //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
//                        public void onItemClick(AdapterView<?> arg0, View arg1,
//                                                int position, long arg3) {
//
//                            StringBuilder currentText = new StringBuilder(toContactEditText.getText().toString());
//                            Spannable spannableText = toContactEditText.getText();
//                            SpannableStringBuilder spanStringBuilder = new SpannableStringBuilder();
//                            CheckBox selectorChkbox = (CheckBox)(arg1.findViewById(R.id.selected_checkbox));
//                            //workaround for making view clickable
//                            selectorChkbox.setFocusable(false);
//                            RoundedBackgroundSpan bgSpan = new RoundedBackgroundSpan(StartConversationActivity.this);
//                            Contact currentContact = (Contact) arg0.getAdapter().getItem(position);
//                            int indexOfCheckedListItem = clickedViewList.indexOf(arg1);
//                            boolean hasTypedChars = true;
//
//                            if(selectorChkbox.isChecked()){
//                                if(isHighlighted) {
//                                    if(indexOfSelectedSpan == indexOfCheckedListItem) {
//                                        deleteSpan(indexOfSelectedSpan);
//                                        contactSpanArrayList.remove(indexOfSelectedSpan);
//                                        indexOfSelectedSpan = -1;
//                                    }
//                                    else {
//                                        deleteSpan(indexOfCheckedListItem,1);
//                                        contactSpanArrayList.get(indexOfCheckedListItem).setCheckbox(true);
//                                        contactSpanArrayList.remove(indexOfCheckedListItem);
//
//                                        if(indexOfSelectedSpan > indexOfCheckedListItem){
//                                            indexOfSelectedSpan --;
//                                        }
//                                        isHighlighted = true;
//
//                                    }
//                                }else {
//                                    ((TextView) arg1.findViewById(R.id.contact_display_name)).setTextColor(getPrimaryTextColor());
//                                    ((TextView) arg1.findViewById(R.id.contact_jid)).setTextColor(getPrimaryTextColor());
//
//                                    //get the string to remove from edittext view
//                                    Jid jidToRemove = ((ListItem) arg0.getAdapter().getItem(position)).getJid();
//                                    textIsUserInput = false;
//                                    int indexToRemove = checkIndexFromJid(jidToRemove);
//                                    deleteSpan(indexToRemove,1);
//                                    contactSpanArrayList.remove(indexToRemove);
//                                    indexOfSelectedSpan = -1;
//                                    toContactEditText.setSelection(toContactEditText.length());
//                                }
//
//                            }
//                            else {
//
//                                if(isHighlighted){
//                                    deleteSpan(indexOfSelectedSpan);
//                                    contactSpanArrayList.get(indexOfSelectedSpan).setCheckbox();
//                                    contactSpanArrayList.remove(indexOfSelectedSpan);
//                                    indexOfSelectedSpan = -1;
//                                }
//
//                                contactSpanArrayList.add(new ContactSpan(currentContact,arg1,getPrimaryTextColor()));
//
//                                if (currentSearchString.length() > 0) {
//                                    String temp;
//                                    //Get only the current typed string
//                                    String newText = currentText.substring(0, currentText.length() - currentSearchString.length());
//                                    temp = newText;
//                                    //remove the typed string from edit field
//                                    toContactEditText.setText(newText);
//                                    temp = temp + ((ListItem) arg0.getAdapter().getItem(position)).getJid();
//
//                                    spanStringBuilder.append(temp);
//                                    textIsUserInput = false;
//                                    spanStringBuilder.append(delimiter);
//                                    populateEdittext(spanStringBuilder.toString());
//                                    hasTypedChars = true;
//
//
//                                } else {
//
//                                    spanStringBuilder.append(((ListItem) arg0.getAdapter().getItem(position)).getDisplayName() + "");
//                                    populateEdittext(toContactEditText.getText()+spanStringBuilder.toString());
//                                }
//
//
//
//                                //selectedContacts.add(((ListItem) arg0.getAdapter().getItem(position)).getJid().toString());
//
//
//                                //clear current search string
//                                currentSearchString.replace(0, currentSearchString.length(), "");
//                                spanStringBuilder.replace(0, spanStringBuilder.length(), "");
//                                //set cursor to the end of edit text
//                                Log.d("userinput","onclick listfrag before setselection"+textIsUserInput);
//                                toContactEditText.setSelection(toContactEditText.length());
//
//                                Log.d("userinput","onclick listfrag end "+textIsUserInput);
//
//                                ((TextView) arg1.findViewById(R.id.contact_display_name)).setTextColor(0xFF3366BB);
//                                ((TextView) arg1.findViewById(R.id.contact_jid)).setTextColor(0xFF3366BB);
//                                hideKeyboard();
//                            }
//                            selectorChkbox.setChecked(!selectorChkbox.isChecked());
//
//                            if(!clickedContactsList.contains(currentContact)) {
//                                clickedContactsList.add(currentContact);
//                            }
//                            if(!clickedViewList.contains(arg1)){
//                                clickedViewList.add(arg1);
//                            }
//                            currentContact.setChecked(!(currentContact.getIsChecked()));
//
//                            //mContactsAdapter.notifyDataSetChanged();
//                            //openConversationForContact(position);
//
//                            if(hasTypedChars){
//                                hasTypedChars = false;
//                                //Clear the search results
//                                filterContacts("");
//                            }
//                        }
//                    });
//                }
//
//
//                fragments[position] = listFragment;
//            }
//            return fragments[position];
//        }
//    }

//    public static class MyListFragment extends ListFragment {
//        private AdapterView.OnItemClickListener mOnItemClickListener;
//        private int mResContextMenu;
//
//        public void setContextMenu(final int res) {
//            this.mResContextMenu = res;
//        }
//
//        @Override
//        public void onListItemClick(final ListView l, final View v, final int position, final long id) {
//            if (mOnItemClickListener != null) {
//                mOnItemClickListener.onItemClick(l, v, position, id);
//            }
//        }
//
//        public void setOnListItemClickListener(AdapterView.OnItemClickListener l) {
//            this.mOnItemClickListener = l;
//        }
//
//        @Override
//        public void onViewCreated(final View view, final Bundle savedInstanceState) {
//            super.onViewCreated(view, savedInstanceState);
//            registerForContextMenu(getListView());
//            getListView().setFastScrollEnabled(true);
//
//        }
//
//
        @Override
        public void onCreateContextMenu(final ContextMenu menu, final View v,
                                        final ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            getMenuInflater().inflate(R.menu.contact_context, menu);
            final AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
                contact_context_id = acmi.position;
                final Contact contact = (Contact) contacts.get(acmi.position);
                final MenuItem blockUnblockItem = menu.findItem(R.id.context_contact_block_unblock);
                final MenuItem showContactDetailsItem = menu.findItem(R.id.context_contact_details);
                if (contact.isSelf()) {
                    showContactDetailsItem.setVisible(false);
                }
                XmppConnection xmpp = contact.getAccount().getXmppConnection();
                if (xmpp != null && xmpp.getFeatures().blocking() && !contact.isSelf()) {
                    if (contact.isBlocked()) {
                        blockUnblockItem.setTitle(R.string.unblock_contact);
                    } else {
                        blockUnblockItem.setTitle(R.string.block_contact);
                    }
                } else {
                    blockUnblockItem.setVisible(false);
                }

        }
        @Override
        public boolean onContextItemSelected(final MenuItem item) {
            switch (item.getItemId()) {
                case R.id.context_start_conversation:
                    openConversationForContact();
                    break;
                case R.id.context_contact_details:
                    openDetailsForContact();
                    break;
                case R.id.context_contact_block_unblock:
                    toggleContactBlock();
                    break;
                case R.id.context_delete_contact:
                    deleteContact();
                    break;
                case R.id.context_join_conference:
                    openConversationForBookmark();
                    break;
                case R.id.context_delete_conference:
                    deleteConference();
            }
            return true;
        }


    protected class Invite extends XmppUri {

        public Invite(final Uri uri) {
            super(uri);
        }

        public Invite(final String uri) {
            super(uri);
        }

        public Invite(Uri uri, boolean safeSource) {
            super(uri,safeSource);
        }

        boolean invite() {
            if (getJid() != null) {
                return handleJid(this);
            }
            return false;
        }

        public boolean isMuc() {
            return muc;
        }
    }
}
