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

import java.util.List;

@ToString( callSuper = true )
public class AstRenderMethod extends AstRender {
    private final String methodName;
    private final List<String> arguments;

    public AstRenderMethod( String methodName, TemplateType methodType, List<String> arguments ) {
        super( methodType );

        this.methodName = methodName;
        this.arguments = arguments;
    }

    @Override
    public void render( Render render ) {
        var variableName = render.newVariable( methodName );

        if( variableName.isNew ) {
            render.ntab().append( "%s %s = %s.%s(%s);",
                type.getTypeName(), variableName.name,
                render.field, methodName, String.join( ",", arguments ) );
        }

        var newRender = render.withField( variableName.name ).withParentType( type );
        children.forEach( a -> a.render( newRender ) );
    }
}
