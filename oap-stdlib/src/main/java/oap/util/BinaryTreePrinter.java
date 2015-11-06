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

package oap.util;

public final class BinaryTreePrinter {
    public interface TreeNode {
        TreeNode getLeft();

        TreeNode getRight();

        TreeNode getAny();

        void print( StringBuilder out );
    }

    public static void print( TreeNode node, StringBuilder out ) {
        print( "", true, node, out, "r" );
    }

    private static void print( String prefix, boolean isTail, TreeNode node, StringBuilder out, String type ) {
        out.append( prefix ).append( isTail ? "└── " : "├── " ).append( type ).append( ":" );
        if( node != null ) {
            node.print( out );
            out.append( "\n" );

            if( node.getLeft() != null ) print( prefix + (isTail ? "    " : "│   "), false, node.getLeft(), out, "l" );
            if( node.getRight() != null ) {
                print( prefix + (isTail ? "    " : "│   "), node.getAny() == null, node.getRight(), out, "r" );
            }
            if( node.getAny() != null ) {
                print( prefix + (isTail ? "    " : "│   "), true, node.getAny(), out, "a" );
            }
        }
    }
}
