package com.noklin.simplechat;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.noklin.simplechat.database.ChatDatabaseManager;
import com.noklin.simplechat.database.User;

import static com.noklin.simplechat.database.ApplicationDatabase.Entities.CHAT_ID;
import static com.noklin.simplechat.database.ApplicationDatabase.Entities.CHAT_OPPONENT;
import static com.noklin.simplechat.database.ApplicationDatabase.Entities.FRIEND;
import static com.noklin.simplechat.database.ApplicationDatabase.Entities.LETTER_IS_CHAT_FIELD;
import static com.noklin.simplechat.database.ApplicationDatabase.Entities.USER_ID;

public class ChatListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static String TAG = FriendListFragment.class.getSimpleName();

    private ChatCursorAdapter mChatCursorAdapter;
    private Loader<Cursor> loader;
    private User mCurrentUser;

    public ChatListFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView)root.findViewById(android.R.id.list);
        registerForContextMenu(listView);
        return root;
    }

    public void updateChatList(){
        getLoaderManager().restartLoader(0, null, this);
    }

    public static ChatListFragment newInstance(User user){
        Bundle args = Utils.userToBundle(user);
        ChatListFragment instance = new ChatListFragment();
        instance.setArguments(args);
        return instance;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentUser = Utils.bundleToUser(getArguments());

    }



    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Cursor currentCursor = (Cursor)mChatCursorAdapter.getItem(position);
        Intent i = new Intent(getActivity(), CommunicationActivity.class);
        i.putExtras(Utils.userToBundle(mCurrentUser));
        i.putExtra(CHAT_OPPONENT, currentCursor.getString(currentCursor.getColumnIndex(CHAT_ID)));
        i.putExtra(LETTER_IS_CHAT_FIELD, true);
        startActivity(i);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.chat_list_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        Cursor currentCursor = (Cursor)mChatCursorAdapter.getItem(info.position);
        switch(item.getItemId()){
            case R.id.action_leave_from_chat:
                ChatCommunicator communicator = (ChatCommunicator)getActivity();
                communicator.leaveFromChat(currentCursor.getString(currentCursor.getColumnIndex(CHAT_ID)));
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mChatCursorAdapter = new ChatCursorAdapter(getActivity() , null , 0);
        loader = getLoaderManager().initLoader(0, null, this);
        setListAdapter(mChatCursorAdapter);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new FriendCursorLoader(getActivity(), mCurrentUser);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mChatCursorAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mChatCursorAdapter.swapCursor(null);
    }

    static class FriendCursorLoader extends CursorLoader {
        private final User mCurrentUser;
        public FriendCursorLoader(Context context, User user) {
            super(context);
            mCurrentUser = user;

        }

        @Override
        public Cursor loadInBackground() {
            return ChatDatabaseManager.getInstance(getContext()).getJoinedChatCursor(mCurrentUser.getId());
        }
    }

    static class ChatCursorAdapter extends CursorAdapter {


        public ChatCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(R.layout.list_item_chat , parent , false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String chatTitle = cursor.getString(cursor.getColumnIndex(CHAT_ID));
            TextView tv = (TextView)view.findViewById(R.id.chat_title);
            tv.setText(chatTitle);
        }

    }
}
