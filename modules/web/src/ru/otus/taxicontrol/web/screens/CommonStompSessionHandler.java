package ru.otus.taxicontrol.web.screens;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;
import java.util.function.Function;


public class CommonStompSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(CommonStompSessionHandler.class);
    private StompSession session;
    private final Function<Object, String> webSocketMessageHandler;
    private String[] subscribePoints;

    public CommonStompSessionHandler(Function<Object, String> webSocketMessageHandler) {
        this.webSocketMessageHandler = webSocketMessageHandler;
    }

    public void setSubscribePoints(String... subscribePoints) {
        this.subscribePoints = subscribePoints;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        this.session = session;
        logger.info("New websocket session established : " + session.getSessionId());
        for (String subscribePoint: subscribePoints) {
            session.subscribe(subscribePoint, this);
            logger.info("Subscribed to: " + subscribePoint);
        }
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error("Got an exception", exception);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return Object.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        if (webSocketMessageHandler != null) {
            webSocketMessageHandler.apply(payload);
        }
    }

    public StompSession getSession() {
        return session;
    }
}
