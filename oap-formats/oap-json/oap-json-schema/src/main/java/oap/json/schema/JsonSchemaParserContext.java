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
package oap.json.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class JsonSchemaParserContext {
    public static final SchemaId ROOT_ID = new SchemaId( "", "", "" );
    public final Map<?, ?> node;
    public final String schemaType;
    public final Function<JsonSchemaParserContext, AbstractSchemaASTWrapper> mapParser;
    public final BiFunction<String, String, AbstractSchemaASTWrapper> urlParser;
    public final String rootPath;
    public final String path;
    public final HashMap<SchemaId, AbstractSchemaASTWrapper> astW;
    public final HashMap<SchemaId, AbstractSchemaAST> ast;
    public final SchemaStorage storage;
    private final String schemaName;

    public JsonSchemaParserContext(
        String schemaName,
        Map<?, ?> node,
        String schemaType,
        Function<JsonSchemaParserContext, AbstractSchemaASTWrapper> mapParser,
        BiFunction<String, String, AbstractSchemaASTWrapper> urlParser,
        String rootPath, String path,
        HashMap<SchemaId, AbstractSchemaASTWrapper> astW,
        HashMap<SchemaId, AbstractSchemaAST> ast,
        SchemaStorage storage ) {
        this.schemaName = schemaName;
        this.node = node;
        this.schemaType = schemaType;
        this.mapParser = mapParser;
        this.urlParser = urlParser;
        this.rootPath = rootPath;
        this.path = path;
        this.astW = astW;
        this.ast = ast;
        this.storage = storage;
    }

    public final JsonSchemaParserContext withNode( String field, Object mapObject ) {
        if( !( mapObject instanceof Map<?, ?> ) )
            throw new IllegalArgumentException( "object expected, but " + mapObject );
        Map<?, ?> map = ( Map<?, ?> ) mapObject;

        Object schemaType = map.get( "type" );

        if( schemaType instanceof String ) {
            return new JsonSchemaParserContext( schemaName, map,
                    ( String ) schemaType,
                    mapParser,
                    urlParser,
                    rootPath,
                    SchemaPath.resolve( path, field ),
                    astW,
                    ast,
                    storage );
        } else {
            throw new UnknownTypeValidationSyntaxException(
                "Unknown SchemaType type: " + ( schemaType == null ? "nothing" : schemaType.getClass() ) + " for field: [" + field + "] and map: " + map
            );
        }
    }

    public <T extends AbstractSchemaASTWrapper> T createWrapper( Function<SchemaId, T> creator ) {
        final T w = creator.apply( getId() );
        astW.put( w.id, w );
        return w;
    }

    public SchemaId getId() {
        return new SchemaId( schemaName, rootPath, path );
    }

    public AbstractSchemaASTWrapper getRoot() {
        final AbstractSchemaASTWrapper root = astW.get( ROOT_ID );
        if( root == null )
            throw new IllegalStateException( "root not found" );
        return root;
    }

    @SuppressWarnings( "unchecked" )
    public <T extends AbstractSchemaAST> T computeIfAbsent( SchemaId id, Supplier<T> s ) {
        AbstractSchemaAST r = ast.get( id );
        if( r == null ) {
            r = s.get();
            ast.put( id, r );
        }
        return ( T ) r;
    }

    public String error( String message ) {
        return ( path.isEmpty() ? "" : "/" + path + ": " ) + message;
    }
}
