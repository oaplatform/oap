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

package oap.logstream;

import oap.template.Template;
import oap.template.TemplateStrategy;

import java.lang.reflect.Type;

/**
 * Created by igor.petrenko on 19.06.2019.
 */
public class ClickhouseLogStrategy implements TemplateStrategy<Template.Line> {
    @Override
    public StringBuilder mapBoolean( StringBuilder c, Type cc, Template.Line line, String field, boolean isJoin ) {
        return c.append( "acc.accept( " ).append( field ).append( " ? 1 : 0 );" );
    }

    @Override
    public void mapString( StringBuilder c, Type cc, Template.Line line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        escape( c, () -> fixUnknown( c, () -> c.append( field ) ) );
        c.append( " );" );
    }

    private void fixUnknown( StringBuilder c, Runnable run ) {
        c.append( "\"UNKNOWN\".equals( " );
        run.run();
        c.append( " ) ? \"\" : " );
        run.run();
    }

    @Override
    public void mapEnum( StringBuilder c, Type cc, Template.Line line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        fixUnknown( c, () -> c.append( field ).append( ".name()" ) );
        c.append( " );" );
    }

    @Override
    public String pathNotFound( String path ) {
        throw new IllegalStateException( "path " + path + " not found." );
    }
}
