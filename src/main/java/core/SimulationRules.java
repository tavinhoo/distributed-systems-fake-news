package core;

import model.CellState;
import model.SimulationConfig;

import java.util.Random;

public class SimulationRules {
    private SimulationRules() {
    }

    public static CellState nextState(CellState[][] grid,
                                      int row,
                                      int col,
                                      int generation,
                                      SimulationConfig config) {
        return nextState(grid, row, col, generation, config, row);
    }

    public static CellState nextState(CellState[][] grid,
                                      int row,
                                      int col,
                                      int generation,
                                      SimulationConfig config,
                                      int randomRow) {
        CellState current = grid[row][col];

        if (current == CellState.INACTIVE || current == CellState.GROK) {
            return current;
        }

        if (current == CellState.SPREADER) {
            double chance = deterministicRandom(config.getSeed(), generation, randomRow, col, 1);
            if (chance < config.getInactiveProbability()) {
                return CellState.INACTIVE;
            }

            if (hasGrokNeighbor(grid, row, col) && isNeutralizedByGrok(generation, config, randomRow, col)) {
                return CellState.INACTIVE;
            }

            return CellState.SPREADER;
        }

        if (hasSpreaderNeighbor(grid, row, col)) {
            double chance = deterministicRandom(config.getSeed(), generation, randomRow, col, 2);
            double spreadProbability = config.getSpreadProbability();
            if (hasGrokNeighbor(grid, row, col)) {
                spreadProbability *= config.getGrokInfluenceReductionFactor();
            }

            return chance < spreadProbability ? CellState.SPREADER : CellState.IGNORANT;
        }

        return CellState.IGNORANT;
    }

    public static boolean wasNeutralizedByGrok(CellState[][] grid,
                                               int row,
                                               int col,
                                               int generation,
                                               SimulationConfig config) {
        return wasNeutralizedByGrok(grid, row, col, generation, config, row);
    }

    public static boolean wasNeutralizedByGrok(CellState[][] grid,
                                               int row,
                                               int col,
                                               int generation,
                                               SimulationConfig config,
                                               int randomRow) {
        if (grid[row][col] != CellState.SPREADER || !hasGrokNeighbor(grid, row, col)) {
            return false;
        }

        double naturalInactiveChance = deterministicRandom(config.getSeed(), generation, randomRow, col, 1);
        if (naturalInactiveChance < config.getInactiveProbability()) {
            return false;
        }

        return isNeutralizedByGrok(generation, config, randomRow, col);
    }

    private static boolean hasSpreaderNeighbor(CellState[][] grid, int row, int col) {
        return hasNeighborWithState(grid, row, col, CellState.SPREADER);
    }

    private static boolean hasGrokNeighbor(CellState[][] grid, int row, int col) {
        return hasNeighborWithState(grid, row, col, CellState.GROK);
    }

    private static boolean hasNeighborWithState(CellState[][] grid, int row, int col, CellState expectedState) {
        for (int deltaRow = -1; deltaRow <= 1; deltaRow++) {
            for (int deltaCol = -1; deltaCol <= 1; deltaCol++) {
                if (deltaRow == 0 && deltaCol == 0) {
                    continue;
                }

                int neighborRow = row + deltaRow;
                int neighborCol = col + deltaCol;
                if (isInside(grid, neighborRow, neighborCol)
                        && grid[neighborRow][neighborCol] == expectedState) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isInside(CellState[][] grid, int row, int col) {
        return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length;
    }

    private static boolean isNeutralizedByGrok(int generation,
                                               SimulationConfig config,
                                               int row,
                                               int col) {
        double chance = deterministicRandom(config.getSeed(), generation, row, col, 3);
        return chance < config.getGrokCorrectionProbability();
    }

    private static double deterministicRandom(long seed, int generation, int row, int col, int ruleId) {
        long mixed = seed;
        mixed = mixed * 31 + generation;
        mixed = mixed * 31 + row;
        mixed = mixed * 31 + col;
        mixed = mixed * 31 + ruleId;
        return new Random(mixed).nextDouble();
    }
}
