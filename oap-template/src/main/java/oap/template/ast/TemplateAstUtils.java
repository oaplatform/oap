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

package oap.template.ast;

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

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
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
            var currentType = rootTemplateType;
            ArrayList<Expr> exprArrayList = exprs.exprs;
            for( int i = 0; i < exprArrayList.size(); i++ ) {
                Expr expr = exprArrayList.get( i );
                if( currentType.isInstanceOf( Optional.class ) ) {
                    currentType = currentType.getActualTypeArguments0();
                    i--;

                } else if( currentType.isInstanceOf( Map.class ) ) {
                    currentType = currentType.getActualTypeArguments1();
                } else if( !expr.method ) {
                    var parentClass = currentType.getTypeClass();
                    var field = findField( parentClass, expr.name );

                    var fieldType = new TemplateType( field.getGenericType(), field.isAnnotationPresent( Nullable.class ) );
                    if( fieldType.isInstanceOf( Ext.class ) ) {
                        var extClass = ExtDeserializer.extensionOf( parentClass, expr.name );
                        fieldType = new TemplateType( extClass, fieldType.nullable );
                    }
                    currentType = fieldType;
                } else {
                    var parentClass = currentType.getTypeClass();
                    var method = Arrays
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

            var fields = clazz.getFields();
            for( var field : fields ) {
                var jsonPropertyAnnotation = field.getAnnotation( JsonProperty.class );
                if( jsonPropertyAnnotation != null ) {
                    var jsonFieldName = jsonPropertyAnnotation.value();
                    if( fieldName.equals( jsonFieldName ) ) return field;
                }

                var jsonAliasAnnotation = field.getAnnotation( JsonAlias.class );
                if( jsonAliasAnnotation != null ) {
                    var jsonFieldNames = jsonAliasAnnotation.value();
                    for( var jsonFieldName : jsonFieldNames ) {
                        if( fieldName.equals( jsonFieldName ) ) return field;
                    }
                }
            }

            log.error( "class {} field {}", clazz, fieldName );
            e.printStackTrace();

            throw e;
        }
    }

    static Ast toAst( Expression expression, TemplateType templateType, String castType, String defaultValue,
                      Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        var orAst = new ArrayList<Ast>();

        for( int i = 0; i < expression.or.size(); i++ ) {
            Exprs item = expression.or.get( i );

            TemplateType expressionResultType = TemplateAstUtils.findExpressionResultType( templateType, item, errorStrategy );

            var itemErrorStrategy = i < expression.or.size() - 1 ? IGNORE : errorStrategy;
            var itemAst = TemplateAstUtils.toAst( item,
                expression.or.size() == 1 ? expression.function : null,
                templateType, expressionResultType, castType, defaultValue, builtInFunction, itemErrorStrategy );
            orAst.add( itemAst );
        }

        Ast ret;

        if( orAst.size() > 1 ) {
            ret = new AstOr( templateType, orAst );
            if( expression.function != null ) {
                Ast astFunction = getFunction( expression.function.name, expression.function.arguments, builtInFunction, errorStrategy );
                ret.addChild( astFunction );
                astFunction.addChild( new AstPrintField( templateType ) );
            } else
                ret.addChild( new AstPrintField( templateType ) );
        } else {
            ret = orAst.get( 0 );
        }

        return ret;

    }

    @SuppressWarnings( "checkstyle:ModifiedControlVariable" )
    private static Ast toAst( Exprs exprs, Func function, TemplateType templateType, TemplateType resultType,
                              String castType, String defaultValue, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        var currentTemplateType = templateType;
        LinkedList<Ast> result = new LinkedList<>();

        try {
            var castFieldType = TemplateType.FieldType.parse( castType != null ? castType : resultType.getTypeName() );

            for( int i = 0; i < exprs.exprs.size(); i++ ) {
                Expr expr = exprs.exprs.get( i );
                if( currentTemplateType.isInstanceOf( Optional.class ) ) {
                    var valueType = currentTemplateType.getActualTypeArguments0();
                    AstOptional ast = new AstOptional( valueType );
                    ast.elseAst = new AstPrintValue( resultType, defaultValue, castFieldType );

                    i--;

                    result.add( ast );
                    currentTemplateType = valueType;
                } else if( currentTemplateType.nullable ) {
                    var newType = new TemplateType( currentTemplateType.type, false );
                    AstNullable ast = new AstNullable( newType );
                    ast.elseAst = new AstPrintValue( resultType, defaultValue, castFieldType );

                    i--;

                    result.add( ast );
                    currentTemplateType = newType;
                } else if( currentTemplateType.isInstanceOf( Map.class ) ) {
                    var valueType = currentTemplateType.getActualTypeArguments1();
                    AstMap ast = new AstMap( expr.name, valueType );

                    result.add( ast );
                    currentTemplateType = valueType;
                } else if( !expr.method ) {
                    var parentClass = currentTemplateType.getTypeClass();
                    var field = findField( parentClass, expr.name );

                    var fieldType = new TemplateType( field.getGenericType(), field.isAnnotationPresent( Nullable.class ) );
                    boolean forceCast = false;
                    if( fieldType.isInstanceOf( Ext.class ) ) {
                        var extClass = ExtDeserializer.extensionOf( parentClass, expr.name );
                        fieldType = new TemplateType( extClass, fieldType.nullable );
                        forceCast = true;
                    }
                    AstField ast = new AstField( field.getName(), fieldType, forceCast,
                        i < exprs.exprs.size() - 1 || exprs.concatenation != null ? null : castFieldType );

                    result.add( ast );
                    currentTemplateType = fieldType;
                } else {
                    var parentClass = currentTemplateType.getTypeClass();
                    var method = Arrays
                        .find( c -> c.getName().equals( expr.name ), parentClass.getMethods() )
                        .orElse( null );
                    if( method == null )
                        method = parentClass.getMethod( expr.name );

                    TemplateType fieldType = new TemplateType( method.getGenericReturnType(), method.isAnnotationPresent( Nullable.class ) );
                    AstMethod ast = new AstMethod( expr.name, fieldType, expr.arguments );

                    result.add( ast );
                    currentTemplateType = fieldType;
                }

            }

            if( currentTemplateType.isOptional() ) {
                TemplateType actualTypeArguments0 = currentTemplateType.getActualTypeArguments0();
                AstOptional ast = new AstOptional( actualTypeArguments0 );
                ast.addChild( wrap( exprs, function, actualTypeArguments0, resultType, defaultValue, builtInFunction, errorStrategy ) );
                ast.elseAst = new AstPrintValue( actualTypeArguments0, defaultValue, castFieldType );
                result.add( ast );
            } else if( currentTemplateType.nullable || !currentTemplateType.isPrimitiveType() ) {
                AstNullable ast = new AstNullable( currentTemplateType );
                ast.addChild( wrap( exprs, function, currentTemplateType, resultType, defaultValue, builtInFunction, errorStrategy ) );
                ast.elseAst = new AstPrintValue( currentTemplateType, defaultValue, castFieldType );
                result.add( ast );
            } else
                result.add( wrap( exprs, function, currentTemplateType, resultType, defaultValue, builtInFunction, errorStrategy ) );

            var it = result.iterator();
            var current = it.next();
            while( it.hasNext() ) {
                var item = it.next();
                current.addChild( item );
                current = item;
            }

            return result.peekFirst();
        } catch( NoSuchFieldException | NoSuchMethodException | ClassNotFoundException e ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( e.getMessage(), e );
            return new AstPathNotFound( e.getMessage() );
        }
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private static Ast wrap( Exprs exprs, Func function, TemplateType parentTemplateType, TemplateType resultType,
                             String defaultValue, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        LinkedList<Ast> list = new LinkedList<>();

        if( exprs.concatenation != null ) {
            ArrayList<Ast> items = new ArrayList<>();

            for( var item : exprs.concatenation.items ) {
                if( item instanceof String si ) items.add( new AstText( si ) );
                else if( item instanceof Expr ei )
                    items.add( toAst( new Exprs( List.of( ei ) ), function, parentTemplateType, resultType, null,
                        defaultValue, builtInFunction, errorStrategy ) );
                else
                    throw new TemplateException( "Unknown concatenation item " + item.getClass() );
            }

            var ast = new AstConcatenation( parentTemplateType, items );
            list.add( ast );

            parentTemplateType = new TemplateType( String.class, false );
        }

        if( exprs.math != null ) {
            AstMath ast = new AstMath( parentTemplateType, exprs.math.operation, exprs.math.value );

            if( list.peekLast() != null ) list.peekLast().addChild( ast );
            else list.add( ast );
        }

        if( function != null ) {
            Ast ast = getFunction( function.name, function.arguments, builtInFunction, errorStrategy );

            if( list.peekLast() != null ) list.peekLast().addChild( ast );
            else list.add( ast );
        }

        var ast = new AstPrintField( new TemplateType( String.class, false ) );
        if( list.peekLast() != null ) list.peekLast().addChild( ast );
        else list.add( ast );


        return list.peekFirst();
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public static AstRoot toAst( Elements elements, TemplateType templateType, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        AstRoot astRoot = new AstRoot( templateType );
        for( var element : elements.elements ) {
            Ast ast;
            if( element instanceof TextElement t ) {
                ast = new AstText( t.text );
            } else if( element instanceof ExpressionElement e ) {
                try {
                    var lexer = new TemplateLexerExpression( CharStreams.fromString( e.expression ) );
                    var grammar = new TemplateGrammarExpression( new BufferedTokenStream( lexer ), builtInFunction, errorStrategy );
                    if( errorStrategy == ErrorStrategy.ERROR ) {
                        lexer.addErrorListener( ThrowingErrorListener.INSTANCE );
                        grammar.addErrorListener( ThrowingErrorListener.INSTANCE );
                    }
                    var tree = grammar.expression().ret;
                    log.trace( e.expression + "\n" + tree.print() );

                    ast = toAst( tree, templateType, tree.castType, tree.defaultValue, builtInFunction, errorStrategy );
                } catch( Exception exp ) {
                    throw new TemplateException( e.expression + ": " + exp.getMessage(), exp );
                }
            } else {
                throw new TemplateException( "Unknown element " + element.getClass() );
            }
            astRoot.addChild( ast );
        }
        return astRoot;
    }

    public static Ast getFunction( String name, List<String> args, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
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

        return new AstFunction( new TemplateType( method.getGenericReturnType(), method.isAnnotationPresent( Nullable.class ) ), method, args );
    }
}