/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.etl;

import oap.io.Resources;
import oap.tsv.Model;
import oap.util.Lists;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class TableTest {
    @Test
    public void testSorted() {
        CountingKeyJoin join = CountingKeyJoin.fromResource( getClass(),
            getClass().getSimpleName() + "/2.tsv", Model.withoutHeader().withVersion("defaultVersion").s(0) ).get();
        StringExport export = new StringExport();
        List<Long> progress = new ArrayList<>();
        Table.fromResource( getClass(), getClass().getSimpleName() + "/1.tsv",
            Model.withoutHeader().withVersion("defaultVersion").s( 1, 2, 3 ) )
            .get()
            .progress( 2, progress::add )
            .sort( new int[]{ 0, 1 } )
            .transform( l -> l.addAll( join.on( (String) l.get( 1 ) ) ) )
            .export( export )
            .compute();
        assertEquals( export.toString(),
            Resources.readString( getClass(), getClass().getSimpleName() + "/sorted.tsv" ).get() );
        assertEquals( progress, Lists.of( 2l, 4l, 6l, 7l ) );
    }

    @Test
    public void testDistincted() {
        CountingKeyJoin join = CountingKeyJoin.fromResource( getClass(),
            getClass().getSimpleName() + "/2.tsv", Model.withoutHeader().withVersion("defaultVersion").s( 0 ) ).get();
        StringExport export = new StringExport();
        Table.fromResource( getClass(), getClass().getSimpleName() + "/1.tsv",
            Model.withoutHeader().withVersion("defaultVersion").s( 1, 2 ).i( 3 ) )
            .get()
            .sort( new int[]{ 0, 1 } )
            .join( 1, join )
            .groupBy( new int[]{ 0, 1 }, Accumulator.count(), Accumulator.intSum( 2 ), Accumulator.intSum( 3 ) )
            .export( export )
            .compute();
        assertEquals( export.toString(),
            Resources.readString( getClass(), getClass().getSimpleName() + "/distincted.tsv" ).get() );
    }

    @Test
    public void testJoined() {
        TableJoin join = TableJoin.fromResource( getClass(), getClass().getSimpleName() + "/2.tsv",
            Model.withoutHeader().withVersion("defaultVersion").s( 1, 2, 0 ), Lists.of( "0", "x" ) ).get();
        StringExport export = new StringExport();
        Table.fromResource( getClass(), getClass().getSimpleName() + "/1.tsv", Model.withoutHeader().withVersion("defaultVersion").s( 1, 2, 3 ) )
            .get()
            .sort( new int[]{ 0, 1 } )
            .join( 1, join )
            .export( export )
            .compute();
        assertEquals( export.toString(),
            Resources.readString( getClass(), getClass().getSimpleName() + "/joined.tsv" ).get() );
    }
}
