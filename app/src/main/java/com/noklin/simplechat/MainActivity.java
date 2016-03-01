package com.noklin.simplechat;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;


import static com.noklin.simplechat.database.ApplicationDatabase.Entities.*;


public class MainActivity extends BaseChatActivity{
    private static final String TAG = MainActivity.class.getSimpleName();

    private ViewPager mViewPager;
    private MenuItem mAddNewItem;

    private FriendListFragment mFriendListFragment;
    private ChatListFragment mChatListFragment;


    @Override
    protected void onUpdatedFriendList() {
        mFriendListFragment.updateFriendList();
    }

    @Override
    protected void onUpdatedChatsList() {
        mChatListFragment.updateChatList();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(sectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }


    private ViewPager.OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
        @Override
        public void onPageScrollStateChanged(int state) {}
        @Override
        public void onPageSelected(int position) {
            if(mAddNewItem == null) return;
            switch(position){
                case 0:
                    mAddNewItem.setVisible(false);
                    break;
                case 1:
                    mAddNewItem.setVisible(true);
                    break;
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mAddNewItem = menu.findItem(R.id.add_new);
        if(mViewPager.getCurrentItem() == 0){
            mAddNewItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id){
            case R.id.action_logout:
                clearUser();
                Intent i = new Intent(this , AuthorizeActivity.class);
                startActivity(i);
                finish();
                return true;
            case R.id.move_to_search:
                i = new Intent(this , SearchUserActivity.class);
                if(mViewPager.getCurrentItem() == 1){
                    i.setClass(this , SearchChatActivity.class);
                }
                i.putExtras(Utils.userToBundle(mCurrentUser));
                startActivity(i);
                return true;
            case R.id.add_new:
                ChatAttributeDialog dialog = new ChatAttributeDialog();
                dialog.show(getSupportFragmentManager() , ChatAttributeDialog.class.getSimpleName());

                return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public class SectionsPagerAdapter extends FragmentPagerAdapter {


        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return FriendListFragment.newInstance(mCurrentUser);
                case 1:
                    return ChatListFragment.newInstance(mCurrentUser);
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Friends";
                case 1:
                    return "Group chats";
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object instance = super.instantiateItem(container, position);
            if(position == 0){
                mFriendListFragment = (FriendListFragment)instance;
            }else if(position == 1){
                mChatListFragment = (ChatListFragment)instance;
            }
            return instance;
        }
    }

    private void clearUser(){
        getSharedPreferences(USER, Context.MODE_PRIVATE).edit().remove(USER_LOGIN)
                .remove(USER_PASSWORD).commit();

    }

    public static class ChatAttributeDialog extends DialogFragment {
        private TextView mChatNameField;
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            getDialog().setTitle("New chat");
            View root = inflater.inflate(R.layout.fragment_new_chat, container , false);
            mChatNameField = (EditText)root.findViewById(R.id.chat_title);
            root.findViewById(R.id.btn_create_dialog).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChatCommunicator communicator = (ChatCommunicator)getActivity();
                    communicator.joinChat(mChatNameField.getText().toString());
                    ChatAttributeDialog.this.dismiss();
                }
            });
            return root;
        }
    }
}

