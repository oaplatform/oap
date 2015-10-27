package oap.util;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class BitSetTest {
    @Test
    public void testMaxBit() {
        BitSet bitSet = new BitSet( 10 );
        bitSet.set( 4 );
        Assert.assertEquals( bitSet.max(), 4 );

        bitSet.set( 6 );
        Assert.assertEquals( bitSet.max(), 6 );

        bitSet.set( 6, false );
        Assert.assertEquals( bitSet.max(), 4 );
    }
}
