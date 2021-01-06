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
import oap.json.ext.Ext;
import oap.json.ext.ExtDeserializer;
import oap.util.Lists;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

abstract class TemplateGrammarAdaptor extends Parser {
    Map<String, List<Method>> builtInFunction;
    ErrorStrategy errorStrategy;

    public TemplateGrammarAdaptor( TokenStream input ) {
        super( input );
    }

    MaxMin getAst( TemplateType parentType, String text, boolean isMethod ) {
        return getAst( parentType, text, isMethod, null );
    }

    String sStringToDString( String sstr ) {
        return '"' + sdStringToString( sstr ) + '"';
    }

    String sdStringToString( String sstr ) {
        return sstr.substring( 1, sstr.length() - 1 );
    }

    MaxMin getAst( TemplateType parentType, String text, boolean isMethod, String defaultValue ) {
        try {
            if( parentType.isInstanceOf( Optional.class ) ) {
                var valueType = parentType.getActualTypeArguments0();
                var child = getAst( valueType, text, isMethod );
                var top = new AstOptional( valueType );
                top.addChild( child.top );

                return new MaxMin( top, child.bottom );
            } else if( parentType.nullable ) {
                var newType = new TemplateType( parentType.type, false );
                var child = getAst( newType, text, isMethod );
                var top = new AstNullable( newType );
                top.addChild( child.top );

                return new MaxMin( top, child.bottom );
            } else if( text == null ) {
                return new MaxMin( new AstPrint( parentType, defaultValue ) );
            } else if( parentType.isInstanceOf( Map.class ) ) {
                var valueType = parentType.getActualTypeArguments1();
                return new MaxMin( new AstMap( text, valueType ) );
            } else if( !isMethod ) {
                var parentClass = parentType.getTypeClass();
                var field = parentClass.getField( text );

                var fieldType = new TemplateType( field.getGenericType(), field.isAnnotationPresent( Template.Nullable.class ) );
                boolean forceCast = false;
                if( fieldType.isInstanceOf( Ext.class ) ) {
                    var extClass = ExtDeserializer.extensionOf( parentClass, text );
                    fieldType = new TemplateType( extClass, fieldType.nullable );
                    forceCast = true;
                }
                return new MaxMin( new AstField( text, fieldType, forceCast ) );
            } else {
                var parentClass = parentType.getTypeClass();
                var method = parentClass.getMethod( text );

                return new MaxMin( new AstMethod( text, new TemplateType( method.getGenericReturnType(), method.isAnnotationPresent( Template.Nullable.class ) ) ) );
            }
        } catch( NoSuchFieldException | NoSuchMethodException e ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( e.getMessage() );
            return new MaxMin( new AstPathNotFound( e.getMessage() ) );
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

        return new AstFunction( new TemplateType( method.getGenericReturnType(), method.isAnnotationPresent( Template.Nullable.class ) ), method, args );
    }

    static class MaxMin {
        public Ast top;
        public Ast bottom;

        public MaxMin( Ast top, Ast bottom ) {
            this.top = top;
            this.bottom = bottom;
        }

        public MaxMin( Ast top ) {
            this( top, top );
        }

        public void setTop( Ast ast ) {
            this.bottom = this.top;
            this.top = ast;

            this.top.addChild( this.bottom );
        }

        public void addToBottomChildrenAndSet( Ast ast ) {
            this.bottom.addChild( ast );
            this.bottom = ast;
        }

        public void addToBottomChildrenAndSet( MaxMin mm ) {
            this.bottom.addChild( mm.top );
            this.bottom = mm.bottom;
        }

        public void addLeafs( Supplier<MaxMin> sup ) {
            addLeafs( bottom, sup );
        }

        private void addLeafs( Ast ast, Supplier<MaxMin> sup ) {
            if( ast.children.isEmpty() )
                ast.addChild( sup.get().top );
            else
                for( var child : ast.children ) {
                    addLeafs( child, sup );
                }
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
