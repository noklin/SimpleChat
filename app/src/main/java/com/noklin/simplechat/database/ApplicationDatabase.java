package com.noklin.simplechat.database;

import java.util.Arrays;
import java.util.List;


public class ApplicationDatabase {

    public static final class Entities {


        public static final String NAME = "simple_chat";
        public static final int VERSION = 1;

        public static final String USER = "user";
        public static final String USER_ID = "_id";
        public static final String USER_PASSWORD = "password";
        public static final String USER_PUBLIC_NAME = "public_name";
        public static final String USER_PHOTO = "photo";
        public static final String USER_UPDATE_POSTED = "updated";

        public static final String USER_LOGIN = "login";
        public static final String FRIEND = "friend";


        public static final String LETTER = "letter";
        public static final String LETTER_ID = "_id";
        public static final String LETTER_DATE = "date";
        public static final String LETTER_DATA = "data";
        public static final String LETTER_CONTENT_TYPE = "content_type";
        public static final String LETTER_SENDER = "sender";
        public static final String LETTER_STATE = "state";

        public static final String LETTER_RECEIVER = "LETTER_RECEIVER";
        public static final String LETTER_STATUS_PREPARE = "PRE";
        public static final String LETTER_INPUT = "input";
        public static final String LETTER_IS_CHAT_FIELD = "LETTER_CHAT_FIELD";

        public static final String CHAT = "chat";
        public static final String CHAT_ID = "_id";

        public static final String CHAT_OPPONENT = "CHAT_OPPONENT";

        public static final String USER_TO_USER = "user_to_user";
        public static final String USER_TO_USER_FROM = "_from";
        public static final String USER_TO_USER_TO = "_to";


        public static final String LOST_REMOVE_FRIEND = "lost_remove_friend";
        public static final String LOST_REMOVE_FRIEND_FROM = "_from";
        public static final String LOST_REMOVE_FRIEND_TO = "_to";




        public static final String USER_TO_CHAT = "user_to_chat";
        public static final String USER_TO_CHAT_FROM = "user_id";
        public static final String USER_TO_CHAT_TO = "chat_id";

        public static final String LOST_LEAVE_CHAT = "lost_leave_chat";
        public static final String LOST_LEAVE_CHAT_FROM = "user_id";
        public static final String LOST_LEAVE_CHAT_TO = "chat_id";


        public static final String USER_TO_LETTER = "user_to_letter";
        public static final String USER_TO_LETTER_FROM = "user_id";
        public static final String USER_TO_LETTER_TO = "letter_id";

        public static final String CHAT_TO_LETTER = "chat_to_letter";
        public static final String CHAT_TO_LETTER_FROM = "chat_id";
        public static final String CHAT_TO_LETTER_TO = "letter_id";


        public static final String IS_HERE = "isHere";

        public static final String CREATE_TABLE_USER =
                "CREATE TABLE " + USER + "(" +
                        USER_ID + " VARCHAR(32) PRIMARY KEY, " +
                        USER_PASSWORD + " VARCHAR(32), " +
                        USER_PUBLIC_NAME + " VARCHAR(64), " +
                        USER_UPDATE_POSTED + " TINYINT(1) NOT NULL DEFAULT 0 , " +
                        USER_PHOTO + " BLOB " +
                        ")";



        public static final String CREATE_TABLE_LETTER =
                "CREATE TABLE " + LETTER + "(" +
                        LETTER_ID + " INTEGER PRIMARY KEY, " +
                        LETTER_DATE + " DATETIME NOT NULL, " +
                        LETTER_DATA + " BLOB NOT NULL, " +
                        LETTER_CONTENT_TYPE + " CHAR(3) NOT NULL " +
                        "CHECK(" + LETTER_CONTENT_TYPE + " IN ('TXT', 'PIC')), " +
                        LETTER_STATE + " CHAR(3) NOT NULL " +
                        "CHECK(" + LETTER_STATE + " IN ('PRE','POS','REC')), " +
                        LETTER_SENDER + " VARCHAR(32) NOT NULL, " +
                        "FOREIGN KEY (" + LETTER_SENDER + ") " +
                        "REFERENCES " + USER + " ( " + USER_ID + " ) " +
                        ")";

        public static final String CREATE_TABLE_CHAT =
                "CREATE TABLE " + CHAT + " ( " +
                        CHAT_ID + " VARCHAR(64) PRIMARY KEY " +
                        ")";


        public static final String CREATE_TABLE_USER_TO_USER =
                "CREATE TABLE " + USER_TO_USER + "(" +
                        USER_TO_USER_FROM + " VARCHAR(32), " +
                        USER_TO_USER_TO + " VARCHAR(32), " +
                        "PRIMARY KEY (" + USER_TO_USER_FROM + " , " + USER_TO_USER_TO + "), " +
                        "FOREIGN KEY ( " + USER_TO_USER_FROM + ") " +
                        "REFERENCES " + USER + " ( " + USER_ID + "), " +
                        "FOREIGN KEY ( " + USER_TO_USER_TO + ") " +
                        "REFERENCES " + USER + " ( " + USER_ID + ") " +
                        ")";

        public static final String CREATE_TABLE_USER_TO_CHAT =
                "CREATE TABLE " + USER_TO_CHAT + " ( " +
                        USER_TO_CHAT_FROM + " VARCHAR(32), " +
                        USER_TO_CHAT_TO + " VARCHAR(64), " +
                        "PRIMARY KEY (" + USER_TO_CHAT_FROM + " , " + USER_TO_CHAT_TO + "), " +
                        "FOREIGN KEY (" + USER_TO_CHAT_FROM + ") " +
                        "REFERENCES " + USER + " ( " + USER_ID + " ), " +
                        "FOREIGN KEY (" + USER_TO_CHAT_TO + ") " +
                        "REFERENCES " + CHAT + " ( " + CHAT_ID + " ) " +
                        ")";

        public static final String CREATE_TABLE_USER_TO_LETTER =
                "CREATE TABLE " + USER_TO_LETTER + "(" +
                        USER_TO_LETTER_FROM + " VARCHAR(32), " +
                        USER_TO_LETTER_TO + " INTEGER, " +
                        "PRIMARY KEY (" + USER_TO_LETTER_FROM + " , " + USER_TO_LETTER_TO + "), " +
                        "FOREIGN KEY ( " + USER_TO_LETTER_FROM + ") " +
                        "REFERENCES " + USER + " ( " + USER_ID + "), " +
                        "FOREIGN KEY ( " + USER_TO_LETTER_TO + ") " +
                        "REFERENCES " + LETTER + " ( " + LETTER_ID + ") " +
                        ")";

        public static final String CREATE_TABLE_CHAT_TO_LETTER =
                "CREATE TABLE " + CHAT_TO_LETTER + "(" +
                        CHAT_TO_LETTER_FROM + " VARCHAR(64), " +
                        CHAT_TO_LETTER_TO + " INTEGER, " +
                        "PRIMARY KEY (" + CHAT_TO_LETTER_FROM + " , " + CHAT_TO_LETTER_TO + "), " +
                        "FOREIGN KEY ( " + CHAT_TO_LETTER_FROM + ") " +
                        "REFERENCES " + CHAT + " ( " + CHAT_ID + "), " +
                        "FOREIGN KEY ( " + CHAT_TO_LETTER_TO + ") " +
                        "REFERENCES " + LETTER + " ( " + LETTER_ID + ") " +
                        ")";

        public static final String CRETE_TABLE_REMOVED_FRIENDS =
                "CREATE TABLE " + LOST_REMOVE_FRIEND + "(" +
                        LOST_REMOVE_FRIEND_FROM + " VARCHAR(64), " +
                        LOST_REMOVE_FRIEND_TO + " VARCHAR(64), " +
                        "PRIMARY KEY (" + LOST_REMOVE_FRIEND_FROM + " , " + LOST_REMOVE_FRIEND_TO + "), " +
                        "FOREIGN KEY ( " + LOST_REMOVE_FRIEND_FROM + ") " +
                        "REFERENCES " + USER + " ( " + USER_ID + "), " +
                        "FOREIGN KEY ( " + LOST_REMOVE_FRIEND_TO + ") " +
                        "REFERENCES " + USER + " ( " + USER_ID + ") " +
                        ")";

        public static final String CRETE_TABLE_LEAVED_CHATS =
                "CREATE TABLE " + LOST_LEAVE_CHAT + " ( " +
                        LOST_LEAVE_CHAT_FROM + " VARCHAR(32), " +
                        LOST_LEAVE_CHAT_TO + " VARCHAR(64), " +
                        "PRIMARY KEY (" + LOST_LEAVE_CHAT_FROM + " , " + LOST_LEAVE_CHAT_TO + "), " +
                        "FOREIGN KEY (" + LOST_LEAVE_CHAT_FROM + ") " +
                        "REFERENCES " + USER + " ( " + USER_ID + " ), " +
                        "FOREIGN KEY (" + LOST_LEAVE_CHAT_TO + ") " +
                        "REFERENCES " + CHAT + " ( " + CHAT_ID + " ) " +
                        ")";

        public static final String SELECT_FRIENDS_BY_USER_ID =
                " SELECT u." + USER_ID + " AS " + USER_ID +
                        ", u." + USER_PUBLIC_NAME + " AS " + USER_PUBLIC_NAME +
                        ", u." + USER_PHOTO + " AS " + USER_PHOTO +
                        " FROM " + USER + " u " +
                        " INNER JOIN " + USER_TO_USER + " utu " +
                        "ON utu." + USER_TO_USER_TO + " = u." + USER_ID +
                        " WHERE utu." + USER_TO_USER_FROM + " = ? ";


        public static final String SELECT_CHATS_BY_USER_ID =
                " SELECT " + USER_TO_CHAT_TO + " AS " + CHAT_ID +
                        " FROM " + USER_TO_CHAT  +
                        " WHERE " + USER_TO_CHAT_FROM + " = ? ";


        public static final String SELECT_USER_DIALOG_BY_IDS =
                " SELECT l." + LETTER_DATE + " AS " + LETTER_DATE +
                        ", l." + LETTER_CONTENT_TYPE + " AS " + LETTER_CONTENT_TYPE +
                        ", l." + LETTER_DATA + " AS " + LETTER_DATA +
                        ", l." + LETTER_SENDER + " AS " + LETTER_SENDER +
                        ", l." + LETTER_STATE + " AS " + LETTER_STATE +
                        ", utl." + USER_TO_CHAT_FROM + "  = ? AS " + LETTER_INPUT  +
                        ", l." + LETTER_ID + " AS " + LETTER_ID +
                        " FROM " + LETTER + " l " +
                        " INNER JOIN " + USER_TO_LETTER + " utl " +
                        " ON utl." + USER_TO_LETTER_TO + " = l." + LETTER_ID +
                        " WHERE (utl." + USER_TO_LETTER_FROM + " = ? AND l." + LETTER_SENDER + " = ? ) " +
                        " OR (utl." + USER_TO_LETTER_FROM + " = ? AND l." + LETTER_SENDER + " = ? ) " +
                        " ORDER BY l." + LETTER_DATE;

        public static final String IS_USER_EXIST =
                "SELECT  count(*) = 1 AS " + IS_HERE +
                        " FROM " + USER +
                        " WHERE " + USER_ID + " = ? ";

        public static final String CHECK_USER_FRIENDSHIP =
                "SELECT count(*) = 1 AS " + IS_HERE +
                        " FROM " + USER_TO_USER +
                        " WHERE " + USER_TO_USER_FROM + " = ? " +
                        " AND " + USER_TO_USER_TO + " = ? ";

        public static final String IS_USER_IN_CHAT =
                "SELECT count(*) = 1 AS " + IS_HERE +
                        " FROM " + USER_TO_CHAT +
                        " WHERE " + USER_TO_CHAT_FROM + " = ? " +
                        " AND " + USER_TO_CHAT_TO + " = ? ";

        public static final String IS_CHAT_EXIST =
                "SELECT count(*) = 1 AS " + IS_HERE +
                        " FROM " + CHAT +
                        " WHERE " + CHAT_ID + " = ? ";

        public static final String CHECK_USER_ID_PASSWORD =
                "SELECT  count(*) = 1 AS " + IS_HERE +
                        " FROM " + USER +
                        " WHERE " + USER_ID + " = ? AND " + USER_PASSWORD + " = ? ";

        public static final String SELECT_LOST_USER_LETTERS =
                " SELECT l." + LETTER_ID + " AS " + LETTER_ID +
                        ", l." + LETTER_DATE + " AS " + LETTER_DATE +
                        ", l." + LETTER_DATA + " AS " + LETTER_DATA +
                        ", l." + LETTER_CONTENT_TYPE + " AS " + LETTER_CONTENT_TYPE +
                        ", l." + LETTER_SENDER + " AS " + LETTER_SENDER +
                        ", l." + LETTER_STATE + " AS " + LETTER_STATE +
                        ", utl." + USER_TO_LETTER_FROM + " AS " + LETTER_RECEIVER +
                        " FROM " + LETTER + " l " +
                        " INNER JOIN " + USER_TO_LETTER + " utl " +
                        " ON utl." + USER_TO_LETTER_TO + " = l." + LETTER_ID +
                        " WHERE l." + LETTER_SENDER + " = ? AND l." + LETTER_STATE + " = '"
                        + LETTER_STATUS_PREPARE + "' ";

        public static final String SELECT_LOST_CHAT_LETTERS =
                " SELECT l." + LETTER_ID + " AS " + LETTER_ID +
                        ", l." + LETTER_DATE + " AS " + LETTER_DATE +
                        ", l." + LETTER_DATA + " AS " + LETTER_DATA +
                        ", l." + LETTER_CONTENT_TYPE + " AS " + LETTER_CONTENT_TYPE +
                        ", l." + LETTER_SENDER + " AS " + LETTER_SENDER +
                        ", l." + LETTER_STATE + " AS " + LETTER_STATE +
                        ", ctl." + CHAT_TO_LETTER_FROM + " AS " + LETTER_RECEIVER +
                        " FROM " + LETTER + " l " +
                        " INNER JOIN " + CHAT_TO_LETTER + " ctl " +
                        " ON ctl." + CHAT_TO_LETTER_TO + " = l." + LETTER_ID +
                        " WHERE l." + LETTER_SENDER + " = ? AND l." + LETTER_STATE + " = '"
                        + LETTER_STATUS_PREPARE + "' ";

        public static final String SELECT_LOST_REMOVE_FRIEND =
                " SELECT " + LOST_REMOVE_FRIEND_TO + " FROM " + LOST_REMOVE_FRIEND +
                " WHERE " + LOST_REMOVE_FRIEND_FROM + " = ? ";

        public static final String SELECT_LOST_LEAVE_CHAT =
                " SELECT " + LOST_LEAVE_CHAT_TO + " FROM " + LOST_LEAVE_CHAT +
                        " WHERE " + LOST_LEAVE_CHAT_FROM + " = ? ";


        public static final String SELECT_CHAT_DIALOG =
                " SELECT l." + LETTER_ID + " AS " + LETTER_ID +
                        ", l." + LETTER_DATE + " AS " + LETTER_DATE +
                        ", l." + LETTER_DATA + " AS " + LETTER_DATA +
                        ", l." + LETTER_CONTENT_TYPE + " AS " + LETTER_CONTENT_TYPE +
                        ", l." + LETTER_SENDER + " AS " + LETTER_SENDER +
                        ", l." + LETTER_STATE + " AS " + LETTER_STATE +
                        ", u." + USER_ID + " AS " + USER_ID +
                        ", u." + USER_PUBLIC_NAME + " AS " + USER_PUBLIC_NAME +
                        " FROM " + LETTER + " l " +
                        " INNER JOIN " + CHAT_TO_LETTER + " ctl " +
                        " ON ctl." + CHAT_TO_LETTER_TO + " = l." + LETTER_ID +
                        " INNER JOIN " + USER + " u " +
                        " ON u." + USER_ID + " = l." + LETTER_SENDER +
                        " WHERE ctl." + CHAT_TO_LETTER_FROM + " = ? " +
                        " ORDER BY l." + LETTER_DATE ;

        public static final String SELECT_USER_BY_ID =
                "SELECT " + USER_ID +
                        ", " + USER_PASSWORD +
                        ", " + USER_PUBLIC_NAME +
                        ", " + USER_PHOTO + " " +
                        "FROM " + USER + " WHERE " + USER_ID + " = ?";

    }
}