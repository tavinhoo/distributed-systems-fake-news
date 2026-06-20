package core;

import model.CellState;
import model.SimulationConfig;

import java.util.Random;

public class SimulationRules {
    public static final int MAX_INFLUENCE_RADIUS = 3;
    private static final double WHATSAPP_SPREAD_BONUS = 0.05;
    private static final double INFLUENCER_SPREAD_BONUS = 0.12;
    private static final double MAX_SPREAD_PROBABILITY = 0.27;
    private static final double WHATSAPP_DECAY_PROBABILITY = 0.010;
    private static final double INFLUENCER_DECAY_PROBABILITY = 0.006;
    private static final double WHATSAPP_CREATION_PROBABILITY = 0.012;
    private static final double INFLUENCER_CREATION_PROBABILITY = 0.004;

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

        if (current == CellState.GROK) {
            return current;
        }

        if (current == CellState.WHATSAPP_GROUP) {
            double chance = deterministicRandom(config.getSeed(), generation, randomRow, col, 4);
            return chance < WHATSAPP_DECAY_PROBABILITY ? CellState.INACTIVE : CellState.WHATSAPP_GROUP;
        }

        if (current == CellState.INFLUENCER) {
            double chance = deterministicRandom(config.getSeed(), generation, randomRow, col, 5);
            return chance < INFLUENCER_DECAY_PROBABILITY ? CellState.INACTIVE : CellState.INFLUENCER;
        }

        if (current == CellState.INACTIVE) {
            return CellState.INACTIVE;
        }

        if (current == CellState.SPREADER) {
            double chance = deterministicRandom(config.getSeed(), generation, randomRow, col, 1);
            return chance < config.getInactiveProbability() ? CellState.INACTIVE : CellState.SPREADER;
        }

        double spreadProbability = spreadProbability(grid, row, col, config);
        if (spreadProbability > 0) {
            CellState amplificationAgent = maybeCreateAmplificationAgent(grid, row, col, generation, config, randomRow);
            if (amplificationAgent != CellState.IGNORANT) {
                return amplificationAgent;
            }

            double chance = deterministicRandom(config.getSeed(), generation, randomRow, col, 2);
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
        return false;
    }

    private static boolean hasSpreaderNeighbor(CellState[][] grid, int row, int col) {
        return hasNeighborWithState(grid, row, col, CellState.SPREADER, 1);
    }

    private static double spreadProbability(CellState[][] grid,
                                            int row,
                                            int col,
                                            SimulationConfig config) {
        boolean directSpreader = hasSpreaderNeighbor(grid, row, col);
        boolean whatsAppBoost = isInfluencedByWhatsAppGroup(grid, row, col);
        boolean influencerBoost = isInfluencedByInfluencer(grid, row, col);

        if (!directSpreader && !whatsAppBoost && !influencerBoost) {
            return 0.0;
        }

        double probability = config.getSpreadProbability();
        if (whatsAppBoost) {
            probability += WHATSAPP_SPREAD_BONUS;
        }
        if (influencerBoost) {
            probability += INFLUENCER_SPREAD_BONUS;
        }

        return Math.min(MAX_SPREAD_PROBABILITY, probability);
    }

    private static CellState maybeCreateAmplificationAgent(CellState[][] grid,
                                                           int row,
                                                           int col,
                                                           int generation,
                                                           SimulationConfig config,
                                                           int randomRow) {
        if (!hasSpreaderNeighbor(grid, row, col)) {
            return CellState.IGNORANT;
        }

        double influencerChance = deterministicRandom(config.getSeed(), generation, randomRow, col, 6);
        if (influencerChance < INFLUENCER_CREATION_PROBABILITY) {
            return CellState.INFLUENCER;
        }

        double whatsAppChance = deterministicRandom(config.getSeed(), generation, randomRow, col, 7);
        if (whatsAppChance < WHATSAPP_CREATION_PROBABILITY) {
            return CellState.WHATSAPP_GROUP;
        }

        return CellState.IGNORANT;
    }

    private static boolean isInfluencedByWhatsAppGroup(CellState[][] grid, int row, int col) {
        return hasNeighborWithState(grid, row, col, CellState.WHATSAPP_GROUP, 1)
                && hasNeighborWithState(grid, row, col, CellState.SPREADER, 2);
    }

    private static boolean isInfluencedByInfluencer(CellState[][] grid, int row, int col) {
        return hasNeighborWithState(grid, row, col, CellState.INFLUENCER, 2)
                && hasNeighborWithState(grid, row, col, CellState.SPREADER, 3);
    }

    private static boolean hasNeighborWithState(CellState[][] grid,
                                                int row,
                                                int col,
                                                CellState expectedState,
                                                int radius) {
        for (int deltaRow = -radius; deltaRow <= radius; deltaRow++) {
            for (int deltaCol = -radius; deltaCol <= radius; deltaCol++) {
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

    private static double deterministicRandom(long seed, int generation, int row, int col, int ruleId) {
        long mixed = seed;
        mixed = mixed * 31 + generation;
        mixed = mixed * 31 + row;
        mixed = mixed * 31 + col;
        mixed = mixed * 31 + ruleId;
        return new Random(mixed).nextDouble();
    }
}
