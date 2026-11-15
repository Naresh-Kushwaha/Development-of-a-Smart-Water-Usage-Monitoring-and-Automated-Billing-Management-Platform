package com.smartwater.service;

import java.util.ArrayList;
import java.util.List;

public final class Tips {
    private Tips() {}
    public static final List<String> ALL = List.of(
        "Fix dripping taps: one drip per second wastes about 10,000 litres a year.",
        "Shorten showers by two minutes to save up to 20 litres each time.",
        "Run washing machines and dishwashers only with full loads.",
        "Turn off the tap while brushing teeth or shaving - it saves around 6 litres a minute.",
        "Check for leaks: put food colouring in the toilet cistern; if colour reaches the bowl, it leaks.",
        "Water plants early morning or evening and use a bucket instead of a hose.",
        "Reuse RO reject water or rinse water for mopping and plants.",
        "Install low-flow aerators on taps and showerheads to cut flow by up to 30%.",
        "Read your meter before and after a few hours away - any movement means a hidden leak.");

    /** Rotates through the list so the feed changes daily. */
    public static List<String> daily(int count, int dayOfYear) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(ALL.get((dayOfYear + i) % ALL.size()));
        return out;
    }
}
