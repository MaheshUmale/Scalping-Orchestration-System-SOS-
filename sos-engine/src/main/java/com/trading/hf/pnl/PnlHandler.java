package com.trading.hf.pnl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import com.trading.hf.model.MarketEvent;
import com.trading.hf.model.VolumeBar;

public class PnlHandler implements EventHandler<MarketEvent> {

    private final PortfolioManager portfolioManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PnlHandler(PortfolioManager portfolioManager) {
        this.portfolioManager = portfolioManager;
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.getType() == MarketEvent.MessageType.MARKET_UPDATE) {
            JsonNode payload = event.getPayload();
            if (payload != null && payload.has("candle")) {
                VolumeBar candle = objectMapper.treeToValue(payload.get("candle"), VolumeBar.class);
                candle.setSymbol(payload.get("symbol").asText());
                portfolioManager.onCandle(candle);
            }
        }
    }
}
