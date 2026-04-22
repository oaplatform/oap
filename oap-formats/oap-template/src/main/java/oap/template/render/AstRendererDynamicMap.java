package oap.template.render;

import lombok.ToString;
import oap.template.runtime.RuntimeContext;

import java.util.ArrayList;
import java.util.Map;

@ToString
public class AstRendererDynamicMap extends AstRender {
    private final ArrayList<String> path = new ArrayList<>();

    public AstRendererDynamicMap() {
        super( new TemplateType( Object.class ) );
    }

    @Override
    public void render( Render render ) {
        Render r = render;
        for( int i = 0; i < path.size(); i++ ) {
            String item = path.get( i );
            if( i == 0 ) {
                r = r.ntab()
                    .append( "Object obj = ( ( Map )%s ).get( \"%s\" );", render.field, item )
                    .withField( "obj" );
            } else {
                r = render.ntab()
                    .append( "if( obj instanceof Map ) {" )
                    .tabInc().ntab()
                    .append( "obj = ( ( Map )%s ).get( \"%s\");", r.field, item )
                    .ntab().append( "}" )
                    .tabDec();
            }
        }

        for( AstRender child : children ) {
            child.render( r.withField( "obj" ) );
        }
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        Object obj = ctx.currentObject;
        for( String key : path ) {
            if( obj instanceof Map<?, ?> m ) {
                obj = m.get( key );
            } else {
                obj = null;
                break;
            }
        }
        RuntimeContext nextCtx = ctx.withCurrentObject( obj );
        for( AstRender child : children ) child.interpret( nextCtx );
    }

    public void addPath( String key ) {
        this.path.add( key );
    }

    public boolean containsNestedFields() {
        return !path.isEmpty();
    }
}
