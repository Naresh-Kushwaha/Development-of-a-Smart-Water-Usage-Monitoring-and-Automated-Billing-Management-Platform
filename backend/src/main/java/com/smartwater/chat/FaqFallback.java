package com.smartwater.chat;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** Keyword answers used when the AI is not configured or temporarily unavailable, so the chat box never goes dead. */
@Component
public class FaqFallback {
    private record Entry(List<String> keywords, String answer) {}

    private static final List<Entry> ENTRIES = List.of(
        new Entry(List.of("log", "reading", "enter", "meter", "litres"),
            "To add a reading: Dashboard > \"Log today's meter usage\". Enter the date and the litres used that day (not the meter dial value). "
          + "You can add one reading per day and not for a future date. Flats without a meter cannot log readings; your admin can upload readings in bulk."),
        new Entry(List.of("pay", "payment", "razorpay", "upi", "card", "paid"),
            "Open Invoices and click \"Pay now\" next to an unpaid invoice (cards, UPI and net banking via Razorpay). The status turns PAID and a receipt is emailed. "
          + "If you do not see the button, online payment is not enabled; follow the payment instructions on your invoice PDF."),
        new Entry(List.of("bill", "invoice", "charge", "high", "amount", "pdf"),
            "Your invoice = water charge + your share of shared areas (garden, pool, lobby) + any admin adjustment. The Dashboard shows a live bill preview for the open cycle, "
          + "and the invoice is created when your admin finalizes the cycle. Use the PDF button on the Invoices page for the itemised copy."),
        new Entry(List.of("tariff", "rate", "tier", "slab", "price", "cost"),
            "Most apartments use a tiered tariff: the first block of kilolitres is charged at a base rate and the rest at a higher rate, plus an optional fixed charge. "
          + "Others split the apartment's actual water purchase cost in proportion to consumption. Your admin chooses the mode and rates."),
        new Entry(List.of("leak", "alert", "overuse", "threshold", "notification", "warning"),
            "You get an overuse alert when your usage this month passes the apartment's limit, and a possible-leak alert when a day's reading is far above your own recent average. "
          + "Both appear on the Alerts page and are emailed to you."),
        new Entry(List.of("compare", "similar", "neighbour", "neighbor", "average", "rank"),
            "The Compare page shows your average daily litres for the last 30 days against similar-sized flats (within 20 per cent of your area) and the apartment average. Other households stay anonymous."),
        new Entry(List.of("save", "tips", "reduce", "conserve", "lower"),
            "Quick ways to use less water: fix dripping taps, take shorter showers, run washing machines with full loads, switch the tap off while brushing, and check the toilet cistern for silent leaks."),
        new Entry(List.of("password", "login", "log in", "sign in", "google", "register", "sign up", "account", "logout", "sign out"),
            "Sign up as a resident with your apartment code and unit number (ask your admin), or use \"Continue with Google\" if it is shown. "
          + "You can change your name and password on the Profile page, and sign out from the sidebar."),
        new Entry(List.of("csv", "upload", "import"),
            "Admins: Overview > Upload meter readings. The CSV needs the columns unit_number, date and litres (one row per unit per day). Duplicates are skipped and problem rows are listed. A template can be downloaded on that page."),
        new Entry(List.of("cycle", "finalize", "finalise", "archive", "adjust"),
            "Admins: Billing cycles > open a cycle with start/end dates, check the live preview, then Finalize to create invoices and notify residents. "
          + "After that you can adjust an invoice (negative = credit), mark it paid, or archive the cycle."),
        new Entry(List.of("household", "unit", "assign", "occupants", "area"),
            "Admins: Households > add each unit with its area, number of occupants and an optional meter number (blank = no meter). Use \"Assign resident\" to link a registered resident's email to a unit.")
    );

    private static final String DEFAULT =
        "I can help with logging readings, understanding your bill and tariff, alerts, paying invoices, comparing usage and account questions. "
      + "Try asking, for example: \"How do I log a reading?\" or \"Why is my bill high?\"";

    public String answer(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        Entry best = null;
        int bestScore = 0;
        for (Entry e : ENTRIES) {
            int score = 0;
            for (String k : e.keywords()) if (q.contains(k)) score++;
            if (score > bestScore) { bestScore = score; best = e; }
        }
        return best == null ? DEFAULT : best.answer();
    }
}
