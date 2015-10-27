package oap.util;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
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
