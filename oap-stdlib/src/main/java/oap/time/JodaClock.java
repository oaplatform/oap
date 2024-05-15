package oap.time;

import org.joda.time.DateTimeUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class JodaClock extends Clock {
    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone( ZoneId zone ) {
        return this;
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli( millis() );
    }

    @Override
    public long millis() {
        return DateTimeUtils.currentTimeMillis();
    }
}
