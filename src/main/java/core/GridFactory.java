package core;

import model.CellState;
import model.SimulationConfig;

import java.util.Random;

public class GridFactory {
    private GridFactory() {
    }

    public static CellState[][] createInitialGrid(SimulationConfig config) {
        Random random = new Random(config.getSeed());
        CellState[][] grid = new CellState[config.getRows()][config.getColumns()];

        for (int row = 0; row < config.getRows(); row++) {
            for (int col = 0; col < config.getColumns(); col++) {
                double chance = random.nextDouble();
                if (chance < config.getInitialGrokPercentage()) {
                    grid[row][col] = CellState.GROK;
                } else if (chance < config.getInitialGrokPercentage()
                        + config.getInitialWhatsAppGroupPercentage()) {
                    grid[row][col] = CellState.WHATSAPP_GROUP;
                } else if (chance < config.getInitialGrokPercentage()
                        + config.getInitialWhatsAppGroupPercentage()
                        + config.getInitialInfluencerPercentage()) {
                    grid[row][col] = CellState.INFLUENCER;
                } else if (chance < config.getInitialGrokPercentage()
                        + config.getInitialWhatsAppGroupPercentage()
                        + config.getInitialInfluencerPercentage()
                        + config.getInitialJournalistPercentage()) {
                    grid[row][col] = CellState.JOURNALIST;
                } else if (chance < config.getInitialGrokPercentage()
                        + config.getInitialWhatsAppGroupPercentage()
                        + config.getInitialInfluencerPercentage()
                        + config.getInitialJournalistPercentage()
                        + config.getInitialSpreaderRate()) {
                    grid[row][col] = CellState.SPREADER;
                } else {
                    grid[row][col] = CellState.IGNORANT;
                }
            }
        }

        return grid;
    }

    public static CellState[][] copyGrid(CellState[][] original) {
        CellState[][] copy = new CellState[original.length][original[0].length];
        for (int row = 0; row < original.length; row++) {
            System.arraycopy(original[row], 0, copy[row], 0, original[row].length);
        }
        return copy;
    }

    public static CellState[][] copyReplacingGrokWithIgnorant(CellState[][] original) {
        CellState[][] copy = new CellState[original.length][original[0].length];
        for (int row = 0; row < original.length; row++) {
            for (int col = 0; col < original[row].length; col++) {
                copy[row][col] = original[row][col] == CellState.GROK
                        ? CellState.IGNORANT
                        : original[row][col];
            }
        }
        return copy;
    }
}
