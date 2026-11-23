package org.fuin.porelo.msgsluice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class LimitFactorsTest {

    /**
     * Porelo's own cases for every reader of the line, a copy of {@code limit-factors-cases.json}
     * that {@code porelo-wire} publishes beside its reader: each good line read to its four
     * factors, each refused line refused naming what the case says.
     */
    private static JsonNode cases() throws IOException {
        try (InputStream in =
                LimitFactorsTest.class.getResourceAsStream("limit-factors-cases.json")) {
            assertThat(in).as("the cases file").isNotNull();
            return new ObjectMapper().readTree(in);
        }
    }

    static Stream<JsonNode> goodLines() throws IOException {
        return StreamSupport.stream(cases().get("good").spliterator(), false);
    }

    static Stream<JsonNode> refusedLines() throws IOException {
        return StreamSupport.stream(cases().get("refused").spliterator(), false);
    }

    @ParameterizedTest
    @MethodSource("goodLines")
    void porelosGoodLinesAreReadToTheirFactors(final JsonNode good) throws Exception {
        final LimitFactors factors = LimitFactors.parse(good.get("line").asText());

        assertThat(factors.services()).isEqualByComparingTo(good.get("services").asText());
        assertThat(factors.machines()).isEqualByComparingTo(good.get("machines").asText());
        assertThat(factors.polling()).isEqualByComparingTo(good.get("polling").asText());
        assertThat(factors.retries()).isEqualByComparingTo(good.get("retries").asText());
    }

    @ParameterizedTest
    @MethodSource("refusedLines")
    void porelosRefusedLinesAreRefusedNamingWhatIsWrong(final JsonNode refused) {
        assertThatThrownBy(() -> LimitFactors.parse(refused.get("line").asText()))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining(refused.get("names").asText());
    }

    @Test
    void theCasesFileHoldsBothKinds() throws Exception {
        assertThat(goodLines()).isNotEmpty();
        assertThat(refusedLines()).isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({"200,1,200", "200,2,400", "2000,0.1,200", "1,0.1,1", "3,0.25,1", "0,50,0"})
    void anIntervalIsTimesPollingRoundedUpToAMillisecond(
            final long configured, final String polling, final long effective) throws Exception {
        final LimitFactors factors =
                new LimitFactors(
                        BigDecimal.ONE, BigDecimal.ONE, new BigDecimal(polling), BigDecimal.ONE);

        assertThat(factors.poll(Duration.ofMillis(configured)))
                .isEqualTo(Duration.ofMillis(effective));
    }

    @ParameterizedTest
    @ValueSource(strings = {"services=50", "machines=50", "retries=50"})
    void theOtherThreeFactorsChangeNoInterval(final String line) throws Exception {
        assertThat(LimitFactors.parse(line).poll(Duration.ofMillis(200)))
                .isEqualTo(Duration.ofMillis(200));
    }

    @Test
    void anIntervalTooLongForADurationIsRefused() throws Exception {
        final LimitFactors factors = LimitFactors.parse("polling=2");

        assertThatThrownBy(() -> factors.poll(Duration.ofMillis(Long.MAX_VALUE)))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("too long");
    }
}
