package com.noklin.simplechat;

import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.noklin.network.packets.FailPacket;
import com.noklin.network.packets.Packet;
import com.noklin.network.packets.clientpackets.RegistratePacket;
import com.noklin.network.simplechat.ChatConnection;
import com.noklin.simplechat.database.ChatDatabaseManager;
import com.noklin.simplechat.database.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import static com.noklin.simplechat.database.ApplicationDatabase.Entities.*;


public class AuthorizeActivity extends AppCompatActivity{
    private static final String TAG = AuthorizeActivity.class.getSimpleName();
    private ChatDatabaseManager mChatDatabaseManager;
    private TextView mStatusTv;
    private EditText mLogin , mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorize);
        User currentUser = getCurrentUser();
        if(currentUser != null){
            moveToChatActivity(currentUser);
        }else{
            mStatusTv = (TextView)findViewById(R.id.auth_status_tv);
            mLogin = (EditText)findViewById(R.id.auth_login);
            mPassword = (EditText)findViewById(R.id.auth_password);
            mChatDatabaseManager = ChatDatabaseManager.getInstance(this);
        }


    }

    public void login(View v){
        String login , password;
        login = mLogin.getText().toString().trim();
        password = mPassword.getText().toString().trim();
        if(mChatDatabaseManager.authorizeUser(login, password)){
            changeCurrentUser(login , password);
            moveToChatActivity(login,password);
        }else{
            Registrant registrant = new Registrant();
            registrant.execute(login,password);
        }

    }


    private class Registrant extends AsyncTask<String,Void,Packet>{
        private Socket mSocket = new Socket();
        private SocketAddress mSocketAddress = new InetSocketAddress(getResources().getString(R.string.serverIp)
                , getResources().getInteger(R.integer.serverPort));


        @Override
        protected Packet doInBackground(String... params) {
            Packet input = null;
            String login , password;
            login = params[0];
            password = params[1];
            try{
                mSocket.connect(mSocketAddress);
                ChatConnection chatConnection = new ChatConnection(mSocket);
                chatConnection.sendPacket(new RegistratePacket(login,password));
                input = chatConnection.receivePacket();
            }catch(IOException ex){
                Log.d(TAG , "Reg ex: " + ex.getMessage());
            }
            return input;

        }

        @Override
        protected void onPostExecute(Packet packet) {
            if(packet ==null){
                mStatusTv.setText(R.string.connection_problem_toast);
                return;
            }
            if(packet.getType() == Packet.Type.REGISTRATE){
                mStatusTv.setText(R.string.authorize_success);
                RegistratePacket input = new RegistratePacket(packet);
                User registeredUser = new User(input.getLogin() , input.getPassword(), null , null);
                mChatDatabaseManager.putUser(registeredUser);
                changeCurrentUser(registeredUser.getLogin(), registeredUser.getPassword());
                moveToChatActivity(registeredUser);
            }
            if(packet.getType() == Packet.Type.FAIL){
                FailPacket failPacket = new FailPacket(packet);
                mStatusTv.setText(failPacket.getMessage());
            }
        }
    }

    private void changeCurrentUser(String login , String password){
        getSharedPreferences(USER ,Context.MODE_PRIVATE).edit().putString(USER_LOGIN , login)
                .putString(USER_PASSWORD, password).commit();
    }

    private void moveToChatActivity(User user){
        moveToChatActivity(user.getLogin() , user.getPassword());
    }

    private void moveToChatActivity(String userLogin , String userPassword){
        Intent i = new Intent(this , MainActivity.class);
        i.putExtra(USER_LOGIN, userLogin).putExtra(USER_PASSWORD , userPassword);
        startActivity(i);
        finish();
    }

    private User getCurrentUser(){
        User user = null;
        String login = getSharedPreferences(USER, Context.MODE_PRIVATE).getString(USER_LOGIN, null);
        String password = getSharedPreferences(USER, Context.MODE_PRIVATE).getString(USER_PASSWORD, null);
        Log.d(TAG, "cur login: " + login + "cur password: " + password);
        if(login != null && password != null){
            user = new User(login ,password , null ,null);
        }
        return user;
    }
}
