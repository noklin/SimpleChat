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

import com.noklin.simplechat.database.User;

import java.util.ArrayList;
import java.util.List;

public class SearchUserActivity extends BaseChatActivity{
    private SearchUserListFragment mSearchUserListFragment;
    private final static String TAG = SearchUserActivity.class.getSimpleName();

    @Override
    protected void onSearchedUser(User searchedUser) {
        mSearchUserListFragment.addUserToList(searchedUser);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        addSearchFragment();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_search, menu);
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

    public static class SearchUserListFragment extends ListFragment {

        private final static String TAG = SearchUserListFragment.class.getSimpleName();

        private SearchUserListAdapter mSearchUserListAdapter;
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mSearchUserListAdapter = new SearchUserListAdapter(new ArrayList<User>());
            setListAdapter(mSearchUserListAdapter);
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

        public void addUserToList(User u){
            mSearchUserListAdapter.add(u);
        }

        public void clearUserList(){
            mSearchUserListAdapter.clear();
        }

        class SearchUserListAdapter  extends ArrayAdapter<User> {
            public SearchUserListAdapter(List<User> objects) {
                super(getActivity(), 0, objects);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater()
                            .inflate(R.layout.list_item_user, null);

                }
                User user = getItem(position);
                TextView tv = (TextView)convertView.findViewById(R.id.user_public_name);
                String publicName;
                if(user.getPublicName() == null || user.getPublicName().isEmpty()){
                    publicName = user.getLogin();
                }else{
                    publicName = user.getPublicName();
                }
                tv.setText(publicName);
                return convertView;
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.search_user_context_menu, menu);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            switch(item.getItemId()){
                case R.id.action_add_to_friend_list:
                    User friend = mSearchUserListAdapter.getItem(info.position);
                    ChatCommunicator communicator = (ChatCommunicator)getActivity();
                    communicator.addFriend(friend);
                    break;
            }
            return super.onContextItemSelected(item);
        }
    }

    private void addSearchFragment(){

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG);
        if(fragment == null){
            mSearchUserListFragment = new SearchUserListFragment();
            fm.beginTransaction().add(R.id.search_user_fragment_container
                    , mSearchUserListFragment, TAG).commit();
        }else{
            mSearchUserListFragment = (SearchUserListFragment) fragment;
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mSearchUserListFragment.clearUserList();
            findUser(intent.getStringExtra(SearchManager.QUERY));
        }
    }
}
