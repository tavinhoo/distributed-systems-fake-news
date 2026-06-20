package model;

import java.io.Serializable;

public enum CellState implements Serializable {
    IGNORANT,
    SPREADER,
    INACTIVE,
    GROK,
    BOT,
    INFLUENCER,
    ECHO_CHAMBER,
    FACT_CHECKER,
    JOURNALIST
}
