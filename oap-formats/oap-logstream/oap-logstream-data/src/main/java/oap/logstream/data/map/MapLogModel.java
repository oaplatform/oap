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

package oap.logstream.data.map;

import oap.dictionary.Dictionary;
import oap.dictionary.DictionaryRoot;
import oap.logstream.data.AbstractLogModel;
import oap.logstream.data.LogRenderer;
import oap.reflect.TypeRef;
import oap.template.TemplateAccumulatorString;
import oap.template.Types;
import org.apache.parquet.Preconditions;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class MapLogModel extends AbstractLogModel<String, StringBuilder, TemplateAccumulatorString> {
    public MapLogModel( @Nonnull DictionaryRoot model ) {
        super( model, new TemplateAccumulatorString() );
    }

    public MapLogRenderer renderer( String id, String tag ) {
        return renderer( new TypeRef<>() {}, id, tag );
    }

    @Override
    public <D, LD extends LogRenderer<D, String, StringBuilder, TemplateAccumulatorString>> LD renderer( TypeRef<D> typeRef, String id, String tag ) {
        return renderer( typeRef, new TemplateAccumulatorString(), id, tag );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <D, LD extends LogRenderer<D, String, StringBuilder, TemplateAccumulatorString>> LD renderer( TypeRef<D> typeRef, TemplateAccumulatorString accumulator, String id, String tag ) {
        Preconditions.checkArgument( typeRef.type().equals( new TypeRef<Map<String, Object>>() {}.type() ), "Map<String, Object>" );

        Dictionary dictionary = requireNonNull( this.model.getValue( id ), id + " not found" );
        var headers = new StringJoiner( "\t" );
        List<String> expressions = new ArrayList<>();
        headers.add( "TIMESTAMP" );
        for( Dictionary field : dictionary.getValues( d -> d.getTags().contains( tag ) ) ) {
            headers.add( field.getId() );
            expressions.add( field.<String>getProperty( "path" )
                .orElseThrow( () -> new IllegalArgumentException( "undefined property path for " + field.getId() ) ) );
        }
        return ( LD ) new MapLogRenderer( new String[] { headers.toString() }, new byte[][] { new byte[] { Types.RAW.id } }, expressions );
    }
}
