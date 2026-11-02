# SL38 — Mutation Testing With PIT

**Status:** Accepted.

**What must be true.** Whoever works on the filter can run PIT by hand and read which checks no test holds: a `pit`
profile, not in CI and with no threshold, with PIT and its JUnit 5 plugin pinned, run on a VM where the tests call real
tools, and a report that sorts the survivors into real gaps and harmless ones. The `pit` profile of
`porelo-message-matrix`, its branch `pit-try` until it is pushed, is the model.
