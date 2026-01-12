package com.trading.hf.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.trading.hf.model.MarketEvent;
import com.trading.hf.model.MarketEventFactory;
import com.trading.hf.pnl.PnlHandler;
import com.trading.hf.ui.UIBroadcastHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DisruptorOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(DisruptorOrchestrator.class);

    private final Disruptor<MarketEvent> disruptor;
    private final RingBuffer<MarketEvent> ringBuffer;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public DisruptorOrchestrator(int bufferSize, OptionChainHandler optionChainHandler, SentimentHandler sentimentHandler, PatternMatcherHandler patternMatcherHandler, ExecutionHandler executionHandler, UIBroadcastHandler uiBroadcastHandler, PnlHandler pnlHandler) {
        // 1. Create a thread pool for the consumers
        executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);

        // 2. The factory for the event
        MarketEventFactory factory = new MarketEventFactory();

        // 3. Construct the Disruptor
        disruptor = new Disruptor<>(factory, bufferSize, DaemonThreadFactory.INSTANCE);

        // 4. Connect the handlers in a chain
        disruptor.handleEventsWith(optionChainHandler)
                 .then(sentimentHandler)
                 .then(patternMatcherHandler)
                 .then(executionHandler)
                 .then(uiBroadcastHandler, pnlHandler);

        // 5. Get the ring buffer from the Disruptor to be used for publishing
        ringBuffer = disruptor.getRingBuffer();
    }

    public void start() {
        disruptor.start();
    }

    public void shutdown() {
        disruptor.shutdown();
        executor.shutdown();
    }

    public void onBridgeMessage(String jsonMessage) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonMessage);
            String typeStr = rootNode.get("type").asText();
            MarketEvent.MessageType messageType = MarketEvent.MessageType.valueOf(typeStr.toUpperCase());
            JsonNode payload = rootNode.get("data");
            long timestamp = rootNode.get("timestamp").asLong();

            ringBuffer.publishEvent((event, sequence, type, data, ts) -> {
                event.setType(type);

                // For MARKET_UPDATE, the payload is the nested 'data' object
                // For other types, it might be different, but we'll stick to the contract
                event.setPayload(data);
                event.setTimestamp(ts);
            }, messageType, payload, timestamp);

        } catch (Exception e) {
            log.error("Error processing message from bridge: {}", jsonMessage, e);
        }
    }
}
