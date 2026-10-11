package io.github.fupacat;

public enum SqlVersion {
    SQL80(80),
    SQL90(90),
    SQL100(100),
    SQL110(110),
    SQL120(120),
    SQL130(130),
    SQL140(140),
    SQL150(150),
    SQL160(160),
    SQL170(170),
    SQL180(180);

    private final int compatibilityLevel;

    SqlVersion(int compatibilityLevel) {
        this.compatibilityLevel = compatibilityLevel;
    }

    // TRACEABILITY_MAPPING: REQ-020
    public int getCompatibilityLevel() {
        return compatibilityLevel;
    }
}
