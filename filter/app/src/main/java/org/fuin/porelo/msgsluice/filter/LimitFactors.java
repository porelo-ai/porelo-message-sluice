package org.fuin.porelo.msgsluice.filter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Porelo's four limit factors, read from the limits line {@code
 * services=1.0,machines=1.0,polling=1.0,retries=1.0} that Porelo hands every program it starts.
 * Read here rather than through Porelo's own reader, since the filter compiles against nothing of
 * Porelo; the grammar is Porelo's. Only {@code polling} has a use in the filter: it calls no
 * outside service, starts no program and retries nothing, so the other three are read and checked
 * but change nothing.
 *
 * @param services an answer from outside
 * @param machines a program, container or machine starting, stopping or running a command
 * @param polling how often something is looked at again
 * @param retries how often, and after what pause, a failed step is tried again
 */
public record LimitFactors(
        BigDecimal services, BigDecimal machines, BigDecimal polling, BigDecimal retries) {

    /** All four at 1.0: a line that names none. */
    public static final LimitFactors NONE =
            new LimitFactors(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);

    private static final List<String> NAMES = List.of("services", "machines", "polling", "retries");

    /** No sign, exponent, comma or leading zero; at most three digits before the dot. */
    private static final Pattern VALUE = Pattern.compile("(?:0|[1-9][0-9]{0,2})(?:\\.[0-9]{1,2})?");

    private static final BigDecimal LOWEST = new BigDecimal("0.1");

    private static final BigDecimal HIGHEST = new BigDecimal("100");

    /**
     * Reads a limits line; an empty one is all four at 1.0.
     *
     * @throws ConfigException naming what is wrong: an unknown name, a name given twice, a value
     *     that is no decimal with at most two digits after the dot, or one outside 0.1 to 100
     */
    public static LimitFactors parse(final String line) throws ConfigException {
        final Map<String, BigDecimal> given = new HashMap<>();
        if (!line.isEmpty()) {
            for (final String pair : line.split(",", -1)) {
                final int eq = pair.indexOf('=');
                if (eq < 1) {
                    throw new ConfigException("'" + pair + "' is no name=value");
                }
                final String name = pair.substring(0, eq);
                if (!NAMES.contains(name)) {
                    throw new ConfigException("'" + name + "' is no limit");
                }
                if (given.containsKey(name)) {
                    throw new ConfigException(name + " is given twice");
                }
                final String text = pair.substring(eq + 1);
                if (!VALUE.matcher(text).matches()) {
                    throw new ConfigException(name + " '" + text + "' is no decimal");
                }
                final BigDecimal value = new BigDecimal(text);
                if (value.compareTo(LOWEST) < 0 || value.compareTo(HIGHEST) > 0) {
                    throw new ConfigException(name + " " + text + " lies outside 0.1 to 100");
                }
                given.put(name, value);
            }
        }
        return new LimitFactors(
                given.getOrDefault("services", BigDecimal.ONE),
                given.getOrDefault("machines", BigDecimal.ONE),
                given.getOrDefault("polling", BigDecimal.ONE),
                given.getOrDefault("retries", BigDecimal.ONE));
    }

    /**
     * An interval of the polling group: its configured length times {@code polling}, rounded up to
     * a whole millisecond.
     *
     * @throws ConfigException when the product no longer fits a duration
     */
    public Duration poll(final Duration configured) throws ConfigException {
        try {
            return Duration.ofMillis(
                    BigDecimal.valueOf(configured.toMillis())
                            .multiply(polling)
                            .setScale(0, RoundingMode.CEILING)
                            .longValueExact());
        } catch (final ArithmeticException ex) {
            throw new ConfigException(
                    configured.toMillis() + " ms times polling=" + polling + " is too long");
        }
    }
}
