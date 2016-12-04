package com.loony.timelapsemaker.http_server.api;

/**
 * Created by Kamil on 12/4/2016.
 */


public enum State {
    UNCONNECTED,
    CONNECTING,
    OPEN,
    CLOSING,
    CLOSED
}