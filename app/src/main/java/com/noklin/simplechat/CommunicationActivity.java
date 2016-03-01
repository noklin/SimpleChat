package com.noklin.simplechat;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.noklin.simplechat.database.ChatDatabaseManager;
import com.noklin.simplechat.database.Letter;
import com.noklin.simplechat.database.User;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import static com.noklin.simplechat.database.ApplicationDatabase.Entities.*;


public class CommunicationActivity extends BaseChatActivity{
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageButton mSendPhoto;
    private ImageButton mSendText;
    private boolean mGroupMode;

    private final static String TAG = CommunicationActivity.class.getSimpleName();

    private CommunicationFragment mCommunicationFragment;
    private EditText mMessageField;
    private String mOpponent;
    private long mLastPhotoDate;


    @Override
    protected void onDialogUpdated(String initiator) {
        if(initiator.equals(mOpponent) || initiator.equals(mCurrentUser.getId())){
            mCommunicationFragment.updateDialogList();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            mLastPhotoDate =  savedInstanceState.getLong(LETTER_DATE);
        }
        setContentView(R.layout.activity_communication);
        mOpponent = getIntent().getStringExtra(CHAT_OPPONENT);
        mMessageField = (EditText)findViewById(R.id.message_field);
        mGroupMode = getIntent().getBooleanExtra(LETTER_IS_CHAT_FIELD , true);
        Log.d(TAG , " opponent: " + mOpponent + " group Mode: "  + mGroupMode);
        mMessageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().isEmpty()){
                    mSendPhoto.setVisibility(View.VISIBLE);
                    mSendText.setVisibility(View.GONE);
                }else{
                    mSendPhoto.setVisibility(View.GONE);
                    mSendText.setVisibility(View.VISIBLE);
                }
            }
        });

        mSendText = (ImageButton)findViewById(R.id.send_text_button);
        mSendPhoto = (ImageButton)findViewById(R.id.send_picture_button);
        mSendPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        Log.d(TAG , " photoFile = createImageFile(); ");
                        mLastPhotoDate = System.currentTimeMillis();
                        photoFile = Utils.createImageFile(mLastPhotoDate, mCurrentUser.getId());
                        Log.d(TAG , " photoFile: " + photoFile);
                    } catch (IOException ex) {
                        Log.d(TAG , " Exception " + ex.getMessage());
                    }
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    closeChatConnection();
                }
            }
        });
        addFriendDialogFragment();
        setTitle(mOpponent);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(LETTER_DATE , mLastPhotoDate);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            sendLetter("PIC", null, mLastPhotoDate);
        }
    }



    public void sendTextMessage(View v){
        String text = mMessageField.getText().toString();
        if(!text.isEmpty())
            sendLetter("TXT", mMessageField.getText().toString().getBytes());
        mMessageField.clearFocus();
        mMessageField.getText().clear();
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void sendLetter(String contentType, byte[] data ){
        sendLetter(contentType, data, System.currentTimeMillis());
    }


    private void sendLetter(String contentType, byte[] data ,long date){
        Letter outputLetter = new Letter(
                mCurrentUser.getId()
                , mOpponent
                , "PRE"
                , data
                , date
                , contentType
                , mGroupMode
        );
        postLetter(outputLetter);
    }

    private void addFriendDialogFragment(){

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG);
        if(fragment == null){
            mCommunicationFragment = CommunicationFragment.newInstance(mCurrentUser, mOpponent, getIntent().getBooleanExtra(LETTER_IS_CHAT_FIELD , true));
            fm.beginTransaction().add(R.id.dialog_container
                    , mCommunicationFragment, TAG).commit();
        }else{
            mCommunicationFragment = (CommunicationFragment) fragment;
        }
    }

    public static class CommunicationFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
        private User mCurrentUser;
        private String mOpponent;
        private boolean mGroupMode;
        private ListView mMessagesList;
        private ChatMessageCursorAdapter mChatMessageCursorAdapter;
        private Loader<Cursor> loader;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            mCurrentUser = Utils.bundleToUser(getArguments().getBundle(USER));
            mOpponent = getArguments().getString(CHAT_OPPONENT);
            mGroupMode = getArguments().getBoolean(LETTER_IS_CHAT_FIELD);

        }

        public void updateDialogList(){
            getLoaderManager().restartLoader(0, null, this);
        }

        public static CommunicationFragment newInstance(User user , String opponent , boolean groupMode) {
            CommunicationFragment fragment = new CommunicationFragment();
            Bundle args = new Bundle();
            args.putBundle(USER, Utils.userToBundle(user));
            args.putString(CHAT_OPPONENT, opponent);
            args.putBoolean(LETTER_IS_CHAT_FIELD, groupMode);
            fragment.setArguments(args);
            return fragment;

        }





        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_communication, container , false);
            mMessagesList = (ListView)root.findViewById(R.id.dialog_list_view);
            mMessagesList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            mMessagesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Cursor currentCursor = (Cursor)mChatMessageCursorAdapter.getItem(position);
                    if(!"PIC".equals(currentCursor.getString(currentCursor.getColumnIndex(LETTER_CONTENT_TYPE)))) return;
                    Log.d(TAG ," position: " + position + " cursor " + currentCursor);
                    Intent i = new Intent();
                    try{
                        File imageFile = Utils.createImageFile(currentCursor.getLong(currentCursor.getColumnIndex(LETTER_DATE))
                                , currentCursor.getString(currentCursor.getColumnIndex(LETTER_SENDER)));
                        i.setAction(Intent.ACTION_VIEW);
                        i.setDataAndType(Uri.parse("file://" + imageFile.getAbsolutePath()), "image/*");
                        startActivity(i);

                    }catch(IOException ex){
                        Log.d(TAG , "Fail start activity");
                    }
                }
            });
            return root;

        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mChatMessageCursorAdapter = new ChatMessageCursorAdapter(getActivity() , null , 0 , mGroupMode, mCurrentUser.getId());
            loader = getLoaderManager().initLoader(0, null, this);

            mMessagesList.setAdapter(mChatMessageCursorAdapter);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new UserLetterCursorLoader(getActivity(), mCurrentUser , mOpponent, mGroupMode);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mChatMessageCursorAdapter.swapCursor(data);

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mChatMessageCursorAdapter.swapCursor(null);
        }

        static class UserLetterCursorLoader extends CursorLoader {
            private boolean mGroupMode;
            private User mCurrentUser;
            private String mOpponent;

            public UserLetterCursorLoader(Context context, User user , String opponent, boolean groupMode) {
                super(context);
                mCurrentUser = user;
                mOpponent = opponent;
                mGroupMode = groupMode;
            }

            @Override
            public Cursor loadInBackground() {
                Cursor cursor = mGroupMode ? ChatDatabaseManager.getInstance(getContext()).getChatDialogCursor(mOpponent)
                        :ChatDatabaseManager.getInstance(getContext()).getFriendDialogCursor(mCurrentUser.getId(), mOpponent) ;
                return cursor;
            }
        }

        static class ChatMessageCursorAdapter extends CursorAdapter {
            private String mUserId;
            private boolean mGroupMode = false;
            private SimpleDateFormat mDateFormat = new SimpleDateFormat("HH:mm");

            public ChatMessageCursorAdapter(Context context, Cursor c, int flags , boolean groupMode, String userId) {
                super(context, c, flags);
                mGroupMode = groupMode;
                mUserId = userId;
            }



            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                return inflater.inflate(R.layout.list_tem_user_dialog, parent , false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                if(mUserId.equals(cursor.getString(cursor.getColumnIndex(LETTER_SENDER)))){
                    view.findViewById(R.id.letter_container).setBackgroundResource(R.drawable.user_dialog_output_message_background);
                    ((RelativeLayout) view).setGravity(Gravity.END);
                }else{
                    view.findViewById(R.id.letter_container).setBackgroundResource(R.drawable.user_dialog_input_message_background);
                    ((RelativeLayout) view).setGravity(Gravity.START);
                }
                long time = cursor.getLong(cursor.getColumnIndex(LETTER_DATE));
                Date date = new Date(time);

                if("TXT".equals(cursor.getString(cursor.getColumnIndex(LETTER_CONTENT_TYPE)))) {
                    view.findViewById(R.id.text_container).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.image_container).setVisibility(View.GONE);
                    TextView text = (TextView) view.findViewById(R.id.content_text);
                    text.setText(
                            new String(cursor.getBlob(cursor.getColumnIndex(LETTER_DATA)))
                    );
                    ((TextView)view.findViewById(R.id.text_date)).setText(mDateFormat.format(date));
                }else if("PIC".equals(cursor.getString(cursor.getColumnIndex(LETTER_CONTENT_TYPE)))){
                    view.findViewById(R.id.text_container).setVisibility(View.GONE);
                    view.findViewById(R.id.image_container).setVisibility(View.VISIBLE);
                    byte[] raw = cursor.getBlob(cursor.getColumnIndex(LETTER_DATA));
                    Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                    ((ImageView)view.findViewById(R.id.content_picture)).setImageBitmap(
                            bitmap
                    );
                    ((TextView)view.findViewById(R.id.image_date)).setText(mDateFormat.format(date));
                }
                if(mGroupMode){
                    TextView authorTv = (TextView)view.findViewById(R.id.letter_sender);
                    authorTv.setVisibility(View.VISIBLE);
                    authorTv.setText(cursor.getString(cursor.getColumnIndex(LETTER_SENDER)));
                }

            }

        }
    }

}
