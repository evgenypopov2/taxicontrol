package ru.otus.taxicontrol.web.screens;

import java.util.function.Function;

public class TaxiStompSessionHandler extends CommonStompSessionHandler {
    public TaxiStompSessionHandler(Function<Object, String> webSocketMessageHandler) {
        super(webSocketMessageHandler);
    }
}
