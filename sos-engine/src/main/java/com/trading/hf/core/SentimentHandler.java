package com.trading.hf.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.lmax.disruptor.EventHandler;
import com.trading.hf.model.MarketEvent;

public class SentimentHandler implements EventHandler<MarketEvent> {

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.getType() == MarketEvent.MessageType.SENTIMENT_UPDATE) {
            JsonNode payload = event.getPayload();
            if (payload != null && payload.has("regime")) {
                String regimeString = payload.get("regime").asText();
                GlobalRegimeController.setRegime(regimeString);
            }
        }
    }
}
