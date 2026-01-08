package com.trading.hf.pnl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.hf.model.VolumeBar;
import com.trading.hf.ui.UISWebSocketServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PortfolioManager {

    private final List<Trade> trades = new CopyOnWriteArrayList<>();
    private final UISWebSocketServer uiWebSocketServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PortfolioManager(UISWebSocketServer uiWebSocketServer) {
        this.uiWebSocketServer = uiWebSocketServer;
    }

    public void newTrade(String symbol, double entry, double sl, double tp, double quantity, Trade.TradeSide side) {
        trades.add(new Trade(symbol, entry, sl, tp, quantity, side));
    }

    public void onCandle(VolumeBar candle) {
        List<Trade> closedTrades = new ArrayList<>();
        double totalPnl = 0.0;
        for (Trade trade : trades) {
            if (trade.isOpen()) {
                if (candle.getSymbol().equals(trade.getSymbol())) {
                    boolean close = false;
                    if (trade.getSide() == Trade.TradeSide.LONG) {
                        if (candle.getClose() <= trade.getStopLoss() || candle.getClose() >= trade.getTakeProfit()) {
                            close = true;
                        }
                    } else { // SHORT
                        if (candle.getClose() >= trade.getStopLoss() || candle.getClose() <= trade.getTakeProfit()) {
                            close = true;
                        }
                    }
                    if (close) {
                        trade.close(candle.getClose());
                        closedTrades.add(trade);
                    }
                }
            }
            if (!trade.isOpen()) {
                totalPnl += trade.getPnl();
            }
        }
        trades.removeAll(closedTrades);

        try {
            String pnlUpdate = objectMapper.writeValueAsString(new PnlUpdate(totalPnl, trades.size()));
            uiWebSocketServer.broadcast(pnlUpdate);
        } catch (Exception e) {
            // log error
        }
    }

    private static class PnlUpdate {
        public final String type = "PNL_UPDATE";
        public final double totalPnl;
        public final int openTrades;

        public PnlUpdate(double totalPnl, int openTrades) {
            this.totalPnl = totalPnl;
            this.openTrades = openTrades;
        }
    }
}
