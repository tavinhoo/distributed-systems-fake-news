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
        int bot = 0;
        int influencer = 0;
        int echoChamber = 0;
        int factChecker = 0;
        int journalist = 0;

        for (CellState[] row : grid) {
            for (CellState state : row) {
                if (state == CellState.IGNORANT) {
                    ignorant++;
                } else if (state == CellState.SPREADER) {
                    spreader++;
                } else if (state == CellState.INACTIVE) {
                    inactive++;
                } else if (state == CellState.GROK) {
                    grok++;
                } else if (state == CellState.BOT) {
                    bot++;
                } else if (state == CellState.INFLUENCER) {
                    influencer++;
                } else if (state == CellState.ECHO_CHAMBER) {
                    echoChamber++;
                } else if (state == CellState.FACT_CHECKER) {
                    factChecker++;
                } else if (state == CellState.JOURNALIST) {
                    journalist++;
                }
            }
        }

        return new SimulationResult(grid, generations, elapsedNanos,
                ignorant, spreader, inactive, grok, bot, influencer,
                echoChamber, factChecker, journalist, neutralizedByGrok);
    }
}
