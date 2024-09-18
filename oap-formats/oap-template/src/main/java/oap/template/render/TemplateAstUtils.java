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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import oap.json.ext.Ext;
import oap.json.ext.ExtDeserializer;
import oap.template.ErrorStrategy;
import oap.template.TemplateException;
import oap.template.TemplateGrammarExpression;
import oap.template.TemplateLexerExpression;
import oap.template.ThrowingErrorListener;
import oap.template.tree.Element;
import oap.template.tree.Elements;
import oap.template.tree.Expr;
import oap.template.tree.Expression;
import oap.template.tree.ExpressionElement;
import oap.template.tree.Exprs;
import oap.template.tree.Func;
import oap.template.tree.TextElement;
import oap.util.Arrays;
import oap.util.Lists;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.template.ErrorStrategy.IGNORE;

@Slf4j
public class TemplateAstUtils {
    @SuppressWarnings( "checkstyle:ModifiedControlVariable" )
    public static TemplateType findExpressionResultType( TemplateType rootTemplateType, Exprs exprs, ErrorStrategy errorStrategy ) {
        if( exprs.concatenation != null ) return new TemplateType( String.class, false );

        try {
            TemplateType currentType = rootTemplateType;
            ArrayList<Expr> exprArrayList = exprs.exprs;
            for( int i = 0; i < exprArrayList.size(); i++ ) {
                Expr expr = exprArrayList.get( i );
                if( currentType.isInstanceOf( Optional.class ) ) {
                    currentType = currentType.getActualTypeArguments0();
                    i--;

                } else if( currentType.isInstanceOf( Map.class ) ) {
                    currentType = currentType.getActualTypeArguments1();
                } else if( !expr.method ) {
                    Class<?> parentClass = currentType.getTypeClass();
                    Field field = findField( parentClass, expr.name );

                    TemplateType fieldType = new TemplateType( field.getGenericType(), field.isAnnotationPresent( Nullable.class ) );
                    if( fieldType.isInstanceOf( Ext.class ) ) {
                        Class<?> extClass = ExtDeserializer.extensionOf( parentClass, expr.name );
                        if( extClass != null ) {
                            fieldType = new TemplateType( extClass, fieldType.nullable );
                        }
                    }
                    currentType = fieldType;
                } else {
                    Class<?> parentClass = currentType.getTypeClass();
                    Method method = Arrays
                        .find( c -> c.getName().equals( expr.name ), parentClass.getMethods() )
                        .orElse( null );
                    if( method == null )
                        method = parentClass.getMethod( expr.name );

                    currentType = new TemplateType( method.getGenericReturnType(), method.isAnnotationPresent( Nullable.class ) );
                }
            }

            if( currentType.isOptional() ) currentType = currentType.getActualTypeArguments0();

            return currentType;
        } catch( NoSuchFieldException | NoSuchMethodException e ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( e.getMessage(), e );
            return new TemplateType( String.class, false );
        }
    }

    static Field findField( Class<?> clazz, String fieldName ) throws NoSuchFieldException {
        try {
            return clazz.getField( fieldName );
        } catch( NoSuchFieldException e ) {

            Field[] fields = clazz.getFields();
            for( Field field : fields ) {
                JsonProperty jsonPropertyAnnotation = field.getAnnotation( JsonProperty.class );
                if( jsonPropertyAnnotation != null ) {
                    String jsonFieldName = jsonPropertyAnnotation.value();
                    if( fieldName.equals( jsonFieldName ) ) return field;
                }

                JsonAlias jsonAliasAnnotation = field.getAnnotation( JsonAlias.class );
                if( jsonAliasAnnotation != null ) {
                    String[] jsonFieldNames = jsonAliasAnnotation.value();
                    for( String jsonFieldName : jsonFieldNames ) {
                        if( fieldName.equals( jsonFieldName ) ) return field;
                    }
                }
            }

            log.error( "class {} field {}", clazz, fieldName );
            e.printStackTrace();

            throw e;
        }
    }

    static AstRender toAst( Expression expression, TemplateType templateType, String castType, String defaultValue,
                            Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) throws ClassNotFoundException {
        ArrayList<AstRender> orAst = new ArrayList<AstRender>();

        TemplateType lastTemplateType = null;

        for( int i = 0; i < expression.or.size(); i++ ) {
            Exprs item = expression.or.get( i );

            TemplateType expressionResultType = TemplateAstUtils.findExpressionResultType( templateType, item, errorStrategy );

            ErrorStrategy itemErrorStrategy = i < expression.or.size() - 1 ? IGNORE : errorStrategy;
            AstRender itemAst = TemplateAstUtils.toAst( item,
                expression.or.size() == 1 ? expression.function : null,
                templateType, expressionResultType, castType, defaultValue, builtInFunction, itemErrorStrategy );
            orAst.add( itemAst );

            TemplateType itemTemplateType = findLastsTemplateType( itemAst );
            if( lastTemplateType != null && !lastTemplateType.equals( itemTemplateType ) ) {
                throw new TemplateException( "last " + lastTemplateType + " current " + itemTemplateType );
            }

            lastTemplateType = itemTemplateType;
        }

        Chain list = new Chain();

        if( expression.comment != null ) list.add( new AstRenderComment( templateType, expression.comment ) );

        if( orAst.size() > 1 ) {
            FieldType castFieldType = FieldType.parse( castType != null ? castType : lastTemplateType.getTypeName() );

            AstRenderOr ast = new AstRenderOr( templateType, orAst );
            ast.elseAstRender = new AstRenderPrintValue( lastTemplateType, defaultValue, castFieldType );
            if( expression.function != null ) {
                AstRender astRenderFunction = getFunction( expression.function.name, expression.function.arguments, builtInFunction, errorStrategy );
                ast.addChild( astRenderFunction );
                astRenderFunction.addChild( new AstRenderPrintField( templateType ) );
            } else
                ast.addChild( new AstRenderPrintField( templateType ) );
            list.add( ast );
        } else {
            list.add( orAst.get( 0 ) );
        }

        return list.head();

    }

    private static TemplateType findLastsTemplateType( AstRender astRender ) {
        if( astRender.children.isEmpty() ) return astRender.type;

        return findLastsTemplateType( astRender.children.get( 0 ) );
    }


    @SuppressWarnings( { "checkstyle:ModifiedControlVariable", "checkstyle:UnnecessaryParentheses", "checkstyle:OverloadMethodsDeclarationOrder" } )
    private static AstRender toAst( Exprs exprs, Func function, TemplateType templateType, TemplateType resultType,
                                    String castType, String defaultValue, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        TemplateType currentTemplateType = templateType;
        Chain result = new Chain();

        try {
            FieldType castFieldType = FieldType.parse( castType != null ? castType : resultType.getTypeName() );

            for( int i = 0; i < exprs.exprs.size(); i++ ) {
                Expr expr = exprs.exprs.get( i );
                if( currentTemplateType.isInstanceOf( Optional.class ) ) {
                    TemplateType valueType = currentTemplateType.getActualTypeArguments0();
                    AstRenderOptional ast = new AstRenderOptional( valueType );
                    ast.elseAstRender = new AstRenderPrintValue( resultType, defaultValue, castFieldType );

                    i--;

                    result.add( ast );
                    currentTemplateType = valueType;
                } else if( currentTemplateType.nullable ) {
                    TemplateType newType = new TemplateType( currentTemplateType.type, false );
                    AstRenderNullable ast = new AstRenderNullable( newType );
                    ast.elseAstRender = new AstRenderPrintValue( resultType, defaultValue, castFieldType );

                    i--;

                    result.add( ast );
                    currentTemplateType = newType;
                } else if( currentTemplateType.isInstanceOf( Map.class ) ) {
                    TemplateType valueType = currentTemplateType.getActualTypeArguments1( true );
                    AstRenderMap ast = new AstRenderMap( expr.name, valueType );

                    result.add( ast );
                    currentTemplateType = valueType;
                } else if( !expr.method ) {
                    Class<?> parentClass = currentTemplateType.getTypeClass();
                    Field field = findField( parentClass, expr.name );

                    boolean nullable = field.isAnnotationPresent( Nullable.class )
                        || ( !field.getType().isPrimitive() && !field.isAnnotationPresent( Nonnull.class ) );
                    TemplateType fieldType = new TemplateType( field.getGenericType(), nullable );
                    boolean forceCast = false;
                    if( fieldType.isInstanceOf( Ext.class ) ) {
                        Class<?> extClass = ExtDeserializer.extensionOf( parentClass, expr.name );
                        if( extClass != null ) {
                            fieldType = new TemplateType( extClass, fieldType.nullable );
                            forceCast = true;
                        }
                    }

                    FieldType currentCastType = i < exprs.exprs.size() - 1 || exprs.concatenation != null ? null : castFieldType;
                    if( function != null ) {
                        AstRenderFunction render = ( AstRenderFunction ) getFunction( function.name, function.arguments, builtInFunction, errorStrategy );
                        Type parameterType = render.method.getGenericParameterTypes()[0];
                        if( parameterType instanceof ParameterizedType parameterizedType ) {
                            currentCastType = new FieldType( ( Class<?> ) parameterizedType.getRawType(), Lists.map( parameterizedType.getActualTypeArguments(), t -> new FieldType( ( Class<?> ) t ) ) );
                        } else {
                            currentCastType = new FieldType( ( Class<?> ) parameterType );
                        }
                    }
                    AstRenderField ast = new AstRenderField( field.getName(), fieldType, forceCast, currentCastType );

                    result.add( ast );
                    currentTemplateType = fieldType;
                } else {
                    Class<?> parentClass = currentTemplateType.getTypeClass();
                    Method method = Arrays
                        .find( c -> c.getName().equals( expr.name ), parentClass.getMethods() )
                        .orElse( null );
                    if( method == null )
                        method = parentClass.getMethod( expr.name );

                    boolean nullable = method.isAnnotationPresent( Nullable.class )
                        || ( !method.getReturnType().isPrimitive() && !method.isAnnotationPresent( Nonnull.class ) );
                    TemplateType fieldType = new TemplateType( method.getGenericReturnType(), nullable );
                    AstRenderMethod ast = new AstRenderMethod( expr.name, fieldType, expr.arguments );

                    result.add( ast );
                    currentTemplateType = fieldType;
                }
            }

            if( currentTemplateType.isOptional() ) {
                TemplateType actualTypeArguments0 = currentTemplateType.getActualTypeArguments0();
                AstRenderOptional ast = new AstRenderOptional( actualTypeArguments0 );
                ast.addChild( wrap( exprs, function, actualTypeArguments0, resultType, defaultValue, builtInFunction, errorStrategy ) );
                ast.elseAstRender = new AstRenderPrintValue( resultType, defaultValue, castFieldType );
                result.add( ast );
            } else if( currentTemplateType.nullable ) {
                AstRenderNullable ast = new AstRenderNullable( currentTemplateType );
                ast.addChild( wrap( exprs, function, currentTemplateType, resultType, defaultValue, builtInFunction, errorStrategy ) );
                ast.elseAstRender = new AstRenderPrintValue( resultType, defaultValue, castFieldType );
                result.add( ast );
            } else
                result.add( wrap( exprs, function, currentTemplateType, resultType, defaultValue, builtInFunction, errorStrategy ) );

            return result.head();
        } catch( NoSuchFieldException | NoSuchMethodException | ClassNotFoundException e ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( e.getMessage(), e );
            return new AstRenderPathNotFound( e.getMessage() );
        }
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private static AstRender wrap( Exprs exprs, Func function, TemplateType parentTemplateType, TemplateType resultType,
                                   String defaultValue, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        Chain list = new Chain();

        if( exprs.concatenation != null ) {
            ArrayList<AstRender> items = new ArrayList<>();

            for( Object item : exprs.concatenation.items ) {
                if( item instanceof String si ) items.add( new AstRenderText( si ) );
                else if( item instanceof Expr ei )
                    items.add( toAst( new Exprs( List.of( ei ) ), function, parentTemplateType, resultType, Object.class.getTypeName(),
                        defaultValue, builtInFunction, errorStrategy ) );
                else
                    throw new TemplateException( "Unknown concatenation item " + item.getClass() );
            }

            AstRenderConcatenation ast = new AstRenderConcatenation( parentTemplateType, items );
            list.add( ast );

            parentTemplateType = new TemplateType( String.class, false );
        }

        if( exprs.math != null ) {
            AstRenderMath ast = new AstRenderMath( parentTemplateType, exprs.math.operation, exprs.math.value );

            list.add( ast );
        }

        if( function != null ) {
            AstRender astRender = getFunction( function.name, function.arguments, builtInFunction, errorStrategy );

            list.add( astRender );
        }

        AstRenderPrintField ast = new AstRenderPrintField( parentTemplateType );
        list.add( ast );


        return list.head();
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public static AstRenderRoot toAst( Elements elements, TemplateType templateType, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        AstRenderRoot astRoot = new AstRenderRoot( templateType );
        for( Element element : elements.elements ) {
            AstRender astRender;
            if( element instanceof TextElement t ) {
                astRender = new AstRenderText( t.text );
            } else if( element instanceof ExpressionElement e ) {
                try {
                    TemplateLexerExpression lexer = new TemplateLexerExpression( CharStreams.fromString( e.expression ) );
                    TemplateGrammarExpression grammar = new TemplateGrammarExpression( new BufferedTokenStream( lexer ), builtInFunction, errorStrategy );
                    if( errorStrategy == ErrorStrategy.ERROR ) {
                        lexer.addErrorListener( ThrowingErrorListener.INSTANCE );
                        grammar.addErrorListener( ThrowingErrorListener.INSTANCE );
                    }
                    Expression tree = grammar.expression().ret;
                    log.trace( e.expression + "\n" + tree.print() );

                    astRender = new AstRenderComment( templateType, "// " + e.expression );
                    astRender.addChild( toAst( tree, templateType, tree.castType, tree.defaultValue, builtInFunction, errorStrategy ) );
                } catch( Exception exp ) {
                    throw new TemplateException( e.expression + ": " + exp.getMessage(), exp );
                }
            } else {
                throw new TemplateException( "Unknown element " + element.getClass() );
            }
            astRoot.addChild( astRender );
        }
        return astRoot;
    }

    public static AstRender getFunction( String name, List<String> args, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        List<Method> list = builtInFunction.get( name );
        if( list == null ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( "function " + name + "(" + String.join( ", ", args ) + ") not found" );
            return new AstRenderPathNotFound( "function " + name + "(" + String.join( ", ", args ) + ") not found" );
        }

        Method method = Lists.find2( list, m -> m.getParameters().length == args.size() + 1 );
        if( method == null ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( "function " + name + "(" + String.join( ", ", args ) + ") not found" );
            return new AstRenderPathNotFound( "function " + name + "(" + String.join( ", ", args ) + ") not found" );
        }

        return new AstRenderFunction( new TemplateType( method.getGenericReturnType(), method.isAnnotationPresent( Nullable.class ) ), method, args );
    }
}
