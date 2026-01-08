package com.trading.hf.pnl;

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
        if (event.getType() == MarketEvent.MessageType.CANDLE_UPDATE) {
            VolumeBar candle = objectMapper.treeToValue(event.getPayload().get("candle"), VolumeBar.class);
            candle.setSymbol(event.getPayload().get("symbol").asText());
            portfolioManager.onCandle(candle);
        }
    }
}
