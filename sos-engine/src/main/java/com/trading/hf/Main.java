package com.trading.hf;

import com.trading.hf.core.*;
import com.trading.hf.execution.OrderOrchestrator;
import com.trading.hf.model.PatternState;
import com.trading.hf.patterns.PatternStateMachine;
import com.trading.hf.state.RecoveryManager;
import com.trading.hf.streamer.TVMarketDataStreamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 1. Initialize core components
        RecoveryManager recoveryManager = new RecoveryManager();
        OrderOrchestrator orderOrchestrator = new OrderOrchestrator();

        // 2. Set up the Disruptor handlers
        SentimentHandler sentimentHandler = new SentimentHandler();
        PatternMatcherHandler patternMatcherHandler = new PatternMatcherHandler();
        ExecutionHandler executionHandler = new ExecutionHandler(orderOrchestrator);

        // 3. Initialize and start the Disruptor orchestrator
        int bufferSize = Config.getInt("disruptor.bufferSize", 1024);
        DisruptorOrchestrator orchestrator = new DisruptorOrchestrator(
                bufferSize,
                sentimentHandler,
                patternMatcherHandler,
                executionHandler
        );
        orchestrator.start();
        log.info("DisruptorOrchestrator started with buffer size {}.", bufferSize);

        // 4. Load previous state (if any)
        patternMatcherHandler.restoreState(recoveryManager.loadState());

        // 5. Add a shutdown hook to save the state on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down... saving state.");
            // Convert the map of state machines to a map of states for persistence
            Map<String, PatternState> statesToSave = patternMatcherHandler.getActiveStateMachines().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getState()));
            recoveryManager.saveState(statesToSave);
            orchestrator.shutdown();
        }));

        // 6. Connect to the Python bridge
        try {
            String websocketUri = Config.get("websocket.uri");
            URI uri = new URI(websocketUri);
            TVMarketDataStreamer streamer = new TVMarketDataStreamer(uri, orchestrator);
            log.info("Connecting to Python Bridge at {}", uri);
            streamer.connectBlocking(); // Use connect() for non-blocking
        } catch (URISyntaxException | InterruptedException e) {
            log.error("Failed to connect to WebSocket bridge:", e);
            orchestrator.shutdown();
        }
    }
}
