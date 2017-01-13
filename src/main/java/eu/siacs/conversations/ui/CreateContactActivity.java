package eu.siacs.conversations.ui;


import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import eu.siacs.conversations.R;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.services.BarcodeProvider;
import eu.siacs.conversations.xmpp.jid.Jid;

public class CreateContactActivity extends XmppActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private String uri;
    private String displayName;
    private String jidString;
    private EditText mSearchEditText;

    private final int REQUEST_CREATE_CONTACT = 2359;

    private static final String TAG = "Barcode-reader";

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String BarcodeObject = "Barcode";

    @Override
    protected void onBackendConnected() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActionBar actionBar = getActionBar();
        setContentView(R.layout.activity_create_contact);
        Intent intent = getIntent();
        uri = intent.getExtras().getString("uri");
        displayName = intent.getExtras().getString("displayName");
        jidString = intent.getExtras().getString("jid");

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.create_contact_view_pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        if (actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener codeTabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                // show the given tab
                mViewPager.setCurrentItem(tab.getPosition());
            }

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // hide the given tab
            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event
            }
        };

        actionBar.addTab(actionBar.newTab().setText("Scan Code").setTabListener(codeTabListener));
        actionBar.addTab(actionBar.newTab().setText("My Code").setTabListener(codeTabListener));

        mViewPager = (ViewPager) findViewById(R.id.create_contact_view_pager);
        mViewPager.requestDisallowInterceptTouchEvent(true);
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });
        mViewPager.setCurrentItem(1);
    }

    public String getUri(){
        return uri;
    }
    public String getDisplayName(){
        return displayName;
    }
    public String getJidString(){
        return jidString;
    }

    @Override
    protected void refreshUiReal() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_contact, menu);
        MenuItem menuSearchView = menu.findItem(R.id.action_search);

        final View mSearchView = menuSearchView.getActionView();
        mSearchEditText = (EditText) mSearchView
                .findViewById(R.id.search_field);
        mSearchEditText.addTextChangedListener(mSearchTextWatcher);
        menuSearchView.setOnActionExpandListener(mOnActionExpandListener);
        mSearchEditText.setOnEditorActionListener(mSearchDone);

        return true;

    }
    protected void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();

        if (focus != null) {

            inputManager.hideSoftInputFromWindow(focus.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
        private TextView.OnEditorActionListener mSearchDone = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            try {
            final Account account = xmppConnectionService.findAccountByJid(Jid.fromString(jidString));
            final Contact contact = account.getRoster().getContact(Jid.fromString(v.getText().toString()));
            hideKeyboard();
            //mListPagerAdapter.requestFocus(pos);
                Log.d("debug","Search this: "+contact.getJid().toString());
                if (contact.showInRoster()) {
                    Toast.makeText(getApplicationContext(),contact.getDisplayName()+" already exist!",Toast.LENGTH_SHORT).show();
                    throw new EnterJidDialog.JidError(getString(R.string.contact_already_exists));
                }else {
                    xmppConnectionService.createContact(contact);
                    Toast.makeText(getApplicationContext(),contact.getDisplayName()+" added!",Toast.LENGTH_SHORT).show();
                    finish();
                }
            }catch (Exception e){

            }
            //List<Contact> contacts = xmppConnectionService.findContacts(invite.getJid());
//            if (invite.isMuc()) {
//                Conversation muc = xmppConnectionService.findFirstMuc(invite.getJid());
//                if (muc != null) {
//                    switchToConversation(muc,invite.getBody(),false);
//                    return true;
//                } else {
//                    showJoinConferenceDialog(invite.getJid().toBareJid().toString());
//                    return false;
//                }
//            } else if (contacts.size() == 0) {
//                showCreateContactDialog(invite.getJid().toString(), invite);
//                return false;
//            } else if (contacts.size() == 1) {
//                Contact contact = contacts.get(0);
//                if (!invite.isSafeSource() && invite.hasFingerprints()) {
//                    displayVerificationWarningDialog(contact,invite);
//                } else {
//                    if (invite.hasFingerprints()) {
//                        xmppConnectionService.verifyFingerprints(contact, invite.getFingerprints());
//                    }
//                    switchToConversation(contact, invite.getBody());
//                }
            return true;
        }

    };

    private final TextWatcher mSearchTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(final Editable editable) {
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                                      final int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before,
                                  final int count) {
            if(s.length() != 0){
                //TODO:Change layout to verify
            }
        }
    };

    private final MenuItem.OnActionExpandListener mOnActionExpandListener = new MenuItem.OnActionExpandListener() {

        @Override
        public boolean onMenuItemActionExpand(final MenuItem item) {
            mSearchEditText.post(new Runnable() {

                @Override
                public void run() {
                    mSearchEditText.requestFocus();
                    final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(mSearchEditText,
                            InputMethodManager.SHOW_IMPLICIT);
                }
            });

            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(final MenuItem item) {
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(),
                    InputMethodManager.HIDE_IMPLICIT_ONLY);
            mSearchEditText.setText("");
            return true;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private ImageView qrCodeView;
        private TextView accountName;
        private TextView accountJid;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int position) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            if(position == 1) {
                //new IntentIntegrator(fragment).initiateScan(Arrays.asList("AZTEC", "QR_CODE"));Intent intent = new Intent("com.google.zxing.client.android.SCAN");

            }
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_create_contact, container, false);

            qrCodeView = (ImageView) rootView.findViewById(R.id.qr_code);
            accountName = (TextView) rootView.findViewById(R.id.account_name);
            accountJid = (TextView) rootView.findViewById(R.id.account_jid);
            String uri = ((CreateContactActivity)getActivity()).getUri();
            String displayJID = ((CreateContactActivity) getActivity()).getJidString();
            String displayName = ((CreateContactActivity) getActivity()).getDisplayName();
            displayJID = displayJID.replace("/phone","");
            if(displayName == null){
                displayName = displayJID.substring(0,displayJID.indexOf("@"));
            }

            if (uri!=null) {
                Bitmap bitmap = BarcodeProvider.createAztecBitmap(uri, qrCodeView.getLayoutParams().width);
                qrCodeView.setImageBitmap(bitmap);
            }

            accountName.setText(displayName);
            accountJid.setText(displayJID);

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0){
                return BarcodePreviewFragment.newInstance("","");
            }

            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
            }
            return null;
        }
    }
}
