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

import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Collection;

public class TemplateAccumulatorObject implements TemplateAccumulator<Object, MutableObject<Object>, TemplateAccumulatorObject> {
    private final MutableObject<Object> obj;

    public TemplateAccumulatorObject() {
        this( new MutableObject<>() );
    }

    public TemplateAccumulatorObject( MutableObject<Object> obj ) {
        this.obj = obj;
    }

    @Override
    public void acceptText( String text ) {
        accept( ( Object ) text );
    }

    @Override
    public void accept( String text ) {
        accept( ( Object ) text );
    }

    @Override
    public void accept( boolean b ) {
        accept( ( Object ) b );
    }

    @Override
    public void accept( char ch ) {
        accept( ( Object ) ch );
    }

    @Override
    public void accept( byte b ) {
        accept( ( Object ) b );
    }

    @Override
    public void accept( short s ) {
        accept( ( Object ) s );
    }

    @Override
    public void accept( int i ) {
        accept( ( Object ) i );
    }

    @Override
    public void accept( long l ) {
        accept( ( Object ) l );
    }

    @Override
    public void accept( float f ) {
        accept( ( Object ) f );
    }

    @Override
    public void accept( double d ) {
        accept( ( Object ) d );
    }

    @Override
    public void accept( Enum<?> e ) {
        accept( ( Object ) e );
    }

    @Override
    public void accept( Collection<?> list ) {
        accept( ( Object ) list );
    }

    @Override
    public void accept( TemplateAccumulatorObject acc ) {
        accept( acc.get() );
    }

    @Override
    public void accept( Object obj ) {
        this.obj.setValue( obj );
    }

    @Override
    public boolean isEmpty() {
        return obj.getValue() != null;
    }

    @Override
    public TemplateAccumulatorObject newInstance() {
        return new TemplateAccumulatorObject();
    }

    @Override
    public TemplateAccumulatorObject newInstance( MutableObject<Object> mutable ) {
        return new TemplateAccumulatorObject( mutable );
    }

    @Override
    public String getTypeName() {
        return "Object";
    }

    @Override
    public Object get() {
        return obj.getValue();
    }
}
