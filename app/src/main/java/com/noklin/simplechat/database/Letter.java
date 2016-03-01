package com.noklin.simplechat.database;

public class Letter {

    private final String mSender;
    private final String mReceiver;
    private final String mState;
    private final String mContentType;
    private byte[] mData;
    private long mDate;
    private final long mId;
    private final boolean mChatField;


    public Letter(String sender, String receiver, String state, byte[] data , long date
            , String contentType , boolean chatField){

        mId = getId(sender , date);
        mChatField = chatField;
        mContentType = contentType;
        mSender = sender;
        mReceiver = receiver;
        mState = state;
        mData = data;
        mDate = date;

    }

    public void setDate(long date) {
        mDate = date;
    }

    public boolean isChatField() {
        return mChatField;
    }

    public long getId() {
        return mId;
    }

    public String getSender() {
        return mSender;
    }

    public String getReceiver() {
        return mReceiver;
    }

    public String getState() {
        return mState;
    }

    public String getContentType() {
        return mContentType;
    }

    public byte[] getData() {
        return mData;
    }

    public long getDate() {
        return mDate;
    }

    private long getId(String sender , long date){
        return sender.hashCode() + date;
    }

    public void setData(byte[] data) {
        mData = data;
    }
}