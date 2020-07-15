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
import oap.util.Lists;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.TokenStream;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by igor.petrenko on 2020-07-14.
 */
abstract class TemplateGrammarAdaptor extends Parser {
    Map<String, List<Method>> builtInFunction;
    ErrorStrategy errorStrategy;

    public TemplateGrammarAdaptor( TokenStream input ) {
        super( input );
    }

    MinMax getAst( TemplateType parentType, String text, boolean isMethod ) {
        return getAst( parentType, text, isMethod, null );
    }

    String sStringToDString( String sstr ) {
        return '"' + sstr.substring( 1, sstr.length() - 1 ) + '"';
    }

    MinMax getAst( TemplateType parentType, String text, boolean isMethod, String defaultValue ) {
        try {
            if( parentType.isInstanceOf( Optional.class ) ) {
                var valueType = parentType.getActualTypeArguments0();
                var child = getAst( valueType, text, isMethod );
                var top = new AstOptional( valueType );
                top.addChild( child.top );

                return new MinMax( top, child.bottom );
            } else if( parentType.nullable ) {
                var newType = new TemplateType( parentType.type, false );
                var child = getAst( newType, text, isMethod );
                var top = new AstNullable( newType );
                top.addChild( child.top );

                return new MinMax( top, child.bottom );
            } else if( text == null ) {
                return new MinMax( new AstPrint( parentType, defaultValue ) );
            } else if( parentType.isInstanceOf( Map.class ) ) {
                var valueType = parentType.getActualTypeArguments1();
                return new MinMax( new AstMap( text, valueType ) );
            } else if( !isMethod ) {
                var parentClass = parentType.getTypeClass();
                var field = parentClass.getField( text );

                return new MinMax( new AstField( text, new TemplateType( field.getGenericType(), field.isAnnotationPresent( Template2.Nullable.class ) ) ) );
            } else {
                var parentClass = parentType.getTypeClass();
                var method = parentClass.getMethod( text );

                return new MinMax( new AstMethod( text, new TemplateType( method.getGenericReturnType(), method.isAnnotationPresent( Template2.Nullable.class ) ) ) );
            }
        } catch( NoSuchFieldException | NoSuchMethodException e ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( e.getMessage() );
            return new MinMax( new AstPathNotFound( e.getMessage() ) );
        }
    }

    public Ast getFunction( String name, List<String> args ) {
        var list = builtInFunction.get( name );
        if( list == null ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( "function " + name + "(" + String.join( ", ", args ) + ") not found" );
            return new AstPathNotFound( "function " + name + "(" + String.join( ", ", args ) + ") not found" );
        }

        var method = Lists.find2( list, m -> m.getParameters().length == args.size() + 1 );
        if( method == null ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( "function " + name + "(" + String.join( ", ", args ) + ") not found" );
            return new AstPathNotFound( "function " + name + "(" + String.join( ", ", args ) + ") not found" );
        }

        return new AstFunction( new TemplateType( method.getGenericReturnType(), method.isAnnotationPresent( Template2.Nullable.class ) ), method, args );
    }

    static class MinMax {
        public final Ast top;
        public Ast bottom;

        public MinMax( Ast top, Ast bottom ) {
            this.top = top;
            this.bottom = bottom;
        }

        public MinMax( Ast top ) {
            this( top, top );
        }

        public void addToBottomChildrenAndSet( Ast ast ) {
            this.bottom.addChild( ast );
            this.bottom = ast;
        }

        public void addToBottomChildrenAndSet( MinMax mm ) {
            this.bottom.addChild( mm.top );
            this.bottom = mm.bottom;
        }
    }

    @ToString
    static class Function {
        public final String name;

        public Function( String name ) {
            this.name = name;
        }
    }
}
