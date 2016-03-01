package com.noklin.simplechat;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

public class SearchChatActivity extends BaseChatActivity{

    private SearchChatListFragment mSearchChatListFragment;
    private final static String TAG = SearchChatActivity.class.getSimpleName();

    @Override
    protected void onSearchedGroupChat(String chatId) {
        mSearchChatListFragment.addChatToList(chatId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        addSearchFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_search, menu);
        menu.findItem(R.id.search);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        return true;
    }


    @Override
    public boolean onSearchRequested() {
        return false;
    }

    public static class SearchChatListFragment extends ListFragment {

        private final static String TAG = SearchChatListFragment.class.getSimpleName();


        private SearchChatListAdapter mSearchChatListAdapter;
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mSearchChatListAdapter = new SearchChatListAdapter(new ArrayList<String>());
            setListAdapter(mSearchChatListAdapter);
            setRetainInstance(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = super.onCreateView(inflater, container, savedInstanceState);
            ListView listView;
            if (root != null) {
                listView = (ListView)root.findViewById(android.R.id.list);
                registerForContextMenu(listView);
            }
            return root;
        }

        public void addChatToList(String chatTitle){
            mSearchChatListAdapter.add(chatTitle);
        }

        public void clearChatList(){
            mSearchChatListAdapter.clear();
        }




    class SearchChatListAdapter extends ArrayAdapter<String> {
            public SearchChatListAdapter(List<String> objects) {
                super(getActivity(), 0, objects);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater()
                            .inflate(R.layout.list_item_chat, null);

                }
                String chatTitle = getItem(position);
                TextView title = (TextView)convertView.findViewById(R.id.chat_title);
                title.setText(chatTitle);
                return convertView;

            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.search_chat_context_menu, menu);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            switch(item.getItemId()){
                case R.id.action_join_chat:
                    String chatId = mSearchChatListAdapter.getItem(info.position);
                    ChatCommunicator communicator = (ChatCommunicator)getActivity();
                    communicator.joinChat(chatId);
                    return true;
            }
            return super.onContextItemSelected(item);
        }
    }

    private void addSearchFragment(){

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG);
        if(fragment == null){
            mSearchChatListFragment = new SearchChatListFragment();
            fm.beginTransaction().add(R.id.search_user_fragment_container
                    , mSearchChatListFragment, TAG).commit();
        }else{
            mSearchChatListFragment = (SearchChatListFragment) fragment;
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mSearchChatListFragment.clearChatList();
            findChat(intent.getStringExtra(SearchManager.QUERY));
        }
    }
}
