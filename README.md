# Scalping-Orchestration-System-SOS-
Scalping Orchestration System (SOS)
Architectural Document: Scalping Orchestration System (SOS)
1. Core System Philosophy
The SOS framework is designed for Modularity, Redundancy, and Adaptability.

Decoupling: The "heavy lifting" of data fetching and mathematical processing is offloaded to a Python Bridge.

Logic Isolation: The Java Engine remains a "Logic-Only" layer, executing signals based on high-level data snapshots rather than raw tick noise.

Regime-Adaptive: The system changes its sensitivity (stops, targets, and filters) based on real-time market sentiment.

This document summarizes the high-level architecture, technical requirements, and data contracts discussed for your modular trading system. This system decouples data ingestion (Python Bridge) from strategy execution (Java Engine) using a generic, regime-adaptive JSON framework.

---

# **Architectural Document: Scalping Orchestration System (SOS)**

## **1. Core System Philosophy**

The SOS framework is designed for **Modularity**, **Redundancy**, and **Adaptability**.

* **Decoupling:** The "heavy lifting" of data fetching and mathematical processing is offloaded to a Python Bridge.
* **Logic Isolation:** The Java Engine remains a "Logic-Only" layer, executing signals based on high-level data snapshots rather than raw tick noise.
* **Regime-Adaptive:** The system changes its sensitivity (stops, targets, and filters) based on real-time market sentiment.

---

## **2. Technical Requirements (TRD)**

### **A. Functional Requirements**

1. **Multi-Tier Redundancy:** The system must maintain data integrity using prioritized fallbacks (e.g., Upstox  TradingView  Yahoo Finance).
2. **Generic State Machine:** The engine must track pattern phases (Setup  Validation  Trigger) across multiple candles.
3. **Regime Awareness:** Identification of 7 distinct market states:
* *Complete Bullish, Bullish, Sideways-Bullish, Sideways, Sideways-Bearish, Bearish, Complete Bearish.*


4. **Variable Capture:** Ability to "remember" price levels (e.g., `mother_candle_high`) from past states to use as future triggers.

### **B. Non-Functional Requirements**

1. **Latency:** Bridge-to-Execution latency should be .
2. **Stateless Recovery:** The engine must be able to reconstruct its state from the Bridge's historical data upon restart.

---

## **3. Contract Document (Bridge  Engine)**

Communication is via WebSocket (JSON). All messages must include a `type` field.

### **Contract 1: `CANDLE_UPDATE**`

Broadcasts 1m/5m OHLC snapshots every 60 seconds.

* **Fields:** `symbol`, `timestamp`, `1m {o, h, l, c, v}`, `5m {o, h, l, c, v}`.

### **Contract 2: `SENTIMENT_UPDATE**`

Aggregates high-level market metrics.

* **PCR:** Total Put-Call Ratio for the index.
* **Breadth:** Advances vs. Declines counts.
* **OI Walls:** Major strike prices with high open interest concentrations.

---

## **4. Generic JSON Pattern Logic**

Patterns are no longer hardcoded in Java. They are defined in JSON files that the engine parses into a state machine.

### **Logic Flow**

1. **Setup Phase:** Identifies the "Anchor" (e.g., a high-volume mother candle).
2. **Validation Phase:** Ensures the market behaves as expected (e.g., a brief pullback or consolidation).
3. **Trigger Phase:** Specific price action (e.g., break of a level) + Sentiment Gate check.

### **JSON Structure Definition**

```json
{
  "pattern_id": "EXAMPLE_PATTERN",
  "regime_overrides": {
    "COMPLETE_BULLISH": { "tp_multiplier": 3.0, "sl_tightness": 0.05 },
    "SIDEWAYS": { "tp_multiplier": 1.0, "sl_tightness": 0.2, "quantity_scale": 0.5 }
  },
  "states": [
    { "step": 1, "conditions": ["candle.vol > avg * 2"], "capture": {"anchor": "high"} },
    { "step": 2, "conditions": ["candle.close < var.anchor"], "timeout": 5 }
  ]
}

```

---

## **5. Feature Rationale**

| Feature | Rationale |
| --- | --- |
| **Python Bridge** | Python is superior for API handling, data scraping, and mathematical libraries like Pandas/NumPy. |
| **Java Engine** | Java provides the speed, thread safety, and robust object-oriented structure needed for execution and state management. |
| **Sentiment Gates** | Prevents "Blind Technicals." A technical "Sell" is ignored if the underlying Sentiment (PCR/OI) is aggressively bullish. |
| **Regime Overrides** | Solves the "Sideways Chop." By reducing position size or tightening TP in sideways markets, the system protects capital for trending days. |

This blueprint serves as the foundation for the implementation phase.



This is a professional approach. Moving from "coding on the fly" to a **Technical Requirements Document (TRD)** and a **Contract Definition** ensures that your system remains robust even as you add complex patterns or move to different brokers.

Below is the blueprint for our **Scalping Orchestration System (SOS)**.

---

## 1. Architectural Document (SOS Framework)

### System Philosophy

The system is divided into two decoupled domains:

* **The Bridge (Python):** Handles the "Messy Reality" of market data—redundancy, API rate limits, and heavy mathematical computation (PCR velocity, Greek sensitivities).
* **The Engine (Java):** Handles "Logic & Probability"—state management, pattern matching, and order execution.

### High-Level Flow

1. **Bridge** pushes a `MultiContextUpdate` (Candles + Sentiment) every 60 seconds (or on-demand).
2. **Engine** updates the `RegimeController` to determine the current "Market Flavor."
3. **Pattern Manager** iterates through loaded JSON schemas, using the captured variables and regime-adjusted parameters to check for triggers.
4. **Execution Manager** calculates position size based on Regime-Conviction and sends the order.

---

## 2. TRD (Technical Requirements Document)

### Functional Requirements

* **Regime Awareness:** The engine must identify 7 states (Complete Bullish to Complete Bearish).
* **State Persistence:** Patterns must maintain state across multiple candles (e.g., remembering a "Mother Candle" from 10 minutes ago).
* **Dynamic Parameterization:** SL/TP and entry offsets must be variables, not constants.
* **Veto Logic:** Any pattern trigger must pass a "Sentiment Check" (e.g., No Shorting if PCR > 1.3).

### Non-Functional Requirements

* **Latency:** Signal generation from Bridge-Update to Execution should be .
* **Stateless Recovery:** If the Engine restarts, it should be able to reconstruct the last 20 candles of "State" from the Bridge's initial sync.

---

## 3. Contract Document (Bridge  Engine)

The communication will happen over a **WebSocket**. Every message must be a JSON object with a `type` field.

### A. Message Type: `MARKET_UPDATE`

Sent every time a candle closes or a major PCR shift occurs.

```json
{
  "type": "MARKET_UPDATE",
  "symbol": "NIFTY_BANK",
  "timestamp": 1704711600,
  "data": {
    "candle": {"o": 48100, "h": 48250, "l": 48050, "c": 48150, "v": 15000},
    "sentiment": {
      "pcr": 1.15,
      "pcr_velocity": 0.02,
      "breadth": {"adv": 35, "dec": 15},
      "oi_wall_above": 48500,
      "oi_wall_below": 47500
    }
  }
}

```

---

## 4. JSON Pattern Schema Definition

This is how we define a pattern like your **BRF** or a **Head & Shoulders** generically.

### Schema Blueprint

| Key | Description |
| --- | --- |
| `conditions` | Logic that must be true to enter a state. |
| `capture` | Variables to save (like `anchor_high`) for later use. |
| `regime_mod` | Multipliers for SL/TP based on market condition. |

### Example Pattern: `BRF_REVERSAL_SHORT.json`

```json
{
  "pattern_id": "BRF_SHORT",
  "regime_config": {
    "COMPLETE_BULLISH": { "allow_entry": false },
    "SIDEWAYS": { "tp_mult": 1.0, "quantity_mod": 0.5 },
    "BEARISH": { "tp_mult": 2.5, "quantity_mod": 1.0 }
  },
  "phases": [
    {
      "id": "SETUP",
      "conditions": ["candle.range > atr * 1.2"],
      "capture": { "mother_h": "candle.high", "mother_l": "candle.low" }
    },
    {
      "id": "VALIDATION",
      "conditions": ["candle.close < var.mother_l"],
      "timeout": 5
    },
    {
      "id": "TRIGGER",
      "conditions": [
        "candle.high > var.mother_l",
        "candle.body < candle.range * 0.2",
        "sentiment.pcr_velocity < 0"
      ]
    }
  ],
  "execution": {
    "entry": "var.mother_l - 0.1",
    "sl": "var.mother_h",
    "tp": "entry - (risk * tp_mult)"
  }
}

```

---

## 5. How to Use (The Workflow)

1. **Define Pattern:** Write a new JSON file for a strategy (e.g., `M_TOP.json`).
2. **Upload:** Drop the JSON into the `/strategies` folder of the Java Engine.
3. **Bootstrap:** The Engine detects the new file, parses the logic, and starts monitoring the `MARKET_UPDATE` stream from the Python Bridge.
4. **Adapt:** If the Python Bridge sends a `market_breadth` update showing a crash, the Engine automatically switches to the `COMPLETE_BEARISH` config in the JSON, tightening stops and extending targets.

### Discussion:

We have the **Bridge** code (Python) and the **Streamer** (Java). The missing core is the **`GenericPatternManager`** in Java which can interpret these JSON phases.


## ---
I apologize for the oversight. You are right; a system of this complexity requires a high-fidelity record of every logic gate, structural decision, and rationalized "why" behind the architecture.

Below is the **Master Architectural & Strategy Document**. It covers the end-to-end vision we have built together, from the multi-tier Python Bridge to the Regime-Adaptive Java Engine.

---

# **SOS Framework: Strategic & Technical Master Blueprint**

## **1. Executive Summary & Design Philosophy**

The **Scalping Orchestration System (SOS)** is a cross-platform (Python/Java) algorithmic trading framework. Its core purpose is to solve the "Rigidity Problem" in retail trading bots by introducing **Market Regime Awareness** and **Decoupled Strategy Logic**.

### **The Rationale**

* **Decoupling:** By separating the data bridge (Python) from the engine (Java), we ensure that API failures or heavy math (OI analysis) do not stall the high-speed execution engine.
* **State Machine Patterns:** Moving from `if-else` blocks to JSON state machines allows the system to remember "the footprint of big players" across time, rather than just reacting to the current tick.
* **Regime Sensitivity:** The system acknowledges that a "Bullish Pattern" in a "Bearish Market" is a low-probability trade and must be either vetoed or traded with reduced size.

---

## **2. The Data Tier: Python Bridge (`tv_data_bridge.py`)**

This tier acts as the "Single Source of Truth." It handles data redundancy and sophisticated sentiment calculations.

### **Features & Logic**

* **Tiered Redundancy:** 1.  *Primary:* Upstox API (Institutional speed).
2.  *Secondary:* TradingView Premium (Stability).
3.  *Tertiary:* Public Scrapers/Yahoo Finance (Emergency fallback).
* **Sentiment Aggregation:** * **PCR Velocity:** Calculates not just the Put-Call Ratio, but how fast it is changing ().
* **OI Barrier Analysis:** Identifies "Walls" where big players are writing Call/Put options to cap market movement.
* **Market Breadth:** Tracks the Advance-Decline ratio of the Nifty 50/BankNifty constituents.



---

## **3. The Logic Tier: Java Engine (`ScalpingSignalEngine`)**

The Engine is the "Command & Control" center. It consumes the Bridge's data and matches it against the Strategy Playbook.

### **State-Based Pattern Matching (The "Footprint" Logic)**

Patterns are broken into **Phases** to mirror institutional behavior:

1. **Phase 1: Setup (The Anchor):** Identification of a "Big Player" move (e.g., a high-volume Mother Candle with wicks).
2. **Phase 2: Validation (The Trap):** Waiting for a breakout that fails to follow through. This is the "Shakeout" phase where retail traders are trapped.
3. **Phase 3: Trigger (The Rejection):** Finding Dojis or weak rejection candles within the range of the Anchor.
4. **Phase 4: Execution:** Entering only when sellers/buyers retake control, ensuring momentum is back on our side.

### **The 7-State Regime Controller**

The engine maintains a "Global Market Mood" that modifies every strategy:

1. **Complete Bullish:** Aggressive buys, disable shorts.
2. **Bullish:** Prefer buys, tight stops for shorts.
3. **Sideways to Bullish:** Scalp buys, avoid trend-following.
4. **Sideways:** Mean reversion only, tight TP, 50% quantity.
5. **Sideways to Bearish:** Scalp shorts.
6. **Bearish:** Prefer shorts.
7. **Complete Bearish:** Aggressive shorts, disable buys.

---

## **4. The Contract: Bridge-to-Engine Interface**

The "Contract" defines exactly how the two worlds talk. This ensures that if you change your Python script, the Java engine doesn't break.

### **JSON Update Schema**

* **`type: "MARKET_UPDATE"`**: Sends the OHLC of the closed candle.
* **`type: "SENTIMENT_UPDATE"`**: Sends the current PCR, Breadth, and Regime string.
* **`type: "OPTION_CHAIN"`**: Sends Strike-wise Delta and OI Change.

---

## **5. Strategy Abstraction: The JSON Playbook**

This is the most critical innovation. We are moving all "Hardcoded" patterns into external JSON files.

### **Pattern Schema Features**

* **Variable Capture:** The ability to save a value (`var.anchor_high = candle.high`) and reference it 10 candles later.
* **Regime Overrides:** A section within the JSON that says: *"If Regime is SIDEWAYS, change TP from 2.0 to 1.0."*
* **Sentiment Gates:** A "Veto" logic that says: *"Do not trigger this Short pattern if `pcr_velocity` is positive."*

---

## **6. Rationalizing the Trade Execution (Entry/SL/TP)**

* **Entry:** Usually defined as a "Break of the Trigger Candle" (+/- a buffer like 0.1) to ensure momentum.
* **Stop Loss (SL):** Defined by the **Structural Anchor** (e.g., the high/low of the Mother Candle), not a random percentage.
* **Take Profit (TP):** Adjusted dynamically.
* *Trending:* Trail the stop to capture the big move.
* *Sideways:* Hit-and-run at 1:1 Risk/Reward.



---

## **7. Roadmap: Next Steps**

1. **Java Registry Implementation:** Create the `PatternDefinition` and `StateTracker` classes.
2. **Logic Parser:** Implement the code that evaluates strings like `"candle.close < var.mother_low"`.
3. **Regime Integration:** Link the Python Bridge's Breadth/PCR data to the Java Engine's position-sizing logic.

**Summary Conclusion:** We have designed a system that thinks like a trader (Regimes and Patterns) but executes like a machine (Decoupled and Data-Redundant).

## ===
This pseudo-code logic provides a comprehensive architectural and operational view of the entire **SOS (Scalping Orchestration System)**. It integrates the Python Bridge's multi-tier redundancy with the Java Engine's regime-adaptive state machines, incorporating the state management and recovery features previously discussed.

---

# **1. Python Bridge Logic (`tv_data_bridge.py`)**

The bridge handles data collection, sentiment calculation, and broadcasting. It serves as the system's "Sensory Array".

```python
CLASS PythonBridge:
    FUNCTION __init__():
        LOAD symbol_master
        INITIALIZE Upstox_Client, TV_Screener, NSE_API_Client
        INITIALIZE Trendlyne_Database_Connection

    ASYNC FUNCTION start_service():
        START_TASKS(
            broadcast_candles(),        # Price Data
            broadcast_sentiment(),      # PCR & Regime
            broadcast_option_chain(),   # OI Walls
            broadcast_market_breadth()  # Advances/Declines
        )

    ASYNC FUNCTION broadcast_sentiment():
        WHILE system_is_active:
            # Multi-tier PCR calculation
            pcr = FETCH_PCR_FROM_NSE() or FETCH_FROM_TRENDLYNE_DB()
            pcr_velocity = (pcr - last_pcr) / time_delta
            
            # Regime Determination
            breadth = FETCH_ADVANCE_DECLINE_RATIO()
            market_regime = CALCULATE_REGIME(pcr, pcr_velocity, breadth)
            
            SEND_TO_JAVA_ENGINE({
                "type": "SENTIMENT_UPDATE",
                "pcr": pcr,
                "velocity": pcr_velocity,
                "regime": market_regime
            })
            SLEEP(60)

    ASYNC FUNCTION broadcast_candles():
        WHILE market_is_open:
            # Tiered Redundancy for data integrity
            candle_data = FETCH_UPSTOX() or FETCH_TV_PREMIUM() or FETCH_YAHOO()
            
            SEND_TO_JAVA_ENGINE({
                "type": "CANDLE_UPDATE",
                "data": candle_data
            })
            SLEEP(60)

```

---

# **2. Java Engine Logic (`ScalpingSignalEngine.java`)**

The engine acts as the "Decision Brain," managing the state of multiple patterns and symbols.

```java
CLASS ScalpingSignalEngine:
    // Memory and Persistence
    Map<String, List<VolumeBar>> historyMap;
    Map<String, Map<String, PatternState>> symbolPatternStates;
    Regime currentRegime;

    FUNCTION onMessageReceived(JSON message):
        SWITCH(message.type):
            CASE "SENTIMENT_UPDATE":
                UPDATE_GLOBAL_REGIME(message.regime);
            CASE "CANDLE_UPDATE":
                PROCESS_ALL_PATTERNS(message.data);
            CASE "SYNC_RECOVERY":
                RECONSTRUCT_STATE(message.history_data);

    FUNCTION processAllPatterns(Candle candle):
        symbol = candle.getSymbol();
        historyMap.get(symbol).add(candle);
        
        // Iterate through all JSON-defined patterns for this symbol
        FOR EACH pattern IN strategyPlaybook:
            stateTracker = symbolPatternStates.get(symbol, pattern.id);
            
            // 1. Regime Veto
            IF (pattern.isForbiddenIn(currentRegime)):
                stateTracker.reset();
                CONTINUE;

            // 2. State Machine Progress
            CURRENT_PHASE = stateTracker.getPhase();
            
            SWITCH(CURRENT_PHASE):
                PHASE SETUP:
                    IF (EVALUATE(pattern.setup_cond, candle)):
                        stateTracker.captureVars(pattern.setup_vars);
                        stateTracker.moveTo(VALIDATION);
                
                PHASE VALIDATION:
                    IF (EVALUATE(pattern.validation_cond, candle)):
                        stateTracker.moveTo(TRIGGER);
                    ELSE IF (TIMEOUT_REACHED):
                        stateTracker.reset();

                PHASE TRIGGER:
                    IF (EVALUATE(pattern.trigger_cond, candle)):
                        EXECUTE_TRADE(symbol, pattern, stateTracker.getCapturedVars());
                        stateTracker.reset();

```

---

# **3. State Management & Recovery Logic**

Ensures the system survives crashes and maintains temporal integrity.

```java
CLASS RecoveryManager:
    FUNCTION onStartup():
        // Solve the "Amnesia Problem"
        SEND_TO_BRIDGE("REQUEST_HISTORY_SYNC");
        
    FUNCTION handleHistorySync(JSON historyPacket):
        FOR EACH candle IN historyPacket:
            // Feed historical candles through the engine to "Replay" patterns
            engine.onVolumeBar(candle, isHistorical = true);
        
    FUNCTION checkTemporalIntegrity(Message msg):
        // Solve the "Ghost Signal" problem
        IF (CURRENT_TIME - msg.timestamp > 5_SECONDS):
            LOG_WARNING("Stale data detected. Ignoring message.");
            RETURN false;
        RETURN true;

```

---

# **4. Order Execution Logic**

Handles the "Contract" between strategy intent and broker API.

```java
CLASS OrderOrchestrator:
    FUNCTION executeTrade(Symbol symbol, Pattern pattern, Variables capturedVars):
        // 1. Position Sizing based on Regime
        riskMultiplier = currentRegime.getRiskMod(); # e.g., 0.5 for Sideways
        finalQuantity = calculateBaseQty(capturedVars.stopLoss) * riskMultiplier;
        
        // 2. Map Generic Symbol to Broker Instrument (Upstox)
        brokerKey = instrumentMapper.findATM(symbol, pattern.side);
        
        // 3. Dispatch Atomic Order
        orderResult = brokerClient.placeOrder(
            key = brokerKey,
            qty = finalQuantity,
            type = "LIMIT",
            price = capturedVars.entryPrice
        );
        
        IF (orderResult.success):
            monitorExits(orderResult.orderID, pattern, capturedVars);

```

---

### **System Data Flow Summary**

1. **Ingestion:** Python Bridge fetches redundant data.
2. **Broadcasting:** Bridge sends `MARKET_UPDATE` and `SENTIMENT_UPDATE` to Java.
3. **State Logic:** Java Engine updates internal `PatternState` trackers and captures variables (like `mother_high`).
4. **Execution:** When `TRIGGER` phase is met, the Engine adjusts quantity for the current `Regime` and executes via Upstox.

## ======

In the previous pseudo-code, the **LMAX Disruptor architecture** was not explicitly implemented as a class; rather, its principles were implied in the "Logic Interaction" section regarding how the system handles high-concurrency state transitions without latency.

To integrate it properly into the **Java Orchestrator**, we need to move away from traditional `BlockingQueues` or heavy `synchronized` blocks. The Disruptor pattern uses a **Ring Buffer** to ensure that data from the **Python Bridge** is processed by the **Pattern Engine** and **Execution Manager** in a lock-free, sequential manner.

Here is the updated pseudo-code logic explicitly incorporating the **Disruptor Architecture**:

### **1. The Disruptor Ring Buffer (The Central Nervous System)**

Instead of the `TVMarketDataStreamer` calling the engine directly, it "publishes" events to a pre-allocated Ring Buffer.

```java
// Logic for Central Dispatcher using Disruptor
CLASS MarketEventDisruptor:
    RingBuffer<MarketEvent> ringBuffer;

    FUNCTION onBridgeMessage(JSON msg):
        // Lock-free publishing to the ring
        long sequence = ringBuffer.next();
        MarketEvent event = ringBuffer.get(sequence);
        
        // Populate pre-allocated event object (Zero Garbage)
        event.setType(msg.type);
        event.setPayload(msg.data);
        event.setTimestamp(msg.ts);
        
        ringBuffer.publish(sequence);

```

### **2. Parallel Consumer Chains (The "Gating" Logic)**

In LMAX architecture, multiple consumers (Handlers) process the same event in a specific order without contention.

```java
// Handler 1: Update Global Market Context
CLASS SentimentHandler implements EventHandler<MarketEvent>:
    FUNCTION onEvent(event):
        IF (event.type == "SENTIMENT_UPDATE"):
            GlobalRegime.set(event.regime);

// Handler 2: Pattern State Machine (Depends on Handler 1)
CLASS PatternMatcherHandler implements EventHandler<MarketEvent>:
    FUNCTION onEvent(event):
        IF (event.type == "CANDLE_UPDATE"):
            FOR EACH symbol_pattern IN activeTrackers:
                symbol_pattern.evaluate(event.candle, GlobalRegime.get());

// Handler 3: Execution & Risk (Depends on Handler 2)
CLASS ExecutionHandler implements EventHandler<MarketEvent>:
    FUNCTION onEvent(event):
        IF (PatternMatcherHandler.hasTriggered(event.symbol)):
            Orchestrator.execute(event.signal);

```

### **3. Why this is added to the SOS Project**

* **Mechanical Sympathy**: By using the Ring Buffer, the Java Engine avoids the "Race Conditions" identified during reanalysis. Each symbol's state transition happens in a deterministic sequence.
* **Zero-Lock Concurrency**: The `PatternMatcherHandler` can process NIFTY and BANKNIFTY candles simultaneously from the same buffer without needing `synchronized` blocks that slow down scalping execution.
* **State Recovery**: The Ring Buffer acts as a short-term memory. If the `ExecutionHandler` lags, the events stay in the buffer until processed, preventing the "Ghost Signal" problem where the system skips a trigger due to a thread being blocked.

### **4. Updated Operational Flow**

1. **Python Bridge** pushes data  **TVMarketDataStreamer** receives it.
2. **Streamer** translates JSON to a `MarketEvent` and pushes to the **Disruptor Ring Buffer**.
3. **Consumer 1** updates the **7-State Regime**.
4. **Consumer 2** (Pattern Engine) uses the updated Regime to check the **JSON Phases** (Setup  Validation  Trigger).
5. **Consumer 3** (Order Manager) fires the trade to the broker only after the previous two handlers have finished their logic for that specific millisecond.


 ### -------------------

 To implement the architecture we have discussed, including the **LMAX Disruptor** for lock-free concurrency, the **Python Bridge** for data redundancy, and the **JSON-based Pattern Engine**, the project requires a highly decoupled structure.

Below is the project organization and the logical data flow that connects these components.

---

## **1. Project Directory Structure**

The project is split into the **Data Tier** (Python) and the **Logic Tier** (Java).

### **A. Python Data Bridge (`/sos-bridge`)**

* **`tv_data_bridge.py`**: The main WebSocket server handling redundancy and broadcasting.
* **`NSEAPICLient.py`**: Handles direct communication with the NSE for PCR and Breadth data.
* **`SymbolMaster.py`**: Maps generic symbols (e.g., NIFTY) to broker-specific keys (e.g., Upstox).
* **`backfill_trendlyne.py`**: Manages the SQLite database for historical option chain recovery.
* **`config.py`**: Stores API tokens (Upstox) and environment variables.

### **B. Java Trading Engine (`/sos-engine`)**

* **`com.trading.hf.core`**:
* **`DisruptorOrchestrator.java`**: Sets up the Ring Buffer and consumer chains.
* **`GlobalRegimeController.java`**: Maintains the 7-state market sentiment.


* **`com.trading.hf.streamer`**:
* **`TVMarketDataStreamer.java`**: The WebSocket client that feeds the Ring Buffer.


* **`com.trading.hf.patterns`**:
* **`GenericPatternParser.java`**: Loads JSON strategy files.
* **`PatternStateMachine.java`**: Manages "Step" transitions and variable capture.


* **`com.trading.hf.model`**:
* **`VolumeBar.java`**: Data object for candles and indicators.
* **`MarketEvent.java`**: The pre-allocated event object used in the Ring Buffer.


* **`resources/strategies/`**: Folder containing `.json` strategy files (e.g., `BRF_Short.json`).

---

## **2. Logical Data Flow (LMAX Disruptor Model)**

The system operates in a linear, non-blocking pipeline to ensure that sentiment is always updated before a pattern is evaluated.

### **Step 1: Ingestion & Normalization**

1. **Python Bridge** fetches data from Upstox, NSE, or TradingView fallbacks.
2. Data is packaged as a JSON `MARKET_UPDATE` or `SENTIMENT_UPDATE` and sent via WebSocket.
3. **`TVMarketDataStreamer`** receives the JSON and translates it into a pre-allocated **`MarketEvent`** in the Disruptor Ring Buffer.

### **Step 2: Sequential Processing (The Pipeline)**

The Disruptor ensures these handlers run in order for every single event:

* **Handler A (Regime Update):** Reads the sentiment data (PCR, Breadth) and updates the **`GlobalRegimeController`**. This sets the "sensitivity" for all other logic.
* **Handler B (Pattern Matching):** * Iterates through all active JSON patterns.
* Checks if the current candle triggers a **Setup**, **Validation**, or **Trigger** phase.
* If a Setup is hit, it captures variables like `mother_high` in the **`PatternStateMachine`**.


* **Handler C (Execution Engine):** * If Handler B flags a "Trigger," this handler checks the current Regime conviction.
* It adjusts order quantity (e.g., 50% for Sideways, 120% for Complete Trending).
* It dispatches the order to the broker.



### **Step 3: State Persistence & Recovery**

* Every 5 minutes, a **Persistence Handler** takes a snapshot of the `PatternStateMachine` variables and saves them to a local disk.
* Upon restart, the engine requests a "Backfill" from the Python Bridge to re-simulate the morning's price action and restore the state of active patterns.

---

## **3. Logical Flow of a Trade (Example: BRF Short)**

| Component | Action |
| --- | --- |
| **Python Bridge** | Detects PCR falling and Breadth turning negative; broadcasts `SENTIMENT_UPDATE`. |
| **Java Regime Handler** | Sets global state to **"Bearish"**. |
| **Java Pattern Handler** | JSON Step 1 (Mother Candle) was captured 10 mins ago. Current candle triggers Step 3 (Trigger). |
| **Java Execution Handler** | Sees "Bearish" regime permits shorting; calculates entry at `mother_low - 0.1` and fires order. |

 

