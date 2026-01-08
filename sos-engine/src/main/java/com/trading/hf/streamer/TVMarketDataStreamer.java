package com.trading.hf.streamer;

import com.trading.hf.core.DisruptorOrchestrator;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class TVMarketDataStreamer extends WebSocketClient {
    private static final Logger log = LoggerFactory.getLogger(TVMarketDataStreamer.class);

    private final DisruptorOrchestrator disruptorOrchestrator;

    public TVMarketDataStreamer(URI serverUri, DisruptorOrchestrator disruptorOrchestrator) {
        super(serverUri);
        this.disruptorOrchestrator = disruptorOrchestrator;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Connected to Python Bridge at {}", getURI());
    }

    @Override
    public void onMessage(String message) {
        log.debug("Received message: {}", message);
        disruptorOrchestrator.onBridgeMessage(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("Disconnected from Python Bridge. Reason: {}", reason);
    }

    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error:", ex);
    }
}
