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
 

