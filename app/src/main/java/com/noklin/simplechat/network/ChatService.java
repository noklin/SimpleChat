package com.noklin.simplechat.network;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.noklin.simplechat.Utils;

import java.io.IOException;

public class ChatService extends Service{
    private static final String TAG = ChatService.class.getSimpleName();

    private ChatManager mChatManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG , "onCreate");
    }


    public ChatService(){
        Log.d(TAG , "ChatService()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG , "onBind()");
        mChatManager = new ChatManager(getApplicationContext(), Utils.bundleToUser(intent.getExtras()));
        return myBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG , "onUnbind");
        try{
            mChatManager.closeChatConnection();
        }catch(IOException ex){
            Log.d(TAG, "Close ex: " + ex.getMessage());
        }
        return super.onUnbind(intent);
    }

    private final IBinder myBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public ChatManager getChatNetworkManager() {
            return mChatManager;
        }
    }
}