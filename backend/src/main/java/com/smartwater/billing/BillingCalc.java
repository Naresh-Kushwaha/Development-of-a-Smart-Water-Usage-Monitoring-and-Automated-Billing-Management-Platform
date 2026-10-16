package com.smartwater.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Pure, side-effect-free billing and statistics helpers (easy to unit test). */
public final class BillingCalc {
    private BillingCalc() {}

    public record Unit(long id, BigDecimal areaSqft, int occupants, boolean metered, BigDecimal kl) {}

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /** Tiered tariff: first {@code limit} kL at r1, remainder at r2, plus a fixed charge. */
    public static BigDecimal tiered(BigDecimal kl, BigDecimal limit, BigDecimal r1, BigDecimal r2, BigDecimal fixed) {
        BigDecimal first = kl.min(limit);
        BigDecimal excess = kl.subtract(limit).max(ZERO);
        return first.multiply(r1).add(excess.multiply(r2)).add(fixed).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Splits {@code amount} proportionally to {@code weights}. Rounds to 2 dp and assigns the rounding
     * remainder to the heaviest weight so the parts always add up exactly. Zero total weight => equal split.
     */
    public static Map<Long, BigDecimal> distribute(BigDecimal amount, Map<Long, BigDecimal> weights) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        if (weights.isEmpty()) return out;
        BigDecimal amt = amount.setScale(2, RoundingMode.HALF_UP);
        Map<Long, BigDecimal> w = new LinkedHashMap<>(weights);
        BigDecimal total = w.values().stream().reduce(ZERO, BigDecimal::add);
        if (total.signum() == 0) {
            w.replaceAll((k, v) -> BigDecimal.ONE);
            total = BigDecimal.valueOf(w.size());
        }
        BigDecimal assigned = ZERO;
        Long heaviest = null;
        BigDecimal max = BigDecimal.valueOf(-1);
        for (var e : w.entrySet()) {
            BigDecimal share = amt.multiply(e.getValue()).divide(total, 2, RoundingMode.HALF_UP);
            out.put(e.getKey(), share);
            assigned = assigned.add(share);
            if (e.getValue().compareTo(max) > 0) { max = e.getValue(); heaviest = e.getKey(); }
        }
        out.merge(heaviest, amt.subtract(assigned), BigDecimal::add);
        return out;
    }

    /**
     * Weights for consumption-proportional cost sharing. Metered households weigh their kL; unmetered ones
     * fall back to a flat area basis, estimated at the metered average kL per sq ft. If nobody has usable
     * metered data, every household is weighted by area.
     */
    public static Map<Long, BigDecimal> costWeights(List<Unit> units) {
        BigDecimal meteredKl = ZERO, meteredArea = ZERO;
        for (Unit u : units) if (u.metered()) { meteredKl = meteredKl.add(u.kl()); meteredArea = meteredArea.add(u.areaSqft()); }
        Map<Long, BigDecimal> w = new LinkedHashMap<>();
        if (meteredKl.signum() > 0 && meteredArea.signum() > 0) {
            BigDecimal perSqft = meteredKl.divide(meteredArea, 8, RoundingMode.HALF_UP);
            for (Unit u : units) w.put(u.id(), u.metered() ? u.kl() : perSqft.multiply(u.areaSqft()));
        } else {
            for (Unit u : units) w.put(u.id(), u.areaSqft());
        }
        return w;
    }

    public static Map<Long, BigDecimal> sharedWeights(List<Unit> units, boolean byArea) {
        Map<Long, BigDecimal> w = new LinkedHashMap<>();
        for (Unit u : units) w.put(u.id(), byArea ? u.areaSqft() : BigDecimal.valueOf(u.occupants()));
        return w;
    }

    /** Leak/anomaly rule: latest reading more than 2 sample standard deviations above the baseline mean. */
    public static boolean isAnomaly(double latest, double[] baseline) {
        if (baseline.length < 7) return false;
        double mean = Arrays.stream(baseline).average().orElse(0);
        double var = Arrays.stream(baseline).map(x -> (x - mean) * (x - mean)).sum() / (baseline.length - 1);
        double sd = Math.sqrt(var);
        return sd > 0 && latest > mean + 2 * sd;
    }

    public static double mean(double[] v) { return Arrays.stream(v).average().orElse(0); }
}
