package ru.otus.taxicontrol.web.screens;

import java.util.function.Function;

public class ClientStompSessionHandler extends CommonStompSessionHandler {
    public ClientStompSessionHandler(Function<Object, String> webSocketMessageHandler) {
        super(webSocketMessageHandler);
    }
}
