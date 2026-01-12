package com.trading.hf.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import com.trading.hf.model.MarketEvent;
import com.trading.hf.model.PatternDefinition;
import com.trading.hf.model.PatternState;
import com.trading.hf.model.VolumeBar;
import com.trading.hf.patterns.GenericPatternParser;
import com.trading.hf.patterns.PatternStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PatternMatcherHandler implements EventHandler<MarketEvent> {
    private static final Logger log = LoggerFactory.getLogger(PatternMatcherHandler.class);

    private final Map<String, PatternDefinition> patternDefinitions;
    private final Map<String, PatternStateMachine> activeStateMachines = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PatternMatcherHandler() {
        // Load all pattern definitions on startup
        GenericPatternParser parser = new GenericPatternParser();
        this.patternDefinitions = parser.loadPatterns("strategies");
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) throws Exception {
        // Clear any trigger from a previous event in the same slot
        event.setTriggeredMachine(null);

        if (event.getType() == MarketEvent.MessageType.MARKET_UPDATE || event.getType() == MarketEvent.MessageType.CANDLE_UPDATE) {
            VolumeBar candle = event.getCandle();
            if (candle != null) {
                String symbol = event.getSymbol();
                candle.setSymbol(symbol);

                // For each defined pattern, check or create a state machine
                for (PatternDefinition definition : patternDefinitions.values()) {
                    String machineKey = symbol + ":" + definition.getPatternId();
                    PatternStateMachine stateMachine = activeStateMachines.computeIfAbsent(machineKey,
                            k -> new PatternStateMachine(definition, symbol));

                    // Evaluate the current candle against the state machine
                    stateMachine.evaluate(candle);

                    // If the final phase is completed, pass the machine to the next handler
                    if (stateMachine.isTriggered()) {
                        event.setTriggeredMachine(stateMachine);
                        stateMachine.consumeTrigger(); // Reset trigger flag
                        // We break after the first trigger for a symbol to avoid multiple signals
                        break;
                    }
                }
            }
        }
    }

    public Map<String, PatternStateMachine> getActiveStateMachines() {
        return activeStateMachines;
    }

    public void restoreState(Map<String, PatternState> savedStates) {
        for (Map.Entry<String, PatternState> entry : savedStates.entrySet()) {
            String machineKey = entry.getKey();
            PatternState savedState = entry.getValue();
            PatternDefinition definition = patternDefinitions.get(savedState.getPatternId());
            if (definition != null) {
                // Create a new state machine with the loaded state
                PatternStateMachine stateMachine = new PatternStateMachine(definition, savedState.getSymbol(), savedState);
                activeStateMachines.put(machineKey, stateMachine);
            } else {
                log.warn("Could not find pattern definition for loaded state: {}", savedState.getPatternId());
            }
        }
    }
}
