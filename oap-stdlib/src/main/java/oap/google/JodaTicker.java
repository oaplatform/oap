package oap.google;

import com.google.common.base.Ticker;
import org.joda.time.DateTimeUtils;

public class JodaTicker extends Ticker {
    public static final Ticker JODA_TICKER = new JodaTicker();

    private JodaTicker() {
    }

    @Override
    public long read() {
        return DateTimeUtils.currentTimeMillis() * 1000000L;
    }
}
