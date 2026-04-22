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
import oap.template.runtime.AcceptDispatch;
import oap.template.runtime.ReflectionCache;
import oap.template.runtime.RuntimeContext;

import java.lang.reflect.Method;
import java.util.List;

@ToString( callSuper = true )
public class AstRenderFunction extends AstRender {
    final Method method;
    final List<String> parameters;

    public AstRenderFunction( TemplateType type, Method method, List<String> parameters ) {
        super( type );
        this.method = method;
        this.parameters = parameters;
    }

    @Override
    public void render( Render render ) {
        String funcVariable = render.newVariable();

        String funcMethodVariable = render.field;

        if( method.getParameterTypes()[0].equals( String.class )
            && !render.parentType.getTypeClass().equals( String.class ) ) {
            String newTemplateAccName = render.newVariable();
            render
                .ntab().append( "%s %s = %s.newInstance();", render.templateAccumulator.getClass().getTypeName(), newTemplateAccName, render.templateAccumulatorName )
                .ntab().append( "%s.accept( %s );", newTemplateAccName, render.field );

            funcMethodVariable = newTemplateAccName + ".get()";
        }

        render.ntab().append( "%s %s = %s.%s( %s",
            method.getGenericReturnType().getTypeName(), funcVariable,
            method.getDeclaringClass().getName(), method.getName(), funcMethodVariable );

        if( !parameters.isEmpty() ) {
            render.append( ", " );
        }
        render.append( String.join( ", ", parameters ) ).append( " );" );

        Render newRender = render.withField( funcVariable ).withParentType( type );
        children.forEach( a -> a.render( newRender ) );
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object firstArg = ctx.currentObject;
        if( paramTypes.length > 0 && paramTypes[0].equals( String.class ) && !( firstArg instanceof String ) ) {
            firstArg = firstArg != null
                ? AcceptDispatch.toStringViaAcc( firstArg, new TemplateType( firstArg.getClass() ), ctx.acc )
                : null;
        }

        Object[] args = new Object[1 + parameters.size()];
        args[0] = firstArg;
        for( int i = 0; i < parameters.size(); i++ ) {
            args[i + 1] = ReflectionCache.parseArg( paramTypes[i + 1], parameters.get( i ) );
        }

        Object result;
        try {
            result = method.invoke( null, args );
        } catch( Exception e ) {
            throw new RuntimeException( e );
        }

        RuntimeContext nextCtx = ctx.withCurrentObject( result ).withAcc( ctx.acc );
        children.forEach( c -> c.interpret( nextCtx ) );
    }
}
