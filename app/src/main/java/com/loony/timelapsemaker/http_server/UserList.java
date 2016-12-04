package com.loony.timelapsemaker.http_server;

import com.loony.timelapsemaker.http_server.api.CloseCode;
import com.loony.timelapsemaker.http_server.api.WebSocket;

import java.io.IOException;
import java.util.Vector;

/**
 * Created by Kamil on 12/4/2016.
 */

public class UserList {
    Vector<UserSocket> list;

    public UserList() {
        list = new Vector<>();
    }

    public void addUser(UserSocket user) {
        list.add(user);
    }

    public void removeUser(UserSocket user) {
        list.remove(user);
    }

    public int userCount() {
        return list.size();
    }

    public void sendToAll(String str) {
        for (int i = 0; i < list.size(); i++) {
            UserSocket user = list.get(i);
            WebSocket ws = user.webSocket;
            if (ws != null) {
                try {
                    ws.send(str);
                } catch (IOException e) {
                    System.out.println("sending error.....");
                    try {
                        ws.close(CloseCode.InvalidFramePayloadData, "reqrement", false);
                    } catch (IOException e1) {
                        removeUser(user);
                    }
                }
            }
        }
    }

    public void disconectAll() {
        for (int i = 0; i < list.size(); i++) {
            UserSocket user = list.get(i);
            WebSocket ws = user.webSocket;
            if(ws != null){
                try {
                    ws.close(CloseCode.InvalidFramePayloadData, "reqrement", false);
                } catch (IOException e) {
                    removeUser(user);
                }
            }
        }
    }
}
