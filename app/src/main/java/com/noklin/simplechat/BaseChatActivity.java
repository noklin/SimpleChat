package com.noklin.simplechat;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.noklin.simplechat.database.Letter;
import com.noklin.simplechat.database.User;
import com.noklin.simplechat.network.ChatManager;
import com.noklin.simplechat.network.ChatService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.noklin.simplechat.database.ApplicationDatabase.Entities.*;


public abstract class BaseChatActivity  extends AppCompatActivity implements ChatCommunicator{
    private static final String TAG = BaseChatActivity.class.getSimpleName();
    protected User mCurrentUser;
    private ChatBroadcastReceiver mChatBroadcastReceiver;
    private ChatManager mChatManager;
    private boolean mBound;


    protected void onUpdatedFriendList(){}
    protected void onUpdatedChatsList(){}
    protected void onSearchedUser(User searchedUser){}
    protected void onSearchedGroupChat(String chatId){}
    protected void onDialogUpdated(String initiator){}

    public class ChatBroadcastReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getIntExtra(CHAT_BROADCAST_RECEIVER, 0)){
                case ADDED_USER_TO_SEARCH_LIST:
                    onSearchedUser(Utils.bundleToUser(intent.getExtras()));
                    break;
                case UPDATED_FRIEND_LIST:
                    onUpdatedFriendList();
                    break;
                case ADDED_CHAT_TO_SEARCH_LIST:
                    onSearchedGroupChat(intent.getStringExtra(CHAT_ID));
                    break;
                case UPDATED_GROUP_CHATS_LIST:
                    onUpdatedChatsList();
                    break;
                case UPDATED_DIALOG:
                    onDialogUpdated(intent.getStringExtra(LETTER_SENDER));
                    break;
            }
        }
    };

    @Override
    public void addFriend(User user) {
        mChatManager.addFriend(user);
    }

    @Override
    public void removeFriend(String friendId) {
        mChatManager.removeFriend(friendId);
    }

    @Override
    public void findUser(String query) {
        mChatManager.findUser(query);
    }

    @Override
    public void findChat(String query) {
        mChatManager.findChat(query);
    }

    @Override
    public void joinChat(String chatId) {
        mChatManager.joinChat(chatId);
    }

    @Override
    public void leaveFromChat(String chatId) {
        mChatManager.leaveChat(chatId);
    }

    @Override
    public void closeChatConnection() {
        if(mChatManager != null){
            try{
                mChatManager.closeChatConnection();
            }catch(IOException ex){
                Log.d(TAG , "Ex while close chat connection: " + ex.getMessage());
            }
            mChatManager = null;
        }
    }


    private List<Letter> outputLetters = new ArrayList<>();

    @Override
    public void postLetter(Letter letter) {
        if(mChatManager != null){
            mChatManager.postLetter(letter);
        }else{
            outputLetters.add(letter);
        }
    }

    private final ServiceConnection mChatServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mChatManager =  ((ChatService.LocalBinder)service).getChatNetworkManager();
            mBound = true;
            for(Letter l : outputLetters){
                mChatManager.postLetter(l);
            }
            outputLetters.clear();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mChatManager = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChatBroadcastReceiver = new ChatBroadcastReceiver();
        registerReceiver(mChatBroadcastReceiver, new IntentFilter(CHAT_BROADCAST_RECEIVER));
        mCurrentUser = Utils.bundleToUser(getIntent().getExtras());
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mChatBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this , ChatService.class);
        intent.putExtras(Utils.userToBundle(mCurrentUser));
        bindService(intent, mChatServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        unbindService(mChatServiceConnection);
        super.onStop();
    }

    public final static String CHAT_BROADCAST_RECEIVER = BaseChatActivity.class.getCanonicalName();
    public final static int UPDATED_GROUP_CHATS_LIST = 1;
    public final static int UPDATED_FRIEND_LIST = 2;
    public final static int ADDED_USER_TO_SEARCH_LIST = 3;
    public final static int ADDED_CHAT_TO_SEARCH_LIST = 4;
    public final static int UPDATED_DIALOG = 5;
}