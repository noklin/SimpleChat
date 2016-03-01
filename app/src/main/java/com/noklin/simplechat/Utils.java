package com.noklin.simplechat;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

import com.noklin.simplechat.database.Letter;
import com.noklin.simplechat.database.User;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static com.noklin.simplechat.database.ApplicationDatabase.Entities.*;

public class Utils {


    public static Bundle letterToBundle(Letter letter){
        Bundle data = new Bundle();
        data.putString(LETTER_SENDER , letter.getSender());
        data.putString(LETTER_RECEIVER , letter.getReceiver());
        data.putString(LETTER_STATE, letter.getState());
        data.putByteArray(LETTER_DATA, letter.getData());
        data.putLong(LETTER_DATE, letter.getDate());
        data.putString(LETTER_CONTENT_TYPE, letter.getContentType());
        data.putBoolean(LETTER_IS_CHAT_FIELD, letter.isChatField());
        return data;
    }

    public static Letter bundleToLetter(Bundle data){
        return new Letter(
                data.getString(LETTER_SENDER)
                ,data.getString(LETTER_RECEIVER)
                ,data.getString(LETTER_STATE)
                ,data.getByteArray(LETTER_DATA)
                ,data.getLong(LETTER_DATE)
                ,data.getString(LETTER_CONTENT_TYPE)
                ,data.getBoolean(LETTER_IS_CHAT_FIELD)
        );
    }

    public static Bundle userToBundle(User user){
        Bundle data = new Bundle();
        data.putString(USER_LOGIN , user.getLogin());
        data.putString(USER_PASSWORD , user.getPassword());
        data.putString(USER_PUBLIC_NAME , user.getPublicName());
        data.putByteArray(USER_PHOTO , user.getPhoto());
        return data;
    }

    public static User bundleToUser(Bundle data){
        return new User(
                data.getString(USER_LOGIN)
                ,data.getString(USER_PASSWORD)
                ,data.getString(USER_PUBLIC_NAME)
                ,data.getByteArray(USER_PHOTO)
        );
    }



    public static User cursorToUser(Cursor cursor){
        return new User(
                cursor.getString(cursor.getColumnIndex(USER_ID))
                , null
                ,cursor.getString(cursor.getColumnIndex(USER_PUBLIC_NAME))
                ,cursor.getBlob(cursor.getColumnIndex(USER_PHOTO))
        );
    }



    public static File createImageFile(long date , String author) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
        String imageFileName = "/" + author + "_" + timeStamp;
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if(!storageDir.exists()){
            storageDir.mkdirs();
        }
       return new File(storageDir.getAbsolutePath() + imageFileName + ".jpg");
    }

}


