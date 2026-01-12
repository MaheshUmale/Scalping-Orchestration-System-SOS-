package com.trading.hf.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.lmax.disruptor.EventHandler;
import com.trading.hf.model.MarketEvent;

public class SentimentHandler implements EventHandler<MarketEvent> {

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.getType() == MarketEvent.MessageType.MARKET_UPDATE) {
            JsonNode payload = event.getPayload();
            if (payload != null && payload.has("sentiment")) {
                JsonNode sentimentNode = payload.get("sentiment");
                // This is a placeholder for the actual regime calculation logic.
                // In a real system, this would involve a more complex calculation
                // based on the various sentiment fields.
                String regime = determineRegime(sentimentNode);
                GlobalRegimeController.setRegime(regime);
            }
        }
    }

    private static final double PCR_EXTREME_BULLISH = 0.7;
    private static final double PCR_EXTREME_BEARISH = 1.3;
    private static final double PCR_NEUTRAL = 1.0;

    private String determineRegime(JsonNode sentimentNode) {
        // Simple logic for demonstration purposes. A real implementation would be more complex.
        double pcr = sentimentNode.get("pcr").asDouble();
        if (pcr < PCR_EXTREME_BULLISH) {
            return "COMPLETE_BULLISH";
        } else if (pcr < PCR_NEUTRAL) {
            return "BULLISH";
        } else if (pcr > PCR_EXTREME_BEARISH) {
            return "COMPLETE_BEARISH";
        } else if (pcr > PCR_NEUTRAL) {
            return "BEARISH";
        } else {
            return "SIDEWAYS";
        }
    }
}
