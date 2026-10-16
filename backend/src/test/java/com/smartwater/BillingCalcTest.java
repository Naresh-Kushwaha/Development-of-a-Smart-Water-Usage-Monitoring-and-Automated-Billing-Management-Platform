package com.smartwater;

import com.smartwater.billing.BillingCalc;
import com.smartwater.billing.BillingCalc.Unit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BillingCalcTest {
    private static BigDecimal bd(String s) { return new BigDecimal(s); }



    @Test
    void tieredChargeAppliesBaseRateThenHigherRate() {
        // 10 kL @ 12 + 5 kL @ 20 + 50 fixed = 120 + 100 + 50
        assertEquals(bd("270.00"), BillingCalc.tiered(bd("15"), bd("10"), bd("12"), bd("20"), bd("50")));
        // below the tier limit: only the base rate applies
        assertEquals(bd("84.00"), BillingCalc.tiered(bd("7"), bd("10"), bd("12"), bd("20"), BigDecimal.ZERO));
    }

    @Test
    void distributeAlwaysSumsToTheAmount() {
        Map<Long, BigDecimal> w = new LinkedHashMap<>();
        w.put(1L, bd("1")); w.put(2L, bd("1")); w.put(3L, bd("1"));
        Map<Long, BigDecimal> out = BillingCalc.distribute(bd("100.00"), w);
        assertEquals(bd("100.00"), out.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void distributeIsProportionalToWeights() {
        Map<Long, BigDecimal> w = new LinkedHashMap<>();
        w.put(1L, bd("10")); w.put(2L, bd("30"));
        Map<Long, BigDecimal> out = BillingCalc.distribute(bd("200"), w);
        assertEquals(bd("50.00"), out.get(1L));
        assertEquals(bd("150.00"), out.get(2L));
    }

    @Test
    void zeroWeightsFallBackToEqualSplit() {
        Map<Long, BigDecimal> w = new LinkedHashMap<>();
        w.put(1L, BigDecimal.ZERO); w.put(2L, BigDecimal.ZERO);
        assertEquals(bd("50.00"), BillingCalc.distribute(bd("100"), w).get(1L));
    }

    @Test
    void unmeteredHouseholdsFallBackToFlatAreaBasis() {
        List<Unit> units = List.of(
                new Unit(1, bd("1000"), 3, true, bd("20")),     // 0.02 kL / sq ft
                new Unit(2, bd("500"), 2, false, bd("0")));     // estimated 500 * 0.02 = 10 kL
        Map<Long, BigDecimal> w = BillingCalc.costWeights(units);
        assertEquals(0, bd("20").compareTo(w.get(1L)));
        assertEquals(0, bd("10").compareTo(w.get(2L).setScale(2, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void noMeteredDataUsesAreaForEveryone() {
        List<Unit> units = List.of(new Unit(1, bd("800"), 2, false, bd("0")), new Unit(2, bd("400"), 2, false, bd("0")));
        Map<Long, BigDecimal> w = BillingCalc.costWeights(units);
        assertEquals(bd("800"), w.get(1L));
        assertEquals(bd("400"), w.get(2L));
    }

    @Test
    void anomalyNeedsEnoughHistoryAndMoreThanTwoSigma() {
        double[] stable = {400, 410, 390, 405, 395, 400, 402, 398, 401, 399};
        assertTrue(BillingCalc.isAnomaly(900, stable));
        assertFalse(BillingCalc.isAnomaly(410, stable));
        assertFalse(BillingCalc.isAnomaly(900, new double[]{400, 410, 390}));      // too little history
        assertFalse(BillingCalc.isAnomaly(900, new double[]{400, 400, 400, 400, 400, 400, 400})); // zero variance
    }
}
