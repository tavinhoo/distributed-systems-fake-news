package model;

import java.io.Serializable;

public class SimulationConfig implements Serializable {
    private final int rows;
    private final int columns;
    private final int generations;
    private final double initialSpreaderRate;
    private final double initialGrokPercentage;
    private final double initialWhatsAppGroupPercentage;
    private final double initialInfluencerPercentage;
    private final double initialJournalistPercentage;
    private final double spreadProbability;
    private final double inactiveProbability;
    private final double grokCorrectionProbability;
    private final double grokInfluenceReductionFactor;
    private final long seed;

    public SimulationConfig(int rows,
                            int columns,
                            int generations,
                            double initialSpreaderRate,
                            double initialGrokPercentage,
                            double spreadProbability,
                            double inactiveProbability,
                            double grokCorrectionProbability,
                            double grokInfluenceReductionFactor,
                            long seed) {
        this(rows, columns, generations, initialSpreaderRate, initialGrokPercentage,
                0.015, 0.010, 0.025, spreadProbability, inactiveProbability,
                grokCorrectionProbability, grokInfluenceReductionFactor, seed);
    }

    public SimulationConfig(int rows,
                            int columns,
                            int generations,
                            double initialSpreaderRate,
                            double initialGrokPercentage,
                            double initialWhatsAppGroupPercentage,
                            double initialInfluencerPercentage,
                            double spreadProbability,
                            double inactiveProbability,
                            double grokCorrectionProbability,
                            double grokInfluenceReductionFactor,
                            long seed) {
        this(rows, columns, generations, initialSpreaderRate, initialGrokPercentage,
                initialWhatsAppGroupPercentage, initialInfluencerPercentage, 0.025,
                spreadProbability, inactiveProbability, grokCorrectionProbability,
                grokInfluenceReductionFactor, seed);
    }

    public SimulationConfig(int rows,
                            int columns,
                            int generations,
                            double initialSpreaderRate,
                            double initialGrokPercentage,
                            double initialWhatsAppGroupPercentage,
                            double initialInfluencerPercentage,
                            double initialJournalistPercentage,
                            double spreadProbability,
                            double inactiveProbability,
                            double grokCorrectionProbability,
                            double grokInfluenceReductionFactor,
                            long seed) {
        this.rows = rows;
        this.columns = columns;
        this.generations = generations;
        this.initialSpreaderRate = initialSpreaderRate;
        this.initialGrokPercentage = initialGrokPercentage;
        this.initialWhatsAppGroupPercentage = initialWhatsAppGroupPercentage;
        this.initialInfluencerPercentage = initialInfluencerPercentage;
        this.initialJournalistPercentage = initialJournalistPercentage;
        this.spreadProbability = spreadProbability;
        this.inactiveProbability = inactiveProbability;
        this.grokCorrectionProbability = grokCorrectionProbability;
        this.grokInfluenceReductionFactor = grokInfluenceReductionFactor;
        this.seed = seed;
    }

    public static SimulationConfig defaultConfig() {
        return new SimulationConfig(80, 80, 100, 0.02, 0.03, 0.015, 0.010,
                0.015, 0.01, 0.25, 0.50, 42L);
    }

    public SimulationConfig withoutGrok() {
        return new SimulationConfig(rows, columns, generations, initialSpreaderRate, 0.0,
                initialWhatsAppGroupPercentage, initialInfluencerPercentage, initialJournalistPercentage,
                spreadProbability, inactiveProbability, 0.0, grokInfluenceReductionFactor, seed);
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getGenerations() {
        return generations;
    }

    public double getInitialSpreaderRate() {
        return initialSpreaderRate;
    }

    public double getInitialGrokPercentage() {
        return initialGrokPercentage;
    }

    public double getInitialWhatsAppGroupPercentage() {
        return initialWhatsAppGroupPercentage;
    }

    public double getInitialInfluencerPercentage() {
        return initialInfluencerPercentage;
    }

    public double getInitialJournalistPercentage() {
        return initialJournalistPercentage;
    }

    public double getSpreadProbability() {
        return spreadProbability;
    }

    public double getInactiveProbability() {
        return inactiveProbability;
    }

    public double getGrokCorrectionProbability() {
        return grokCorrectionProbability;
    }

    public double getGrokInfluenceReductionFactor() {
        return grokInfluenceReductionFactor;
    }

    public long getSeed() {
        return seed;
    }
}
