/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
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

package oap.tree;

import java.util.ArrayList;
import java.util.List;

public class TreeBuilder<T> {
    private List<Dimension> dimensions;
    private double hashFillFactor = 0.25;
    private int arrayToTree = Integer.MIN_VALUE;

    public TreeBuilder( List<Dimension> dimensions ) {
        this.dimensions = dimensions;
    }

    public TreeBuilder<T> withHashFillFactor( double hashFillFactor ) {
        this.hashFillFactor = hashFillFactor;

        return this;
    }

    public TreeBuilder<T> withArrayToTree( int arrayToTree ) {
        this.arrayToTree = arrayToTree;

        return this;
    }

    public final Tree<T> load( List<Tree.ValueData<T>> data ) {
        final Tree<T> tree = new Tree<>( dimensions, hashFillFactor, arrayToTree );
        tree.load( data );

        return tree;
    }
}
