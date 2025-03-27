package oap.net;

import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import oap.util.Lists;
import org.apache.commons.collections4.IteratorUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class IpRangeTree<Data> implements Serializable, Iterable<Data> {
    @Serial
    private static final long serialVersionUID = 2010688027906213938L;
    public final int shardBits;
    public final int shardSize;
    public final long shardMask;
    public final long valueMask;
    private final Int2ObjectAVLTreeMap<IpRange<Data>>[] hash;

    @SuppressWarnings( "unchecked" )
    public IpRangeTree( int shardBits ) {
        this.shardBits = shardBits;
        this.shardSize = IntMath.pow( 2, shardBits );
        this.shardMask = ( ( long ) shardSize - 1 ) << 32 - shardBits;
        this.valueMask = 0xFFFFFFFFL ^ shardMask;

        hash = new Int2ObjectAVLTreeMap[shardSize];
        for( int i = 0; i < shardSize; i++ ) {
            hash[i] = new Int2ObjectAVLTreeMap<>();
        }
    }

    public void addRange( long lowAddress, long highAddress, Data data ) {
        int lowAddressWithoutShard = getWithoutShard( lowAddress );
        int highAddressWithoutShard = getWithoutShard( highAddress );

        int lowShard = getShard( lowAddress );
        int highShard = getShard( highAddress );
        for( int i = lowShard; i <= highShard; i++ ) {
            int low = i == lowShard ? lowAddressWithoutShard : 0;
            int high = i == highShard ? highAddressWithoutShard : Integer.MAX_VALUE;

            try {
                IpRange<Data> ipRange = new IpRange<>( high, data );

                hash[i].put( low, ipRange );
            } catch( Exception e ) {
                throw new RuntimeException( e );
            }
        }
    }

    public long size() {
        return Stream.of( hash ).mapToInt( Int2ObjectAVLTreeMap::size ).sum();
    }

    @Nullable
    public Data lookUp( long ipv4 ) {
        int shard = getShard( ipv4 );
        int suffix = getWithoutShard( ipv4 );

        Int2ObjectAVLTreeMap<IpRange<Data>> map = hash[shard];

        Int2ObjectSortedMap<IpRange<Data>> headMap = map.headMap( suffix );
        Map.Entry<Integer, IpRange<Data>> lastEntry = headMap.lastEntry();
        if( lastEntry != null && lastEntry.getValue().highAddress >= suffix ) {
            return lastEntry.getValue().data;
        }
        Int2ObjectSortedMap<IpRange<Data>> tailMap = map.tailMap( suffix );
        Map.Entry<Integer, IpRange<Data>> firstEntry = tailMap.firstEntry();
        if( firstEntry != null && firstEntry.getValue().highAddress >= suffix && firstEntry.getKey() <= suffix ) {
            return firstEntry.getValue().data;
        }

        return null;
    }

    @Override
    @Nonnull
    public Iterator<Data> iterator() {
        Iterator<IpRange<Data>> iterator = IteratorUtils.chainedIterator( Lists.map( hash, h -> h.values().iterator() ) );


        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Data next() {
                return iterator.next().data;
            }
        };
    }

    @Override
    public void forEach( Consumer<? super Data> action ) {
        for( int i = 0; i < shardSize; i++ ) {
            hash[i].forEach( ( _, v ) -> action.accept( v.data ) );

        }
    }

    public int getShard( long ipv4 ) {
        return ( int ) ( ( ipv4 & shardMask ) >> ( 32 - shardBits ) );
    }

    public int getWithoutShard( long ipv4 ) {
        return ( int ) ( ipv4 & valueMask );
    }

    public static class IpRange<Data> implements Serializable {
        @Serial
        private static final long serialVersionUID = 736250196728147253L;

        public final int highAddress;
        public Data data;

        public IpRange( int highAddress, Data data ) {
            this.highAddress = highAddress;
            this.data = data;
        }
    }
}
