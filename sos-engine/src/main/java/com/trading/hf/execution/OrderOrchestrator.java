package com.trading.hf.execution;

import com.trading.hf.core.GlobalRegimeController;
import com.trading.hf.model.PatternDefinition;
import com.trading.hf.model.PatternState;
import com.trading.hf.pnl.PortfolioManager;
import com.trading.hf.pnl.Trade;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class OrderOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(OrderOrchestrator.class);
    private final PortfolioManager portfolioManager;

    public OrderOrchestrator(PortfolioManager portfolioManager) {
        this.portfolioManager = portfolioManager;
    }

    public void executeTrade(PatternState triggeredState, PatternDefinition definition) {
        // 1. Get the current market regime and config
        GlobalRegimeController.MarketRegime regime = GlobalRegimeController.getRegime();
        PatternDefinition.RegimeConfig regimeConfig = definition.getRegimeConfig().get(regime.name());

        // 2. Check if the pattern is allowed in the current regime
        if (regimeConfig != null && !regimeConfig.isAllowEntry()) {
            log.info("Execution VETOED: Pattern {} is not allowed in {} regime.", definition.getPatternId(), regime);
            return;
        }

        // 3. Create the execution context for MVEL
        Map<String, Object> context = new HashMap<>();
        context.put("var", triggeredState.getCapturedVariables());
        if (regimeConfig != null) {
            context.put("tp_mult", regimeConfig.getTpMult());
            context.put("quantity_mod", regimeConfig.getQuantityMod());
        } else {
            context.put("tp_mult", 1.0);
            context.put("quantity_mod", 1.0);
        }

        try {
            // 4. Evaluate entry and stop-loss first to calculate risk
            double entryPrice = MVEL.eval(definition.getExecution().getEntry(), context, Double.class);
            double stopLoss = MVEL.eval(definition.getExecution().getSl(), context, Double.class);
            Trade.TradeSide side = Trade.TradeSide.valueOf(definition.getExecution().getSide());

            double risk;
            if (side == Trade.TradeSide.LONG) {
                risk = entryPrice - stopLoss;
            } else {
                risk = stopLoss - entryPrice;
            }
            context.put("entry", entryPrice);
            context.put("risk", risk);

            // 5. Now, evaluate take-profit
            double takeProfit = MVEL.eval(definition.getExecution().getTp(), context, Double.class);

            // 6. Calculate final quantity
            double baseQuantity = 100; // This would typically be calculated based on risk
            double finalQuantity = baseQuantity * (double) context.get("quantity_mod");

            log.info("--- EXECUTION TRIGGERED ---");
            log.info("Pattern: {}", definition.getPatternId());
            log.info("Symbol: {}", triggeredState.getSymbol());
            log.info("Regime: {}", regime);
            log.info("Side: {}", side);
            log.info("Quantity: {}", finalQuantity);
            log.info("Calculated Entry: {}", entryPrice);
            log.info("Calculated SL: {}", stopLoss);
            log.info("Calculated TP: {}", takeProfit);
            log.info("--------------------------");

            portfolioManager.newTrade(triggeredState.getSymbol(), entryPrice, stopLoss, takeProfit, finalQuantity, side);

        } catch (Exception e) {
            log.error("Error evaluating execution logic for pattern {}", definition.getPatternId(), e);
        }
    }
}
