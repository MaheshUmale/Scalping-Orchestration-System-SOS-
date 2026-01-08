package com.trading.hf.patterns;

import com.trading.hf.model.PatternDefinition;
import com.trading.hf.model.PatternState;
import com.trading.hf.model.VolumeBar;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PatternStateMachine {
    private static final Logger log = LoggerFactory.getLogger(PatternStateMachine.class);

    private final PatternDefinition definition;
    private final PatternState state;
    private boolean triggered = false;

    // Compile MVEL expressions for performance
    private final Map<String, Serializable> compiledConditions = new HashMap<>();
    private final Map<String, Serializable> compiledCaptures = new HashMap<>();

    public PatternStateMachine(PatternDefinition definition, String symbol) {
        this.definition = definition;
        this.state = new PatternState(definition.getPatternId(), symbol, definition.getPhases().get(0).getId());
        compileExpressions();
    }

    public PatternStateMachine(PatternDefinition definition, String symbol, PatternState state) {
        this.definition = definition;
        this.state = state;
        compileExpressions();
    }

    private void compileExpressions() {
        for (PatternDefinition.Phase phase : definition.getPhases()) {
            if (phase.getConditions() != null) {
                for (String condition : phase.getConditions()) {
                    compiledConditions.put(condition, MVEL.compileExpression(condition));
                }
            }
            if (phase.getCapture() != null) {
                for (Map.Entry<String, String> entry : phase.getCapture().entrySet()) {
                    compiledCaptures.put(entry.getValue(), MVEL.compileExpression(entry.getValue()));
                }
            }
        }
    }

    public void evaluate(VolumeBar candle) {
        PatternDefinition.Phase currentPhase = getCurrentPhase();
        if (currentPhase == null) return;

        if (checkConditions(currentPhase.getConditions(), candle)) {
            captureVariables(currentPhase.getCapture(), candle);
            moveToNextPhase();
        } else {
            state.incrementTimeout();
            if (state.isTimedOut(currentPhase.getTimeout())) {
                state.reset();
            }
        }
    }

    private boolean checkConditions(java.util.List<String> conditions, VolumeBar candle) {
        if (conditions == null || conditions.isEmpty()) return true;

        Map<String, Object> context = new HashMap<>();
        context.put("candle", candle);
        context.put("var", state.getCapturedVariables());

        for (String condition : conditions) {
            try {
                Boolean result = MVEL.executeExpression(compiledConditions.get(condition), context, Boolean.class);
                if (result == null || !result) {
                    return false;
                }
            } catch (Exception e) {
                log.error("Error evaluating MVEL condition: {}", condition, e);
                return false;
            }
        }
        return true;
    }

    private void captureVariables(Map<String, String> captures, VolumeBar candle) {
        if (captures == null) return;

        Map<String, Object> context = new HashMap<>();
        context.put("candle", candle);

        for (Map.Entry<String, String> entry : captures.entrySet()) {
            try {
                Object value = MVEL.executeExpression(compiledCaptures.get(entry.getValue()), context);
                if (value instanceof Number) {
                    state.capture(entry.getKey(), ((Number) value).doubleValue());
                }
            } catch (Exception e) {
                log.error("Error capturing MVEL variable: {}", entry.getKey(), e);
            }
        }
    }

    private void moveToNextPhase() {
        int currentPhaseIndex = findPhaseIndex(state.getCurrentPhaseId());
        if (currentPhaseIndex < definition.getPhases().size() - 1) {
            String nextPhaseId = definition.getPhases().get(currentPhaseIndex + 1).getId();
            state.moveTo(nextPhaseId);
        } else {
            this.triggered = true;
            log.info("TRIGGER for {} on {}", definition.getPatternId(), state.getSymbol());
        }
    }

    private PatternDefinition.Phase getCurrentPhase() {
        return definition.getPhases().stream()
                .filter(p -> p.getId().equals(state.getCurrentPhaseId()))
                .findFirst()
                .orElse(null);
    }

    private int findPhaseIndex(String phaseId) {
        for (int i = 0; i < definition.getPhases().size(); i++) {
            if (definition.getPhases().get(i).getId().equals(phaseId)) {
                return i;
            }
        }
        return -1;
    }

    public PatternState getState() {
        return state;
    }

    public PatternDefinition getDefinition() {
        return definition;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void consumeTrigger() {
        this.triggered = false;
    }
}
