package distributed;

import model.CellState;
import model.SimulationConfig;

import java.io.Serializable;

public class WorkerTask implements Serializable {
    private final SimulationConfig config;
    private final CellState[][] blockWithGhostRows;
    private final int generation;
    private final int globalStartRow;
    private final int firstInnerRow;
    private final int innerRowCount;

    private WorkerTask(SimulationConfig config,
                       CellState[][] blockWithGhostRows,
                       int generation,
                       int globalStartRow,
                       int firstInnerRow,
                       int innerRowCount) {
        this.config = config;
        this.blockWithGhostRows = blockWithGhostRows;
        this.generation = generation;
        this.globalStartRow = globalStartRow;
        this.firstInnerRow = firstInnerRow;
        this.innerRowCount = innerRowCount;
    }

    public static WorkerTask create(SimulationConfig config,
                                    CellState[][] blockWithGhostRows,
                                    int generation,
                                    int globalStartRow,
                                    int firstInnerRow,
                                    int innerRowCount) {
        return new WorkerTask(config, blockWithGhostRows, generation,
                globalStartRow, firstInnerRow, innerRowCount);
    }

    public SimulationConfig getConfig() {
        return config;
    }

    public CellState[][] getBlockWithGhostRows() {
        return blockWithGhostRows;
    }

    public int getGeneration() {
        return generation;
    }

    public int getGlobalStartRow() {
        return globalStartRow;
    }

    public int getFirstInnerRow() {
        return firstInnerRow;
    }

    public int getInnerRowCount() {
        return innerRowCount;
    }
}
