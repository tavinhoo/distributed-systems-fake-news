package sequential;

import core.SimulationRules;
import core.Statistics;
import model.CellState;
import model.SimulationConfig;
import model.SimulationResult;
import util.Timer;

public class SequentialSimulation {
    public SimulationResult run(CellState[][] initialGrid, SimulationConfig config) {
        Timer timer = new Timer();
        timer.start();

        CellState[][] currentGrid = initialGrid;
        CellState[][] nextGrid = new CellState[config.getRows()][config.getColumns()];
        int neutralizedByGrok = 0;

        for (int generation = 0; generation < config.getGenerations(); generation++) {
            for (int row = 0; row < config.getRows(); row++) {
                for (int col = 0; col < config.getColumns(); col++) {
                    if (SimulationRules.wasNeutralizedByGrok(currentGrid, row, col, generation, config)) {
                        neutralizedByGrok++;
                    }
                    nextGrid[row][col] = SimulationRules.nextState(currentGrid, row, col, generation, config);
                }
            }

            CellState[][] temp = currentGrid;
            currentGrid = nextGrid;
            nextGrid = temp;
        }

        return Statistics.buildResult(currentGrid, config.getGenerations(), timer.elapsedNanos(), neutralizedByGrok);
    }
}
