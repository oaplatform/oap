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

package oap.template;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.util.Dates;
import oap.util.Pair;

@Slf4j
@ToString( callSuper = true )
public class AstPrint extends Ast {
    final Pair<String, Class<?>> defaultValue;

    AstPrint( TemplateType type, Pair<String, Class<?>> defaultValue ) {
        super( type );
        this.defaultValue = defaultValue;
    }

    @Override
    void render( Render render ) {
        if( render.castType != null && !render.castType.isAssignableFrom( render.parentType ) ) {
            throw new ClassCastException( render.content + ": " + render.castType + " isAssignableFrom " + render.parentType.getTypeName() );
        }

        var r = render.ntab();
        var checkNull = defaultValue != null && !r.parentType.isPrimitiveType();
        if( checkNull ) {
            if( !Utils.canConvert( defaultValue._1, render.parentType.getTypeClass(), defaultValue._2 ) ) {
                throw new ClassCastException( render.content + ": " + render.parentType.getTypeName() + " instanceOf " + defaultValue._2.getTypeName() );
            }

            r = r
                .append( "if( %s == null ) {", r.field )
                .tabInc().ntab().append( "%s.accept( %s );", r.templateAccumulatorName, defaultValue._1 )
                .tabDec().ntab().append( "} else {" ).tabInc();
        }
        r.ntab().append( "%s.accept( %s );", r.templateAccumulatorName, r.field );

        if( checkNull ) r.tabDec().ntab().append( "}" );
    }

    private boolean checkDateTime() {
        try {
            if( defaultValue._2.equals( String.class ) ) {
                Dates.FORMAT_SIMPLE_CLEAN.parseDateTime( defaultValue._1.substring( 1, defaultValue._1.length() - 1 ) );
                return true;
            }

        } catch( IllegalArgumentException e ) {
            log.trace( e.getMessage(), e );
        }
        return false;
    }
}
