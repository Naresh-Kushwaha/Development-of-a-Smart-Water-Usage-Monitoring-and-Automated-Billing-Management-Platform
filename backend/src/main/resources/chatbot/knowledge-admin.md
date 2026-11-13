# AquaTrack help centre - apartment admins

## Setting up
- "Register apartment" creates the apartment, your admin account and a default tariff: first 10 kL at INR 12 per kL, then INR 20, no fixed charge, overuse alert at 15 kL per month. Change it on the Tariff page.
- The apartment sign-up code is shown in the sidebar. Give it to residents; they need it to sign up.

## Households
- Households page: add each unit with unit number, area (sq ft), number of occupants and an optional meter number. A blank meter number means the unit has no meter and is billed by area.
- "Assign resident" links a registered resident (by email) to a unit. Residents sign up first, then you assign, or they sign up directly with the code and the unit number.
- Edit changes the details. Delete removes the unit together with its readings and invoices.

## Uploading meter readings
- Overview > Upload meter readings. CSV columns: unit_number,date,litres - one row per unit per day. Dates can be yyyy-MM-dd or dd-MM-yyyy; comma or semicolon separated; maximum 5 MB. A template can be downloaded on the page.
- Rows with an unknown unit, a unit without a meter, negative litres, or a future date are reported and skipped. Duplicates (same unit and date) are skipped. The rest is imported.
- After an import the overuse and leak checks run. "Run checks now" on the Overview page runs them manually; they also run every morning.
- Overview also shows usage per household for any date range.

## Tariff page
- Tier-1 limit (kL per cycle), tier-1 rate, tier-2 rate, fixed charge per household, overuse alert threshold (kL per month), shared-area split (by flat size or by occupancy), and billing mode: Tiered tariff or Cost sharing.

## Water purchases
- Record each tanker or municipal delivery: date, source, volume (kL) and cost per kL.
- Purchases inside a cycle's dates set the cost of the shared-area water, and in Cost sharing mode they are the amount that is split between flats. With no purchases in the cycle, shared-area water is priced at the tier-1 rate.

## Billing cycles
- Open a cycle with a label, start and end date and the shared-area usage in kL (garden, pool, lobby). Cycles cannot overlap.
- While a cycle is OPEN the table is a live preview (nothing is saved).
- Finalize creates one invoice per household, notifies residents by email and in the app. Only an OPEN cycle can be finalized.
- Shared-area cost = shared kL x the average purchase cost per kL in that cycle, split by flat size or occupancy.
- Tiered mode: each flat pays its own tiered charge. Cost sharing mode: (total purchase cost minus shared-area cost) is split in proportion to metered consumption; flats without meters are estimated from area.
- After finalizing you can Adjust an invoice (negative amount = credit, with a note), Mark paid (cash or bank transfer), or Archive the cycle. PDF downloads an invoice.
- If online payment is enabled, residents can pay UNPAID invoices themselves with "Pay now" and the invoice turns PAID automatically.

## Alerts
- Overuse: monthly usage above the threshold, once per month per resident. Leak: a latest reading well above that household's own recent average (needs about a week of history). Residents and admins are notified; admins also get an anomaly summary email.
