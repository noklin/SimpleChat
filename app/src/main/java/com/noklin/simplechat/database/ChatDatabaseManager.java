package com.noklin.simplechat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import static com.noklin.simplechat.database.ApplicationDatabase.Entities.*;

public class ChatDatabaseManager extends SQLiteOpenHelper{
    private static final String TAG = ChatDatabaseManager.class.getSimpleName();

    private static ChatDatabaseManager sInstance;


    public static ChatDatabaseManager getInstance(Context context){
        if(sInstance == null){
            sInstance = new ChatDatabaseManager(context.getApplicationContext());

        }
        return sInstance;
    }

    public ChatDatabaseManager(Context context) {
        super(context, NAME, null, VERSION);
        Log.d(TAG, "ChatDatabaseManager init");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG , " Init DB... ");
        db.execSQL(CREATE_TABLE_USER);
        db.execSQL(CREATE_TABLE_LETTER);
        db.execSQL(CREATE_TABLE_CHAT);
        db.execSQL(CREATE_TABLE_USER_TO_USER);
        db.execSQL(CREATE_TABLE_USER_TO_CHAT);
        db.execSQL(CREATE_TABLE_USER_TO_LETTER);
        db.execSQL(CREATE_TABLE_CHAT_TO_LETTER);
        db.execSQL(CRETE_TABLE_REMOVED_FRIENDS);
        db.execSQL(CRETE_TABLE_LEAVED_CHATS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void putUser(User user){
        if(isUserExist(user)){
            updateUser(user);
            return;
        }
        ContentValues values = userToContentValues(user);
        getWritableDatabase().insert(USER, null, values);
        Log.d(TAG, " inserted user with id: " + user.getId());
    }

    public void updateUser(User user){
        if(!isUserExist(user)) return;
        ContentValues values = new ContentValues();
        values.put(USER_PUBLIC_NAME, user.getPublicName());
        values.put(USER_PHOTO, user.getPhoto());
        ContentValues v = new ContentValues();
        int i = getWritableDatabase().update(USER, values, USER_ID
                + " =  ? ", new String[]{user.getId()});
        Log.d(TAG , " updated user with id: " + user.getId() + " success:" + (i == 1));
    }

    public boolean addFriend(String userId , String friendId){
        if(isFriendshipExist(userId, friendId)) return false;
        ContentValues values = new ContentValues();
        values.put(USER_TO_USER_FROM, userId);
        values.put(USER_TO_USER_TO, friendId);
        long result = getWritableDatabase().insert(USER_TO_USER, null, values);
        Log.d(TAG , " added friendship from " + userId +" to " + friendId);
        return result != -1;
    }

    public boolean removeFriend(String userId, String friendId){
        int i = getWritableDatabase().delete(USER_TO_USER
                , USER_TO_USER_FROM + " = ? and " + USER_TO_USER_TO
                + " = ?", new String[]{userId, friendId});
        Log.d(TAG, " deleted friendship  from: " + userId + " to: " + friendId + " success: " + (i == 1));
        return  i == 1;
    }

    public void putLetter(Letter letter){
        if(!isUserExist(letter.getSender())){
            Log.d(TAG , " note exist user added ");
            putUser(new User(letter.getSender(), null, null, null));
        }
        ContentValues values = letterToContentValues(letter);
        getWritableDatabase().insert(LETTER, null, values);
        Log.d(TAG, " insert  letter: " + letter.getId());
        values.clear();
        if(letter.isChatField()){
            if(!isChatExist(letter.getReceiver())) return;
            values.put(CHAT_TO_LETTER_FROM, letter.getReceiver());
            values.put(CHAT_TO_LETTER_TO, letter.getId());
            getWritableDatabase().insert(CHAT_TO_LETTER, null, values);
            Log.d(TAG, " added letter to chat: " + letter.getReceiver() + " letter id: " + letter.getId());
        }else{
            if(!isUserExist(letter.getReceiver())){
                return;
            }
            values.put(USER_TO_LETTER_FROM, letter.getReceiver());
            values.put(USER_TO_LETTER_TO, letter.getId());
            getWritableDatabase().insert(USER_TO_LETTER, null, values);
            Log.d(TAG, " added letter to user: " + letter.getReceiver() + " letter id: " + letter.getId());
        }
    }

    public boolean authorizeUser(String login, String password){
        boolean isValid = false;
        Cursor cursor = getReadableDatabase().rawQuery(CHECK_USER_ID_PASSWORD
                , new String[]{login, password});
        if(cursor.moveToFirst()){
            int index = cursor.getColumnIndex(IS_HERE);
            isValid = cursor.getInt(index) == 1;
        }
        cursor.close();
        return isValid;
    }


    public void updateLetterState(long letterId , String state){
        ContentValues values = new ContentValues();
        values.put(LETTER_STATE, state);
        int i = getWritableDatabase().update(LETTER, values
                , LETTER_ID + " =  ? ", new String[]{"" + letterId});
        Log.d(TAG, " updated " + i);
    }



    public void putChat(String chatTitle){
        if(isChatExist(chatTitle)) return;
        ContentValues values = new ContentValues();
        values.put(CHAT_ID, chatTitle);
        getWritableDatabase().insert(CHAT, null, values);
        Log.d(TAG , "inserted chat: " + chatTitle);

    }

    public boolean joinChat(String userId, String chatTitle){
        boolean success = false;
        if(isUserInChat(userId , chatTitle)) return success;
        if(!isChatExist(chatTitle)){
            putChat(chatTitle);
        }
        ContentValues values = new ContentValues();
        values.put(USER_TO_CHAT_FROM, userId);
        values.put(USER_TO_CHAT_TO, chatTitle);
        long result = getWritableDatabase().insert(USER_TO_CHAT, null, values);
        success = result != -1;
        Log.d(TAG , userId + " join chat: " + chatTitle + " sucess: " + result);
        return success;
    }

    public boolean leaveChat(String userId, String chatTitle){
        int i = getWritableDatabase().delete(USER_TO_CHAT
                , USER_TO_CHAT_FROM + " = ? and " + USER_TO_CHAT_TO
                + " = ?", new String[]{userId, chatTitle});
        Log.d(TAG ,userId + " leave chat: " + chatTitle + " success: " + (i == 1));
        return i == 1;
    }


    public void putLostRemoveFriend(String userId, String friendId){
        ContentValues values = new ContentValues();
        values.put(LOST_REMOVE_FRIEND_FROM, userId);
        values.put(LOST_REMOVE_FRIEND_TO, friendId);
        long i = getWritableDatabase().insert(LOST_REMOVE_FRIEND, null, values);
        Log.d(TAG , "put lost remove friend. User: " + userId + " Friend: " + friendId + " success: " + (i != -1));
    }

    public void removeLostRemoveFriend(String userId, String friendId){
        int i = getWritableDatabase().delete(LOST_REMOVE_FRIEND
                , LOST_REMOVE_FRIEND_FROM + " = ? and " + LOST_REMOVE_FRIEND_TO
                + " = ?", new String[]{userId, friendId});
        Log.d(TAG ,"removed lostremovefriend. user: " + userId + " friend:  " + friendId + " success: " + (i == 1));
    }


    public void putLostLeaveChat(String userId, String chatTitle){
        ContentValues values = new ContentValues();
        values.put(LOST_LEAVE_CHAT_FROM, userId);
        values.put(LOST_LEAVE_CHAT_TO, chatTitle);
        long i = getWritableDatabase().insert(LOST_LEAVE_CHAT, null, values);
        Log.d(TAG , "putt lost leave chat. User: " + userId + " Chat: " + chatTitle + " success: " + (i != -1));
    }


    public void removeLostLeaveChat(String userId, String chatTitle){
        int i = getWritableDatabase().delete(LOST_LEAVE_CHAT
                , LOST_LEAVE_CHAT_FROM + " = ? and " + LOST_LEAVE_CHAT_TO
                + " = ?", new String[]{userId, chatTitle});
        Log.d(TAG ,"removed removeLostLeaveChat. user: " + userId + " chatTitle:  " + chatTitle + " success: " + (i == 1));
    }


    public void changeUpdateState(String userId , boolean updated){
        ContentValues values = new ContentValues();
        values.put(USER_UPDATE_POSTED, updated);
        int i = getWritableDatabase().update(USER, values
                , USER_ID + " =  ? ", new String[]{userId});
        Log.d(TAG, "change updated state: "+ updated +" for user: "  + userId + " success: " + (i != 0));
    }

    public Cursor getFriendsCursor(User  user){
        return getReadableDatabase().rawQuery(SELECT_FRIENDS_BY_USER_ID
                , new String[]{user.getId()});
    }

    public Cursor getFriendDialogCursor(String userId, String  friendId){
        return getReadableDatabase().rawQuery(SELECT_USER_DIALOG_BY_IDS
                , new String[]{userId, userId, friendId, friendId, userId});
    }

    public Cursor getJoinedChatCursor(String userId){
        return getReadableDatabase().rawQuery(SELECT_CHATS_BY_USER_ID , new String[]{userId});
    }

    public Cursor getChatDialogCursor(String chatTitle){
        return getReadableDatabase().rawQuery(SELECT_CHAT_DIALOG
                , new String[]{chatTitle});
    }

    public User getUser(String userId){
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_USER_BY_ID , new String[]{userId});
        cursor.close();
        return cursor.moveToFirst() ?
                new User(
                cursor.getString(cursor.getColumnIndex(USER_ID))
                ,cursor.getString(cursor.getColumnIndex(USER_PASSWORD))
                ,cursor.getString(cursor.getColumnIndex(USER_PUBLIC_NAME))
                ,cursor.getBlob(cursor.getColumnIndex(USER_PHOTO))
                )  : null ;
    }




    public Set<String> getLostRemoveFriend(String userId){
        Set<String> lostFriendIds = Collections.EMPTY_SET;
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_LOST_REMOVE_FRIEND , new String[]{userId});
        if(cursor.moveToFirst()){
            lostFriendIds = new HashSet<>();
            do{
                lostFriendIds.add(cursor.getString(cursor.getColumnIndex(LOST_REMOVE_FRIEND_TO)));
            }while(cursor.moveToNext());
        }
        cursor.close();
        return lostFriendIds;
    }

    public Set<String> getLostLeaveChat(String userId){
        Set<String> lostFriendIds = Collections.EMPTY_SET;
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_LOST_LEAVE_CHAT , new String[]{userId});
        if(cursor.moveToFirst()){
            lostFriendIds = new HashSet<>();
            do{
                lostFriendIds.add(cursor.getString(cursor.getColumnIndex(LOST_LEAVE_CHAT_TO)));
            }while(cursor.moveToNext());
        }
        cursor.close();
        return lostFriendIds;
    }

    public Set<Letter> getLostPostLetter(String userId){
        Set<Letter> letters = new HashSet<>();
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_LOST_USER_LETTERS , new String[]{userId});
        if(cursor.moveToFirst()){
            do{
                letters.add(new Letter(
                        cursor.getString(cursor.getColumnIndex(LETTER_SENDER))
                        ,cursor.getString(cursor.getColumnIndex(LETTER_RECEIVER))
                        ,cursor.getString(cursor.getColumnIndex(LETTER_STATE))
                        ,cursor.getBlob(cursor.getColumnIndex(LETTER_DATA))
                        ,cursor.getLong(cursor.getColumnIndex(LETTER_DATE))
                        ,cursor.getString(cursor.getColumnIndex(LETTER_CONTENT_TYPE))
                        , false
                ));
            }while(cursor.moveToNext());
        }
        cursor = getReadableDatabase().rawQuery(SELECT_LOST_CHAT_LETTERS , new String[]{userId});
        if(cursor.moveToFirst()){
            do{
                letters.add(new Letter(
                        cursor.getString(cursor.getColumnIndex(LETTER_SENDER))
                        ,cursor.getString(cursor.getColumnIndex(LETTER_RECEIVER))
                        ,cursor.getString(cursor.getColumnIndex(LETTER_STATE))
                        ,cursor.getBlob(cursor.getColumnIndex(LETTER_DATA))
                        ,cursor.getLong(cursor.getColumnIndex(LETTER_DATE))
                        ,cursor.getString(cursor.getColumnIndex(LETTER_CONTENT_TYPE))
                        , true
                ));
            }while(cursor.moveToNext());
        }
        cursor.close();
        return letters;
    }

    private boolean isUserExist(User user){
        return isUserExist(user.getId());
    }

    private boolean isUserExist(String userId){
        boolean exist = false;
        Cursor cursor = getReadableDatabase().rawQuery(IS_USER_EXIST
                , new String[]{userId});
        if(cursor.moveToFirst())
            exist = 1 == cursor.getInt(cursor.getColumnIndex(IS_HERE));
        cursor.close();
        return exist;
    }

    public boolean isUserInChat(String userId , String chatTitle){
        boolean exist = false;
        Cursor cursor = getReadableDatabase().rawQuery(IS_USER_IN_CHAT
                , new String[]{userId , chatTitle});
        if(cursor.moveToFirst())
            exist = 1 == cursor.getInt(cursor.getColumnIndex(IS_HERE));
        cursor.close();
        return exist;
    }

    public boolean isChatExist(String chatTitle){
        boolean exist = false;
        Cursor cursor = getReadableDatabase().rawQuery(IS_CHAT_EXIST
                , new String[]{chatTitle});
        if(cursor.moveToFirst())
            exist = 1 == cursor.getInt(cursor.getColumnIndex(IS_HERE));
        cursor.close();
        return exist;
    }

    public boolean isFriendshipExist(String userId , String friendId){
        boolean exist = false;
        Cursor cursor = getReadableDatabase().rawQuery(CHECK_USER_FRIENDSHIP
                , new String[]{ userId , friendId});
        if(cursor.moveToFirst())
            exist = 1 == cursor.getInt(cursor.getColumnIndex(IS_HERE));
        cursor.close();
        return exist;
    }



    private ContentValues userToContentValues(User user){
        ContentValues values = new ContentValues();
        values.put(USER_ID, user.getId());
        values.put(USER_PUBLIC_NAME, user.getPublicName());
        values.put(USER_PHOTO, user.getPhoto());
        values.put(USER_PASSWORD, user.getPassword());
        return values;
    }

    private ContentValues letterToContentValues(Letter letter){
        ContentValues values = new ContentValues();
        values.put(LETTER_ID, letter.getId());
        values.put(LETTER_DATE, letter.getDate());
        values.put(LETTER_DATA, letter.getData());
        values.put(LETTER_CONTENT_TYPE, letter.getContentType());
        values.put(LETTER_SENDER , letter.getSender());
        values.put(LETTER_STATE , letter.getState());
        return values;
    }

}