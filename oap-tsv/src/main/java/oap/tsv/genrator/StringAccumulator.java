package oap.tsv.genrator;

/**
 * Created by macchiatow on 07.06.17.
 */
public class StringAccumulator implements Accumulator<String> {

    private final java.lang.StringBuilder sb = new java.lang.StringBuilder();

    @Override
    public String build() {
        return sb.toString();
    }

    @Override
    public Accumulator accept( Object o ) {
        sb.append( o );
        return this;
    }
}
