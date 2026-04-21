package oap.template.tree;

public class ToStringRender {
    public final StringBuilder sb;
    public final int space;

    public ToStringRender() {
        this( new StringBuilder(), 0 );
    }

    private ToStringRender( StringBuilder sb, int space ) {
        this.sb = sb;
        this.space = space;
    }

    public ToStringRender append( String str ) {
        sb.append( str );

        return this;
    }

    public ToStringRender append( String format, Object... args ) {
        sb.append( String.format( format, args ) );

        return this;
    }

    public ToStringRender append( char ch ) {
        sb.append( ch );

        return this;
    }


    public ToStringRender spaceInc( int space ) {
        return new ToStringRender( sb, this.space + space );
    }

    public ToStringRender spaceInc() {
        return spaceInc( 2 );
    }

    public ToStringRender spaceDec( int space ) {
        return new ToStringRender( sb, this.space - space );
    }

    public ToStringRender spaceDec() {
        return spaceDec( 2 );
    }

    public ToStringRender n() {
        sb.append( '\n' );
        return this;
    }

    public ToStringRender space() {
        sb.repeat( " ", space );

        return this;
    }

    public ToStringRender nspace() {
        return this.n().space();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
