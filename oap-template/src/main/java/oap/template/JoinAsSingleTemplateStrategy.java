package oap.template;

import java.lang.reflect.Type;

/**
 * Created by macchiatow on 21.06.17.
 */
public class JoinAsSingleTemplateStrategy implements TemplateStrategy<Template.Line>{

    @Override
    public void mapFirstJoin( StringBuilder c, Template.Line line ){
        c.append( "\njb = new StringBuilder();\n" );
    }

    @Override
    public void mapLastJoin( StringBuilder c, Template.Line line ){
        c.append( "\nacc.accept( " );
        function( c, line.function, () -> escape( c, () -> c.append( " jb.toString()" ) ) );
        c.append( " );" );
    }

    @Override
    public void mapCollection( StringBuilder c, Type cc, Template.Line line, String field ) {
        c.append( "{acc.accept( '[' + " );
        function( c, line.function, () -> escape( c, () -> c.append( " Strings.join( " ).append( field ).append( " )" ) ) );
        c.append( " + ']' );}" );
    }

    @Override
    public void mapObject( StringBuilder c, Type cc, Template.Line line, String field, boolean isJoin ){
        if ( isJoin ){
            c.append( "jb.append( " );
        } else {
            c.append( "acc.accept( " );
        }        function( c, line.function, () -> escape( c, () -> c.append( " String.valueOf( " ).append( field ).append( " )" ) ) );
        c.append( " );" );
    }

    @Override
    public void mapPrimitive( StringBuilder c, Type cc, Template.Line line, String field, boolean isJoin ) {
        if ( isJoin ){
            c.append( "jb.append( " );
        } else {
            c.append( "acc.accept( " );
        }
        function( c, line.function, () -> c.append( field ) );
        c.append( " );" );
    }

    @Override
    public void mapInterJoin( StringBuilder c, Type cc, Template.Line line, String field ){
        c.append( "jb.append( " );
        function( c, line.function, () -> c.append( field ) );
        c.append( " );\n" );
    }

    @Override
    public void mapString( StringBuilder c, Type cc, Template.Line line, String field, boolean isJoin ) {
        if ( isJoin ){
            c.append( "jb.append( " );
        } else {
            c.append( "acc.accept( " );
        }
        function( c, line.function, () -> escape( c, () -> c.append( field ) ) );
        c.append( " );" );
    }

    @Override
    public void mapEnum( StringBuilder c, Type cc, Template.Line line, String field, boolean isJoin ) {
        if ( isJoin ){
            c.append( "jb.append( " );
        } else {
            c.append( "acc.accept( " );
        }
        function( c, line.function, () -> c.append( field ) );
        c.append( " );" );
    }
}
