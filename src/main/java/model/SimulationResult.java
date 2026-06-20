package model;

public class SimulationResult {
    private final CellState[][] finalGrid;
    private final int generations;
    private final long elapsedNanos;
    private final int ignorantCount;
    private final int spreaderCount;
    private final int inactiveCount;
    private final int grokCount;
    private final int botCount;
    private final int influencerCount;
    private final int echoChamberCount;
    private final int factCheckerCount;
    private final int journalistCount;
    private final int neutralizedByGrokCount;

    public SimulationResult(CellState[][] finalGrid,
                            int generations,
                            long elapsedNanos,
                            int ignorantCount,
                            int spreaderCount,
                            int inactiveCount,
                            int grokCount,
                            int botCount,
                            int influencerCount,
                            int echoChamberCount,
                            int factCheckerCount,
                            int journalistCount,
                            int neutralizedByGrokCount) {
        this.finalGrid = finalGrid;
        this.generations = generations;
        this.elapsedNanos = elapsedNanos;
        this.ignorantCount = ignorantCount;
        this.spreaderCount = spreaderCount;
        this.inactiveCount = inactiveCount;
        this.grokCount = grokCount;
        this.botCount = botCount;
        this.influencerCount = influencerCount;
        this.echoChamberCount = echoChamberCount;
        this.factCheckerCount = factCheckerCount;
        this.journalistCount = journalistCount;
        this.neutralizedByGrokCount = neutralizedByGrokCount;
    }

    public CellState[][] getFinalGrid() {
        return finalGrid;
    }

    public int getGenerations() {
        return generations;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }

    public int getIgnorantCount() {
        return ignorantCount;
    }

    public int getSpreaderCount() {
        return spreaderCount;
    }

    public int getInactiveCount() {
        return inactiveCount;
    }

    public int getGrokCount() {
        return grokCount;
    }

    public int getBotCount() {
        return botCount;
    }

    public int getInfluencerCount() {
        return influencerCount;
    }

    public int getEchoChamberCount() {
        return echoChamberCount;
    }

    public int getFactCheckerCount() {
        return factCheckerCount;
    }

    public int getJournalistCount() {
        return journalistCount;
    }

    public int getNeutralizedByGrokCount() {
        return neutralizedByGrokCount;
    }
}
