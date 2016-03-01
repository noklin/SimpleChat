package com.noklin.simplechat.database;

public class User {

    private final String mLogin;
    private final String mPassword;
    private final String mPublicName;
    private final byte[] mPhoto;


    public User(String login , String password, String publicName , byte[] photo){
        mLogin = login;
        mPassword = password;
        mPublicName = publicName;
        mPhoto = photo;
    }


    public String getId(){
        return mLogin;
    }

    public String getLogin() {
        return mLogin;
    }

    public String getPassword() {
        return mPassword;
    }

    public String getPublicName() {
        return mPublicName;
    }

    public byte[] getPhoto() {
        return mPhoto;
    }

    @Override
    public int hashCode() {
        return mLogin.hashCode();
    }


    //
//    @Override
//    public String toString() {
//        return "User{" +
//                "mLogin='" + mLogin + '\'' +
//                ", mName='" + mName + '\'' +
//                ", mPhoto=" + Arrays.toString(mPhoto) +
//                ", mPassword='" + mPassword + '\'' +
//                '}';
//    }
//
//
//    public static final String LOGIN = "LOGIN";
//    public static final String PASSWORD = "PASSWORD";
//    public static final String PUBLIC_NAME = "PUBLIC_NAME";
//    public static final String PHOTO = "PHOTO";
//    public static final String USER_INFO = "USER_INFO";
//    public static final String INFO_STATUS = "USER_INFO_STATUS";
}