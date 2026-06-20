package distributed;

import model.CellState;

import java.io.Serializable;

public class WorkerResult implements Serializable {
    private final int startRow;
    private final CellState[][] computedRows;
    private final int neutralizedByGrok;

    public WorkerResult(int startRow, CellState[][] computedRows, int neutralizedByGrok) {
        this.startRow = startRow;
        this.computedRows = computedRows;
        this.neutralizedByGrok = neutralizedByGrok;
    }

    public int getStartRow() {
        return startRow;
    }

    public CellState[][] getComputedRows() {
        return computedRows;
    }

    public int getNeutralizedByGrok() {
        return neutralizedByGrok;
    }
}
