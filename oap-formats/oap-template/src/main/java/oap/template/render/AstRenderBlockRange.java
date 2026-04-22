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

package oap.template.render;

import lombok.ToString;
import oap.template.runtime.RuntimeContext;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Block range over a collection or map.
 * {{% range .list }} body {{% end %}}
 * {{% range $item := .list }} body {{% end %}}
 * {{% range $index,$item := .list }} body {{% end %}}
 * {{% range $key,$value := .map }} body {{% end %}}
 */
@ToString( callSuper = true )
public class AstRenderBlockRange extends AstRender {
    public enum Mode { IMPLICIT_SCOPE, NAMED_ITEM, NAMED_INDEX_ITEM, MAP_KEY_VALUE }

    private final AstRender collectionAst;
    private final TemplateType collectionType;
    private final TemplateType itemType;
    private final Mode mode;
    @Nullable private final String itemVarName;
    @Nullable private final String indexOrKeyVarName;
    @Nullable private final TemplateType keyType;
    @Nullable private final TemplateType valueType;
    private final List<AstRender> bodyChildren;
    @Nullable private final List<AstRender> elseChildren;

    public AstRenderBlockRange( TemplateType type, AstRender collectionAst, TemplateType collectionType,
                                TemplateType itemType, Mode mode,
                                @Nullable String itemVarName, @Nullable String indexOrKeyVarName,
                                @Nullable TemplateType keyType, @Nullable TemplateType valueType,
                                List<AstRender> bodyChildren, @Nullable List<AstRender> elseChildren ) {
        super( type );
        this.collectionAst = collectionAst;
        this.collectionType = collectionType;
        this.itemType = itemType;
        this.mode = mode;
        this.itemVarName = itemVarName;
        this.indexOrKeyVarName = indexOrKeyVarName;
        this.keyType = keyType;
        this.valueType = valueType;
        this.bodyChildren = bodyChildren;
        this.elseChildren = elseChildren;
    }

    @Override
    public void render( Render render ) {
        String collVar = render.newVariable();
        render.ntab().append( "%s %s = null;", collectionType.getTypeName(), collVar );
        collectionAst.render( render.withScopeVar( collVar ) );

        boolean isMap = mode == Mode.MAP_KEY_VALUE;
        String emptyCheck = collVar + " != null && !" + collVar + ".isEmpty()";
        render.ntab().append( "if( %s ) {", emptyCheck );

        Render outerRender = render.tabInc().newBlock();

        if( isMap ) {
            renderMap( render, outerRender, collVar );
        } else if( mode == Mode.IMPLICIT_SCOPE ) {
            renderImplicit( render, outerRender, collVar );
        } else if( mode == Mode.NAMED_INDEX_ITEM ) {
            renderIndexItem( render, outerRender, collVar );
        } else {
            renderNamedItem( render, outerRender, collVar );
        }

        render.ntab().append( "}" );

        if( elseChildren != null ) {
            render.append( " else {" );
            Render elseRender = render.tabInc().newBlock();
            for( AstRender child : elseChildren ) child.render( elseRender );
            render.ntab().append( "}" );
        }
    }

    private void renderImplicit( Render render, Render outerRender, String collVar ) {
        String itemVar = render.newVariable();
        outerRender.ntab().append( "for( %s %s : %s ) {", itemType.getTypeName(), itemVar, collVar );
        Render innerRender = outerRender.tabInc().newBlock();
        Render bodyRender = innerRender.withField( itemVar ).withParentType( itemType );
        for( AstRender child : bodyChildren ) child.render( bodyRender );
        outerRender.ntab().append( "}" );
    }

    private void renderNamedItem( Render render, Render outerRender, String collVar ) {
        String itemVar = render.newVariable();
        outerRender.ntab().append( "for( %s %s : %s ) {", itemType.getTypeName(), itemVar, collVar );
        Render innerRender = outerRender.tabInc().newBlock();
        Render bodyRender = innerRender.withRangeVar( itemVarName, itemVar );
        for( AstRender child : bodyChildren ) child.render( bodyRender );
        outerRender.ntab().append( "}" );
    }

    private void renderIndexItem( Render render, Render outerRender, String collVar ) {
        String idxVar = render.newVariable();
        outerRender.ntab().append( "int %s = 0;", idxVar );
        String itemVar = render.newVariable();
        outerRender.ntab().append( "for( %s %s : %s ) {", itemType.getTypeName(), itemVar, collVar );
        Render innerRender = outerRender.tabInc().newBlock();
        Render bodyRender = innerRender
            .withRangeVar( indexOrKeyVarName, idxVar )
            .withRangeVar( itemVarName, itemVar );
        for( AstRender child : bodyChildren ) child.render( bodyRender );
        innerRender.ntab().append( "%s++;", idxVar );
        outerRender.ntab().append( "}" );
    }

    private void renderMap( Render render, Render outerRender, String collVar ) {
        String entryVar = render.newVariable();
        String entryType = "Map.Entry<" + keyType.getTypeName() + "," + valueType.getTypeName() + ">";
        outerRender.ntab().append( "for( %s %s : %s.entrySet() ) {", entryType, entryVar, collVar );
        Render innerRender = outerRender.tabInc().newBlock();
        String keyVar = render.newVariable();
        String valueVar = render.newVariable();
        innerRender.ntab().append( "%s %s = %s.getKey();", keyType.getTypeName(), keyVar, entryVar );
        innerRender.ntab().append( "%s %s = %s.getValue();", valueType.getTypeName(), valueVar, entryVar );
        Render bodyRender = innerRender
            .withRangeVar( indexOrKeyVarName, keyVar )
            .withRangeVar( itemVarName, valueVar );
        for( AstRender child : bodyChildren ) child.render( bodyRender );
        outerRender.ntab().append( "}" );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void interpret( RuntimeContext ctx ) {
        Object[] capture = { null };
        collectionAst.interpret( ctx.withScopeCapture( capture ) );
        Object coll = capture[0];

        boolean nonEmpty = mode == Mode.MAP_KEY_VALUE
            ? ( coll instanceof Map<?, ?> m && !m.isEmpty() )
            : ( coll instanceof Collection<?> c && !c.isEmpty() );

        if( nonEmpty ) {
            if( mode == Mode.MAP_KEY_VALUE ) {
                interpretMap( ctx, ( Map<?, ?> ) coll );
            } else if( mode == Mode.IMPLICIT_SCOPE ) {
                interpretImplicit( ctx, ( Collection<?> ) coll );
            } else if( mode == Mode.NAMED_INDEX_ITEM ) {
                interpretIndexItem( ctx, ( Collection<?> ) coll );
            } else {
                interpretNamedItem( ctx, ( Collection<?> ) coll );
            }
        } else if( elseChildren != null ) {
            elseChildren.forEach( c -> c.interpret( ctx ) );
        }
    }

    private void interpretImplicit( RuntimeContext ctx, Collection<?> coll ) {
        for( Object item : coll ) {
            RuntimeContext inner = ctx.withCurrentObject( item );
            bodyChildren.forEach( c -> c.interpret( inner ) );
        }
    }

    private void interpretNamedItem( RuntimeContext ctx, Collection<?> coll ) {
        for( Object item : coll ) {
            RuntimeContext inner = ctx.withRangeVar( itemVarName, item );
            bodyChildren.forEach( c -> c.interpret( inner ) );
        }
    }

    private void interpretIndexItem( RuntimeContext ctx, Collection<?> coll ) {
        int idx = 0;
        for( Object item : coll ) {
            RuntimeContext inner = ctx.withRangeVar( indexOrKeyVarName, idx ).withRangeVar( itemVarName, item );
            bodyChildren.forEach( c -> c.interpret( inner ) );
            idx++;
        }
    }

    private void interpretMap( RuntimeContext ctx, Map<?, ?> map ) {
        for( Map.Entry<?, ?> entry : map.entrySet() ) {
            RuntimeContext inner = ctx
                .withRangeVar( indexOrKeyVarName, entry.getKey() )
                .withRangeVar( itemVarName, entry.getValue() );
            bodyChildren.forEach( c -> c.interpret( inner ) );
        }
    }
}
