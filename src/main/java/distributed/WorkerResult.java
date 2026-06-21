package distributed;

import model.CellState;

import java.io.Serializable;

public class WorkerResult implements Serializable {
    private final int startRow;
    private final CellState[][] computedRows;
    private final int neutralizedByGrok;
    private final long processingNanos;

    public WorkerResult(int startRow, CellState[][] computedRows, int neutralizedByGrok) {
        this(startRow, computedRows, neutralizedByGrok, 0);
    }

    public WorkerResult(int startRow, CellState[][] computedRows, int neutralizedByGrok, long processingNanos) {
        this.startRow = startRow;
        this.computedRows = computedRows;
        this.neutralizedByGrok = neutralizedByGrok;
        this.processingNanos = processingNanos;
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

    public long getProcessingNanos() {
        return processingNanos;
    }
}
