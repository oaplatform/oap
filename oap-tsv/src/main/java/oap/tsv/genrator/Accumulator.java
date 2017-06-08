package oap.tsv.genrator;

import org.apache.commons.lang3.builder.Builder;

/**
 * Created by macchiatow on 07.06.17.
 */
public interface Accumulator<T> extends Builder<T> {

    Accumulator accept( Object o );
}
