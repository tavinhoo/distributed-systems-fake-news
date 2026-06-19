package core;

import model.CellState;
import model.SimulationResult;

public class Statistics {
    private Statistics() {
    }

    public static SimulationResult buildResult(CellState[][] grid,
                                               int generations,
                                               long elapsedNanos,
                                               int neutralizedByGrok) {
        int ignorant = 0;
        int spreader = 0;
        int inactive = 0;
        int grok = 0;

        for (CellState[] row : grid) {
            for (CellState state : row) {
                if (state == CellState.IGNORANT) {
                    ignorant++;
                } else if (state == CellState.SPREADER) {
                    spreader++;
                } else if (state == CellState.INACTIVE) {
                    inactive++;
                } else {
                    grok++;
                }
            }
        }

        return new SimulationResult(grid, generations, elapsedNanos,
                ignorant, spreader, inactive, grok, neutralizedByGrok);
    }
}
