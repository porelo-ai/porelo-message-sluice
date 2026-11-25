package org.fuin.porelo.msgsluice.filter.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Profile 2's time of writing, an instant in UTC in whole seconds on a real calendar. Harm guarded:
 * a date the calendar does not have, which one reader takes and another refuses, passing the
 * filter.
 */
class ReviewProfileCheckInstantTest {

    /** Instants at the calendar's edges are real. */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "2024-02-29T00:00:00Z",
                "2026-04-30T23:59:59Z",
                "2026-12-31T23:59:59Z",
                "1970-01-01T00:00:00Z"
            })
    void anInstantOnARealCalendarIsAdmitted(final String text) {
        assertThat(ReviewProfileCheck.instant(text)).as(text).isTrue();
    }

    /** A date or time the calendar does not have, or another way of writing one, is refused. */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "2025-02-29T00:00:00Z",
                "2026-02-30T00:00:00Z",
                "2026-04-31T00:00:00Z",
                "2026-13-01T00:00:00Z",
                "2026-00-10T00:00:00Z",
                "2026-01-00T00:00:00Z",
                "2026-01-01T24:00:00Z",
                "2026-01-01T25:00:00Z",
                "2026-01-01T00:60:00Z",
                "2016-12-31T23:59:60Z",
                "2026-01-01T00:00:00.5Z",
                "2026-01-01T00:00:00+00:00",
                "2026-01-01t00:00:00z",
                "2026-01-01T00:00:00Z\n",
                "2026-01-01T00:00:00"
            })
    void anInstantTheCalendarDoesNotHaveIsRefused(final String text) {
        assertThat(ReviewProfileCheck.instant(text)).as(text).isFalse();
    }
}
