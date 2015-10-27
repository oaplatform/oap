package oap.util;

import java.util.function.BiFunction;

public class Pair<K, V> {
    public final K _1;
    public final V _2;

    public Pair( K _1, V _2 ) {
        this._1 = _1;
        this._2 = _2;
    }

    public static <K, V> Pair<K, V> __( K _1, V _2 ) {
        return new Pair<>( _1, _2 );
    }

    public <KR, VR> Pair<KR, VR> map( BiFunction<K, V, Pair<KR, VR>> mapper ) {
        return mapper.apply( _1, _2 );
    }

    public <R> R fold( BiFunction<K, V, R> mapper ) {
        return mapper.apply( _1, _2 );
    }

    @Override
    public boolean equals( Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        Pair pair = (Pair) o;

        if( _1 != null ? !_1.equals( pair._1 ) : pair._1 != null ) return false;
        if( _2 != null ? !_2.equals( pair._2 ) : pair._2 != null ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + (_2 != null ? _2.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "__(" + _1 + ", " + _2 + ')';
    }
}
