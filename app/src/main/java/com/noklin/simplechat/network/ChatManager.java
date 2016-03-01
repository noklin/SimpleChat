package com.noklin.simplechat.network;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.noklin.network.packets.DataPacket;
import com.noklin.network.packets.LetterStatePacket;
import com.noklin.network.packets.Packet;
import com.noklin.network.packets.clientpackets.AddFriendPacket;
import com.noklin.network.packets.clientpackets.AuthorizePacket;
import com.noklin.network.packets.clientpackets.FindChatPacket;
import com.noklin.network.packets.clientpackets.FindUserPacket;
import com.noklin.network.packets.clientpackets.JoinChatPacket;
import com.noklin.network.packets.clientpackets.LeaveChatPacket;
import com.noklin.network.packets.clientpackets.RemoveFriendPacket;
import com.noklin.network.packets.serverpackets.ChatInfoPacket;
import com.noklin.network.packets.serverpackets.UserInfoPacket;
import com.noklin.network.simplechat.ChatConnection;
import com.noklin.simplechat.BaseChatActivity;
import com.noklin.simplechat.R;
import com.noklin.simplechat.Utils;
import com.noklin.simplechat.database.ChatDatabaseManager;
import com.noklin.simplechat.database.Letter;
import com.noklin.simplechat.database.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.noklin.simplechat.database.ApplicationDatabase.Entities.*;

public class ChatManager {

    private final static String TAG = ChatManager.class.getSimpleName();
    private final Context mContext;
    private final User mCurrentUser;
    private final ChatDatabaseManager mChatDatabaseManager;

    private ChatConnection mChatConnection;
    private ChatDatabaseHandler mChatDatabaseHandler;
    private ChatPostHandler mChatPostHandler;
    private ChatReceiveHandler mChatReceiveHandler;
    private InetSocketAddress mServerAddress;



    public void findUser(String userIs){
        sendFindUser(userIs);
    }

    public void addFriend(User friend){
        addUser(friend);
        addFriendship(friend.getId());
    }

    public void removeFriend(String friendId){
        removeFriendship(friendId);
    }

    public void findChat(String chatId){
        sendFindChat(chatId);
    }

    public void joinChat(String chatId){
        sendJoinChat(chatId);
    }

    public void leaveChat(String chatId) {
        removeJoinedChat(chatId);
    }

    public void postLetter(Letter outputLetter){
        putLetter(outputLetter);
        sendLetter(outputLetter);
    }

    public ChatManager(Context context , User user){
        mCurrentUser = user;
        mContext = context;
        mChatDatabaseManager = ChatDatabaseManager.getInstance(context);
        mServerAddress = new InetSocketAddress(context.getString(R.string.serverIp)
                , context.getResources().getInteger(R.integer.serverPort));

        HandlerThread chatDatabaseThread = new HandlerThread(ChatDatabaseHandler.class.getSimpleName()
                , android.os.Process.THREAD_PRIORITY_BACKGROUND);
        chatDatabaseThread.setDaemon(true);
        chatDatabaseThread.start();
        mChatDatabaseHandler = new ChatDatabaseHandler(chatDatabaseThread.getLooper());
        mChatReceiveHandler = new ChatReceiveHandler();
        Thread inputChatMessageThread = new Thread(mChatReceiveHandler);
        inputChatMessageThread.setDaemon(true);
        inputChatMessageThread.start();

        HandlerThread chatOutputHandleThread = new HandlerThread(ChatPostHandler.class.getSimpleName()
                , android.os.Process.THREAD_PRIORITY_BACKGROUND);
        chatOutputHandleThread.setDaemon(true);
        chatOutputHandleThread.start();
        mChatPostHandler = new ChatPostHandler(chatOutputHandleThread.getLooper());
//        openConnection();
    }

    private boolean isInternetAvailable(){
        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private synchronized ChatConnection getChatConnection() throws IOException {
        if(mChatConnection == null){
            Socket socket = new Socket();
            socket.connect(mServerAddress);
            mChatConnection = new ChatConnection(socket);
            Log.d(TAG , "authorize l: " + mCurrentUser.getLogin() + " p: "  + mCurrentUser.getPassword() + " thread: " + Thread.currentThread().getName());
            mChatConnection.sendPacket(new AuthorizePacket(mCurrentUser.getLogin()
                    , mCurrentUser.getPassword()));
            Packet response = mChatConnection.receivePacket();
            if(response == null || response.getType() != Packet.Type.AUTHORIZE){
                throw new IOException("Authorize failed: bad response");
            }

            for(String userId : mChatDatabaseManager.getLostRemoveFriend(mCurrentUser.getId())){
                Log.d(TAG, " lost remove friend: " + userId);
                sendRemoveFriend(userId);
                mChatDatabaseManager.removeLostRemoveFriend(mCurrentUser.getId(), userId);
            }

            for(String chatId : mChatDatabaseManager.getLostLeaveChat(mCurrentUser.getId())){
                Log.d(TAG, " lost remove chat: " + chatId);
                sendLeaveChat(chatId);
                mChatDatabaseManager.removeLostLeaveChat(mCurrentUser.getId(), chatId);
            }

            for(Letter letter : mChatDatabaseManager.getLostPostLetter(mCurrentUser.getId())){
                Log.d(TAG , " lost letter: " + letter.getContentType());
                if("PIC".equals(letter.getContentType())){
                    Log.d(TAG , "letter prepare to post");
                    prepareToPost(letter);
                }
                sendLetter(letter);
            }
        }
        return mChatConnection;
    }

    public void closeChatConnection() throws IOException{
        if(mChatConnection != null){
            try{
                mChatConnection.close();
            }finally{
                mChatConnection = null;
            }
        }
    }



    private final class ChatReceiveHandler implements Runnable{

        @Override
        public void run() {
            try{
                ChatConnection connection = getChatConnection();
                Packet inputPacket;
                User inputUser;
                Letter inputLetter;
                while((inputPacket = connection.receivePacket())!= null){
                    Log.d(TAG , "Received packet type: " + inputPacket.getType());
                    switch(inputPacket.getType()){
                        case USER_INFO_PACKET:
                            UserInfoPacket userInfoPacket = new UserInfoPacket(inputPacket);
                            inputUser = new User(
                                    userInfoPacket.getLogin()
                                    , null
                                    , userInfoPacket.getPublicName()
                                    , userInfoPacket.getPhoto()
                            );
                            if("SEARCH".equals(userInfoPacket.getMode())){
                                searchUserListChangedNotification(inputUser);
                            }else{
//                                onUpdateUserInfoReceived(userInfoPacket.getLogin());
                            }
                            break;
                        case CHAT_INFO_PACKET:
                            ChatInfoPacket chatInfoPacket = new ChatInfoPacket(inputPacket);
                            searchChatListChangedNotification(chatInfoPacket.getTitle());
                            Log.d(TAG , "CHAT_INFO_PACKET: title:" + chatInfoPacket.getTitle());
                            break;
                        case DATA_PACKET:
                            DataPacket dataPacket = new DataPacket(inputPacket);
                            Log.d("DATA_PACKET", " receiver: " + dataPacket.getReceiver());
                            inputLetter = new Letter(
                                    dataPacket.getSender()
                                    , dataPacket.getReceiver()
                                    ,"REC"
                                    , dataPacket.getData()
                                    , dataPacket.getDate()
                                    , dataPacket.getContentType()
                                    , dataPacket.isChatable()
                            );
                            sendLetterState(inputLetter);
                            inputLetter.setDate(System.currentTimeMillis());
                            if("PIC".equals(dataPacket.getContentType())){
                                try{
                                    saveImage(inputLetter);
                                }catch(IOException ex){
                                    Log.d(TAG , "Save image failed: "  + ex.getMessage());
                                    break;
                                }
                            }
                            putLetter(inputLetter);
                            break;
                        case LETTER_STATE_PACKET:
                            LetterStatePacket report = new LetterStatePacket(inputPacket);
                            updateLetterState(report.getLetterId() , report.getStatus(), report.getLetterReceiver());
                            break;
                        case ADD_FRIEND:
                            AddFriendPacket addFriendPacket = new AddFriendPacket(inputPacket);
                            addFriendship(addFriendPacket.getTarget());
                            friendListChangedNotification();
                            break;
                        case REMOVE_FRIEND:
                            RemoveFriendPacket removeFriendPacket = new RemoveFriendPacket(inputPacket);
                            removeFriendship(removeFriendPacket.getTarget());
                            break;
                        case JOIN_CHAT:
                            JoinChatPacket joinChatPacket = new JoinChatPacket(inputPacket);
                            addJoinedChat(joinChatPacket.getChat());
                            break;
                    }
                }

            }catch(IOException ex){
                Log.i(TAG, "Connection problems: " + ex.getMessage());
            }
            finally{
                try{
                    closeChatConnection();
                }catch (IOException closeEx){
                    Log.i(TAG, "Exception while closing: " + closeEx.getMessage());
                }
            }
        }
    }

    public void saveImage(Letter letter) throws IOException{
        File photoFile = Utils.createImageFile(letter.getDate(), letter.getSender());
        FileOutputStream fout = new FileOutputStream(photoFile);
        fout.write(letter.getData());
    }

//                    database communication


    private void updateLetterState(long letterId, String state, String letterReceiver){
        Bundle data = new Bundle();
        data.putLong(LETTER_ID, letterId);
        data.putString(LETTER_STATE, state);
        data.putString(LETTER_RECEIVER, letterReceiver);
        sendMessageToChatDatabaseHandler(WHAT_DB_UPDATE_LETTER, data);
    }

    public void putLetter(Letter letter){
        sendMessageToChatDatabaseHandler(WHAT_DB_ADD_LETTER, Utils.letterToBundle(letter));
    }


    private void addUser(User user){
        sendMessageToChatDatabaseHandler(WHAT_DB_ADD_USER, Utils.userToBundle(user));
    }

    private void addFriendship(String friendId){
        Bundle data = new Bundle();
        data.putString(USER_ID , friendId);
        sendMessageToChatDatabaseHandler(WHAT_DB_ADD_FRIEND, data);
    }

    private void removeFriendship(String friendId) {
        Bundle data = new Bundle();
        data.putString(USER_ID, friendId);
        sendMessageToChatDatabaseHandler(WHAT_DB_REMOVE_FRIEND, data);
    }

    public void updateUser(User user){
        sendMessageToChatDatabaseHandler(WHAT_DB_UPDATE_USER, Utils.userToBundle(user));
    }



    private void addJoinedChat(String chatId){
        Bundle data = new Bundle();
        data.putString(CHAT_ID , chatId);
        sendMessageToChatDatabaseHandler(WHAT_DB_JOIN_CHAT, data);
    }

    private void removeJoinedChat(String chatId){
        Bundle data = new Bundle();
        data.putString(CHAT_ID , chatId);
        sendMessageToChatDatabaseHandler(WHAT_DB_LEAVE_CHAT, data);
    }

    private void addLostLeaveChat(String chatId){
        Bundle data = new Bundle();
        data.putString(CHAT_ID, chatId);
        sendMessageToChatDatabaseHandler(WHAT_DB_ADD_LOST_LEAVE_CHAT, data);
    }

    private void addLostRemoveFriend(String friendId){
        Bundle data = new Bundle();
        data.putString(USER_ID, friendId);
        sendMessageToChatDatabaseHandler(WHAT_DB_ADD_LOST_REMOVE_FRIEND, data);
    }



    private void addLostUpdateUser(){
        sendMessageToChatDatabaseHandler(WHAT_DB_ADD_LOST_UPDATE_USER, null);
    }

    private void removeLostUpdateUser(){
        sendMessageToChatDatabaseHandler(WHAT_DB_REMOVE_LOST_UPDATE_USER, null);
    }

    private void sendMessageToChatDatabaseHandler(int what , Bundle data){
        Log.d("mChatPostHandler null? ", "" + (mChatPostHandler == null));
        Message msg = mChatPostHandler.obtainMessage();
        msg.what = what;
        msg.setData(data);
        mChatDatabaseHandler.sendMessage(msg);
    }


    private Letter prepareToBd(Letter letter) throws IOException {
        File photoFile = Utils.createImageFile(letter.getDate(), letter.getSender());
        int targetW = mContext.getResources().getDimensionPixelSize(R.dimen.dialog_photo_width);
        int targetH = mContext.getResources().getDimensionPixelSize(R.dimen.dialog_photo_height);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoFile.getAbsolutePath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), bmOptions);
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, blob);
        letter.setData(blob.toByteArray());
        return letter;
    }

    private Letter prepareToPost(Letter letter) throws IOException {
        DataInputStream dis = null;
        try{
            File photoFile = Utils.createImageFile(letter.getDate(), letter.getSender());
            dis = new DataInputStream(new FileInputStream(photoFile));
            byte[] buff = new byte[(int)photoFile.length()];
            dis.readFully(buff);
            letter.setData(buff);
        }finally{
            try{
                if(dis != null)
                    dis.close();
            }catch(IOException ex){
                Log.d(TAG , "Exception while close: " + ex.getMessage());
            }
        }
        return letter;
    }

    private final class ChatDatabaseHandler  extends Handler{



        ChatDatabaseHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case WHAT_DB_UPDATE_LETTER:
                    long letterId = msg.getData().getLong(LETTER_ID);
                    String letterState = msg.getData().getString(LETTER_STATE);
                    String letterReceiver = msg.getData().getString(LETTER_RECEIVER);
                    Log.d("WHAT_DB_UPDATE_LETTER", " letterId: " + letterId + " letterState: " + letterState + " letterReceiver: " + letterReceiver);
                    mChatDatabaseManager.updateLetterState(letterId, letterState);
                    dialogChangedNotification(letterReceiver);
                    break;
                case WHAT_DB_ADD_LETTER:
                    Letter inputLetter = Utils.bundleToLetter(msg.getData());
                    String initiator = inputLetter.isChatField() ? inputLetter.getReceiver() : inputLetter.getSender();
                    if("PIC".equals(inputLetter.getContentType())){
                        try{
                            mChatDatabaseManager.putLetter(prepareToBd(inputLetter));
                            Log.d(TAG , "send data size: " + inputLetter.getData().length);
                            dialogChangedNotification(initiator);
                        }catch(IOException | OutOfMemoryError ex){
                            Log.e(TAG, "Photo save failed " + ex.getMessage());
                            Toast.makeText(mContext , R.string.save_picture_error_toast , Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        mChatDatabaseManager.putLetter(inputLetter);
                        dialogChangedNotification(initiator);
                    }
                    break;
                case WHAT_DB_ADD_USER:
                    User friend = Utils.bundleToUser(msg.getData());
                    mChatDatabaseManager.putUser(friend);
                    break;
                case WHAT_DB_ADD_FRIEND:
                    String friendId = msg.getData().getString(USER_ID);
                    if(mChatDatabaseManager.addFriend(mCurrentUser.getId(), friendId)){
                        sendAddFriend(friendId);
                    }
                    break;
                case WHAT_DB_REMOVE_FRIEND:
                    friendId = msg.getData().getString(USER_ID);
                    if(mChatDatabaseManager.removeFriend(mCurrentUser.getId(), friendId)){
                        friendListChangedNotification();
                        sendRemoveFriend(friendId);
                    }
                    break;
                case WHAT_DB_JOIN_CHAT:
                    if(mChatDatabaseManager.joinChat(mCurrentUser.getId(), msg.getData().getString(CHAT_ID)))
                        groupChatListChangedNotification();
                    break;
                case WHAT_DB_LEAVE_CHAT:
                    String chatId = msg.getData().getString(CHAT_ID);
                    if(mChatDatabaseManager.leaveChat(mCurrentUser.getId(), msg.getData().getString(CHAT_ID))){
                        sendLeaveChat(chatId);
                    }
                    groupChatListChangedNotification();
                    break;
                case WHAT_DB_UPDATE_USER:
                    mChatDatabaseManager.updateUser(Utils.bundleToUser(msg.getData()));
                    break;
                case WHAT_DB_ADD_LOST_LEAVE_CHAT:
                    mChatDatabaseManager.putLostLeaveChat(mCurrentUser.getId(), msg.getData().getString(CHAT_ID));
                    break;
                case WHAT_DB_REMOVE_LOST_LEAVE_CHAT:
                    mChatDatabaseManager.removeLostLeaveChat(mCurrentUser.getId(), msg.getData().getString(CHAT_ID));
                    break;
                case WHAT_DB_ADD_LOST_REMOVE_FRIEND:
                    mChatDatabaseManager.putLostRemoveFriend(mCurrentUser.getId(), msg.getData().getString(USER_ID));
                    break;
                case WHAT_DB_REMOVE_LOST_REMOVE_FRIEND:
                    mChatDatabaseManager.removeLostRemoveFriend(mCurrentUser.getId(), msg.getData().getString(USER_ID));
                    break;
                case WHAT_DB_ADD_LOST_UPDATE_USER:
                    mChatDatabaseManager.changeUpdateState(mCurrentUser.getId() , true);
                    break;
                case WHAT_DB_REMOVE_LOST_UPDATE_USER:
                    mChatDatabaseManager.changeUpdateState(mCurrentUser.getId() , false);
                    break;
                default:
                    Log.d(TAG , "ChatDatabaseHandler default ");
            }
        }
    };


    private void dialogChangedNotification(String initiator){
        Log.d(TAG , "dialogChangedNotification with initioator: " + initiator );
        Bundle data = new Bundle();
        data.putString(LETTER_SENDER , initiator);
        generateChatBroadcast(data, BaseChatActivity.UPDATED_DIALOG);
    }

    private void friendListChangedNotification(){
        generateChatBroadcast(null, BaseChatActivity.UPDATED_FRIEND_LIST);
    }

    private void groupChatListChangedNotification(){
        generateChatBroadcast(null, BaseChatActivity.UPDATED_GROUP_CHATS_LIST);
    }


    private void searchUserListChangedNotification(User user){
        generateChatBroadcast(Utils.userToBundle(user), BaseChatActivity.ADDED_USER_TO_SEARCH_LIST);
    }


    private void searchChatListChangedNotification(String chatId){
        Bundle data = new Bundle();
        data.putString(CHAT_ID, chatId);
        generateChatBroadcast(data, BaseChatActivity.ADDED_CHAT_TO_SEARCH_LIST);
    }

    private void generateChatBroadcast(Bundle data , int whatReceived){
        Intent intent = new Intent(BaseChatActivity.CHAT_BROADCAST_RECEIVER);
        intent.putExtra(BaseChatActivity.CHAT_BROADCAST_RECEIVER, whatReceived);
        if(data != null)
            intent.putExtras(data);
        mContext.sendBroadcast(intent);
    }



    //            server communication




    private void sendLetter(Letter letter){
        sendMessageToChatPostHandler(WHAT_SEND_LETTER, Utils.letterToBundle(letter));
    }

    private void sendLetterState(Letter letter){
        sendMessageToChatPostHandler(WHAT_SEND_REPORT, Utils.letterToBundle(letter));
    }

    private void sendUpdateUserInfo(User updatedUser){
        sendMessageToChatPostHandler(WHAT_SEND_UPDATED_USER_INFO, Utils.userToBundle(updatedUser));
    }

    private void sendFindUser(String UserId){
        Bundle data = new Bundle();
        data.putString(USER_ID , UserId);
        sendMessageToChatPostHandler(WHAT_SEND_FIND_USER_INFO, data);
    }

    private void sendAddFriend(String friendId){
        Bundle data = new Bundle();
        data.putString(USER_ID, friendId);
        sendMessageToChatPostHandler(WHAT_SEND_ADD_FRIEND, data);
    }

    private void sendRemoveFriend(String friendId){
        Bundle data = new Bundle();
        data.putString(USER_ID, friendId);
        sendMessageToChatPostHandler(WHAT_SEND_REMOVE_FRIEND, data);

    }

    private void sendFindChat(String chatTitle){
        Bundle data = new Bundle();
        data.putString(CHAT_ID , chatTitle);
        sendMessageToChatPostHandler(WHAT_SEND_FIND_CHAT_INFO, data);
    }

    private void sendJoinChat(String chatTitle){
        Bundle data = new Bundle();
        data.putString(CHAT_ID , chatTitle);
        sendMessageToChatPostHandler(WHAT_SEND_JOIN_CHAT, data);
    }

    private void sendLeaveChat(String chatTitle){
        Bundle data = new Bundle();
        data.putString(CHAT_ID , chatTitle);
        sendMessageToChatPostHandler(WHAT_SEND_LEAVE_CHAT, data);
    }



    private void sendMessageToChatPostHandler(int what , Bundle data){
        Message msg = mChatPostHandler.obtainMessage();
        msg.what = what;
        msg.setData(data);
        mChatPostHandler.sendMessage(msg);
    }

    private final class ChatPostHandler extends Handler{

        ChatPostHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "msg.what: " + msg.what);
            Packet outputPacket = null;
            Letter outputLetter;
            User outputUser;
            try{
                switch(msg.what){
                    case WHAT_SEND_LETTER:
                        outputLetter = Utils.bundleToLetter(msg.getData());
                        Log.d(TAG , " SEND LETTER ");
                        if("PIC".equals(outputLetter.getContentType())){
                            try{
                                Log.d(TAG , " PREPARE TO POST ");
                                prepareToPost(outputLetter);
                            }catch(IOException ex){
                                Log.d(TAG , "Send Picture failed: " + ex.getMessage());
                                outputPacket = null;
                                break;
                            }
                            Log.d(TAG ,"PREPARE TO POST LETTER SIZE: " + outputLetter.getData().length);
                        }
                        Log.d(TAG, " out ct: " + outputLetter.getContentType());
                        Log.d(TAG, " out sender: " + outputLetter.getSender());
                        Log.d(TAG, " out receiver: " + outputLetter.getReceiver());
                        Log.d(TAG, " out date len: " + outputLetter.getData().length);
                        outputPacket = new DataPacket(
                                 outputLetter.getContentType()
                                ,outputLetter.getSender()
                                ,outputLetter.getReceiver()
                                ,outputLetter.getDate()
                                ,outputLetter.getData()
                                ,outputLetter.isChatField()
                        );


                        DataPacket dp = (DataPacket)outputPacket;
                        Log.d(TAG ,"dp t: " + outputPacket.getType());
                        Log.d(TAG ,"dp rec: " + dp.getReceiver());
                        Log.d(TAG ,"dp data len: " + dp.getData().length);
                        Log.d(TAG ,"dp chat: " + dp.isChatable());
                        Log.d(TAG ,"dp sender: " + dp.getSender());
                        Log.d(TAG ,"dp content: " + dp.getContentType());

                        break;
                    case WHAT_SEND_REPORT:
                        outputLetter = Utils.bundleToLetter(msg.getData());
                        outputPacket = new LetterStatePacket(
                                 mCurrentUser.getId()
                                ,"REC"
                                ,outputLetter.getId()
                        );
                        break;
                    case WHAT_SEND_UPDATED_USER_INFO:
                        outputUser = Utils.bundleToUser(msg.getData());
                        outputPacket = new UserInfoPacket(
                                outputUser.getLogin()
                                ,outputUser.getPublicName()
                                ,outputUser.getPhoto()
                                ,"UPDATE"
                        );
                        break;
                    case WHAT_SEND_FIND_USER_INFO:
                        outputPacket = new FindUserPacket(msg.getData().getString(USER_ID, ""));
                        break;
                    case WHAT_SEND_ADD_FRIEND:
                        outputPacket = new AddFriendPacket(msg.getData().getString(USER_ID, ""));
                        break;
                    case WHAT_SEND_REMOVE_FRIEND:
                        String friendId = msg.getData().getString(USER_ID, "");
                        outputPacket = new RemoveFriendPacket(msg.getData().getString(USER_ID, ""));
                        if(mChatConnection == null){
                            addLostRemoveFriend(friendId);
                        }
                        break;
                    case WHAT_SEND_FIND_CHAT_INFO:
                        outputPacket = new FindChatPacket(msg.getData().getString(CHAT_ID, ""));
                        break;
                    case WHAT_SEND_JOIN_CHAT:
                        if(!mChatDatabaseManager.isUserInChat(mCurrentUser.getId() , msg.getData().getString(CHAT_ID))){
                            outputPacket = new JoinChatPacket(msg.getData().getString(CHAT_ID, ""));
                        }else {
                            outputPacket = null;
                        }
                        break;
                    case WHAT_SEND_LEAVE_CHAT:
                        String chatId = msg.getData().getString(CHAT_ID, "");
                        outputPacket = new LeaveChatPacket(msg.getData().getString(CHAT_ID, ""));
                        if(mChatConnection == null){
                            addLostLeaveChat(chatId);
                        }
                        break;



                }
                Log.d(TAG , " mChatconnetion null? " + (mChatConnection == null));
                Log.d(TAG , " mOutputPacket null? " + (outputPacket == null));
                if(outputPacket != null){
                    getChatConnection().sendPacket(outputPacket);
                }
            }catch(IOException ex){
                Log.i(TAG, "While send packet: " + ex.getMessage());
            }

        }
    };

    private final static int WHAT_DB_UPDATE_LETTER = 1;
    private final static int WHAT_DB_ADD_LETTER = 2;
    private final static int WHAT_DB_ADD_FRIEND = 3;
    private final static int WHAT_DB_ADD_USER = 16;
    private final static int WHAT_DB_REMOVE_FRIEND = 4;
    private final static int WHAT_DB_JOIN_CHAT = 5;
    private final static int WHAT_DB_LEAVE_CHAT = 6;
    private final static int WHAT_DB_UPDATE_USER = 7;
    private final static int WHAT_DB_ADD_LOST_UPDATE_USER = 9;
    private final static int WHAT_DB_REMOVE_LOST_UPDATE_USER = 15;
    private final static int WHAT_DB_ADD_LOST_REMOVE_FRIEND = 11;
    private final static int WHAT_DB_REMOVE_LOST_REMOVE_FRIEND = 14;
    private final static int WHAT_DB_ADD_LOST_LEAVE_CHAT = 12;
    private final static int WHAT_DB_REMOVE_LOST_LEAVE_CHAT = 13;

    private final static int WHAT_SEND_LETTER = 5;
    private final static int WHAT_SEND_REPORT = 6;
    private final static int WHAT_SEND_UPDATED_USER_INFO = 7;
    private final static int WHAT_SEND_FIND_USER_INFO = 8;
    private final static int WHAT_SEND_ADD_FRIEND = 9;
    private final static int WHAT_SEND_REMOVE_FRIEND = 10;
    private final static int WHAT_SEND_FIND_CHAT_INFO = 11;
    private final static int WHAT_SEND_JOIN_CHAT = 12;
    private final static int WHAT_SEND_LEAVE_CHAT = 13;
}