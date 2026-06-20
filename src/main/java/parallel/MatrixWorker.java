package parallel;

import core.SimulationRules;
import model.CellState;
import model.SimulationConfig;

public class MatrixWorker implements Runnable {
    private final CellState[][] currentGrid;
    private final CellState[][] nextGrid;
    private final SimulationConfig config;
    private final int generation;
    private final int startRowInclusive;
    private final int endRowExclusive;
    private int neutralizedByGrok;

    public MatrixWorker(CellState[][] currentGrid,
                        CellState[][] nextGrid,
                        SimulationConfig config,
                        int generation,
                        int startRowInclusive,
                        int endRowExclusive) {
        this.currentGrid = currentGrid;
        this.nextGrid = nextGrid;
        this.config = config;
        this.generation = generation;
        this.startRowInclusive = startRowInclusive;
        this.endRowExclusive = endRowExclusive;
    }

    @Override
    public void run() {
        for (int row = startRowInclusive; row < endRowExclusive; row++) {
            for (int col = 0; col < config.getColumns(); col++) {
                nextGrid[row][col] = SimulationRules.nextState(currentGrid, row, col, generation, config);
                if (SimulationRules.wasNeutralizedByGrok(currentGrid, row, col, generation, config)) {
                    neutralizedByGrok++;
                }
            }
        }
    }

    public int getNeutralizedByGrok() {
        return neutralizedByGrok;
    }
}
