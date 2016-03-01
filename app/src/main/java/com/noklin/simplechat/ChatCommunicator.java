package com.noklin.simplechat;

import com.noklin.simplechat.database.Letter;
import com.noklin.simplechat.database.User;

public interface ChatCommunicator {
    void findUser(String query);
    void addFriend(User user);
    void removeFriend(String friendId);
    void findChat(String query);
    void joinChat(String chatId);
    void leaveFromChat(String chatId);
    void postLetter(Letter letter);
    void closeChatConnection();
}
