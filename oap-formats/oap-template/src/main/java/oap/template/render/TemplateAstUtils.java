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
import oap.template.tree.AndConditionExpr;
import oap.template.tree.BlockIfElement;
import oap.template.tree.BlockRangeElement;
import oap.template.tree.BlockWithElement;
import oap.template.tree.CompareConditionExpr;
import oap.template.tree.ConditionExpr;
import oap.template.tree.Element;
import oap.template.tree.Elements;
import oap.template.tree.Expr;
import oap.template.tree.Expression;
import oap.template.tree.ExpressionElement;
import oap.template.tree.Exprs;
import oap.template.tree.FieldConditionExpr;
import oap.template.tree.Func;
import oap.template.tree.IfCondition;
import oap.template.tree.LiteralCompareValue;
import oap.template.tree.NotConditionExpr;
import oap.template.tree.OrConditionExpr;
import oap.template.tree.TextElement;
import oap.template.tree.WithCondition;
import oap.util.Arrays;
import oap.util.Lists;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oap.template.ErrorStrategy.IGNORE;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

@Slf4j
public class TemplateAstUtils {
    @SuppressWarnings( "checkstyle:ModifiedControlVariable" )
    public static TemplateType findExpressionResultType( TemplateType rootTemplateType, Exprs exprs, ErrorStrategy errorStrategy ) {
        return findExpressionResultType( rootTemplateType, exprs, errorStrategy, Map.of() );
    }

    @SuppressWarnings( "checkstyle:ModifiedControlVariable" )
    static TemplateType findExpressionResultType( TemplateType rootTemplateType, Exprs exprs, ErrorStrategy errorStrategy,
                                                  Map<String, TemplateType> rangeVarTypes ) {
        if( exprs.concatenation != null ) return new TemplateType( String.class, false );

        if( exprs.varName != null ) {
            TemplateType varType = rangeVarTypes.get( exprs.varName );
            if( varType == null ) return new TemplateType( Object.class, true );
            if( exprs.exprs.isEmpty() ) return varType;
            return findExpressionResultType( varType, new Exprs( exprs.exprs ), errorStrategy, Map.of() );
        }

        try {
            TemplateType currentType = rootTemplateType;
            ArrayList<Expr> exprArrayList = exprs.exprs;

            boolean dynamicReflection = false;

            for( int i = 0; i < exprArrayList.size(); i++ ) {
                Expr expr = exprArrayList.get( i );
                if( currentType.isInstanceOf( Optional.class ) ) {
                    currentType = currentType.getActualTypeArguments0();
                    i--;

                } else if( currentType.isInstanceOf( Map.class ) ) {
                    currentType = currentType.getActualTypeArguments1();
                    dynamicReflection = true;
                } else if( !expr.method ) {
                    Class<?> parentClass = currentType.getTypeClass();
                    if( Object.class.equals( parentClass ) && dynamicReflection ) {
                        return new TemplateType( Object.class, true );
                    } else {
                        Field field = findField( parentClass, expr.name );

                        TemplateType fieldType = new TemplateType( field.getGenericType(), field.isAnnotationPresent( Nullable.class ) );
                        if( fieldType.isInstanceOf( Ext.class ) ) {
                            Class<?> extClass = ExtDeserializer.extensionOf( parentClass, expr.name );
                            if( extClass != null ) {
                                fieldType = new TemplateType( extClass, fieldType.nullable );
                            }
                        }
                        currentType = fieldType;
                    }
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

            throw e;
        }
    }

    static AstRender toAst( Expression expression, TemplateType templateType, String castType, String defaultValue,
                            Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) throws ClassNotFoundException {
        return toAst( expression, templateType, templateType, castType, defaultValue, builtInFunction, errorStrategy, Map.of() );
    }

    static AstRender toAst( Expression expression, TemplateType templateType, TemplateType rootTemplateType, String castType, String defaultValue,
                            Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) throws ClassNotFoundException {
        return toAst( expression, templateType, rootTemplateType, castType, defaultValue, builtInFunction, errorStrategy, Map.of() );
    }

    private static AstRender toAst( Expression expression, TemplateType templateType, TemplateType rootTemplateType, String castType, String defaultValue,
                                    Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy,
                                    Map<String, TemplateType> rangeVarTypes ) throws ClassNotFoundException {
        ArrayList<AstRender> orAst = new ArrayList<AstRender>();

        TemplateType lastTemplateType = null;

        for( int i = 0; i < expression.or.size(); i++ ) {
            Exprs item = expression.or.get( i );

            TemplateType effectiveType = item.rootScoped ? rootTemplateType
                : ( item.varName != null && rangeVarTypes.containsKey( item.varName ) )
                    ? rangeVarTypes.get( item.varName )
                    : templateType;
            TemplateType expressionResultType = TemplateAstUtils.findExpressionResultType( effectiveType, item, errorStrategy, rangeVarTypes );

            ErrorStrategy itemErrorStrategy = i < expression.or.size() - 1 ? IGNORE : errorStrategy;
            AstRender itemAst = TemplateAstUtils.toAst( item,
                expression.or.size() == 1 ? expression.function : null,
                effectiveType, rootTemplateType, expressionResultType, castType, defaultValue, builtInFunction, itemErrorStrategy, rangeVarTypes );
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
                astRenderFunction.addChild( new AstRenderPrintField( templateType, castFieldType ) );
            } else
                ast.addChild( new AstRenderPrintField( templateType, null ) );
            list.add( ast );
        } else if( !orAst.isEmpty() ) {
            list.add( orAst.getFirst() );
        }

        AstRender mainAst = list.head();

        IfCondition ifCondition = expression.ifCondition;
        if( ifCondition != null ) {
            TemplateType expressionResultType = TemplateAstUtils.findExpressionResultType( templateType, ifCondition.thenCode, errorStrategy );

            AstRender conditionAst = toConditionAst( ifCondition.condition, templateType, errorStrategy );
            AstRender thenCode = toAst( ifCondition.thenCode, null, templateType, rootTemplateType, expressionResultType, castType, defaultValue, builtInFunction, errorStrategy );
            AstRender elseCode = ifCondition.elseCode != null
                ? toAst( ifCondition.elseCode, null, templateType, rootTemplateType, expressionResultType, castType, defaultValue, builtInFunction, errorStrategy )
                : null;

            return new AstRenderBooleanIf( templateType, conditionAst, thenCode, elseCode );
        }

        WithCondition withCondition = expression.withCondition;
        if( withCondition != null ) {
            ArrayList<Exprs> expandedOr = new ArrayList<>();
            for( Exprs bodyExprs : withCondition.body ) {
                if( bodyExprs.rootScoped ) {
                    expandedOr.add( bodyExprs );
                } else {
                    Exprs merged = new Exprs();
                    merged.exprs.addAll( withCondition.scopePath.exprs );
                    merged.exprs.addAll( bodyExprs.exprs );
                    merged.math = bodyExprs.math;
                    merged.concatenation = bodyExprs.concatenation;
                    expandedOr.add( merged );
                }
            }
            Expression expandedExpression = new Expression( expression.comment, expression.castType, null, null,
                expandedOr, expression.defaultValue, expression.function );
            return toAst( expandedExpression, templateType, rootTemplateType, castType, defaultValue, builtInFunction, errorStrategy, rangeVarTypes );
        }

        return mainAst;

    }

    private static AstRender toConditionAst( ConditionExpr cond, TemplateType templateType, ErrorStrategy errorStrategy ) {
        if( cond instanceof FieldConditionExpr f ) {
            return toConditionAst( f.fieldPath(), templateType, errorStrategy );
        } else if( cond instanceof AndConditionExpr a ) {
            return new AstRenderConditionAnd( templateType,
                toConditionAst( a.left(), templateType, errorStrategy ),
                toConditionAst( a.right(), templateType, errorStrategy ) );
        } else if( cond instanceof OrConditionExpr o ) {
            return new AstRenderConditionOr( templateType,
                toConditionAst( o.left(), templateType, errorStrategy ),
                toConditionAst( o.right(), templateType, errorStrategy ) );
        } else if( cond instanceof NotConditionExpr n ) {
            return new AstRenderConditionNot( templateType, toConditionAst( n.inner(), templateType, errorStrategy ) );
        } else if( cond instanceof CompareConditionExpr c ) {
            String literal = ( ( LiteralCompareValue ) c.right() ).value();
            return toCompareConditionAst( c.left(), c.op(), literal, templateType, errorStrategy );
        }
        throw new IllegalStateException( "Unknown ConditionExpr: " + cond );
    }

    @SuppressWarnings( { "checkstyle:ModifiedControlVariable", "checkstyle:ParameterAssignment" } )
    private static AstRender toConditionAst( Exprs conditionExprs, TemplateType templateType, ErrorStrategy errorStrategy ) {
        try {
            TemplateType currentTemplateType = templateType;
            Chain result = new Chain();

            for( int i = 0; i < conditionExprs.exprs.size(); i++ ) {
                Expr expr = conditionExprs.exprs.get( i );

                if( currentTemplateType.isInstanceOf( Optional.class ) ) {
                    TemplateType valueType = currentTemplateType.getActualTypeArguments0();
                    AstRenderOptional ast = new AstRenderOptional( valueType );
                    i--;
                    result.add( ast );
                    currentTemplateType = valueType;
                } else if( currentTemplateType.nullable ) {
                    TemplateType newType = new TemplateType( currentTemplateType.type, false );
                    AstRenderNullable ast = new AstRenderNullable( newType );
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
                    java.lang.reflect.Field field = findField( parentClass, expr.name );
                    boolean nullable = field.isAnnotationPresent( Nullable.class )
                        || !field.getType().isPrimitive() && !field.isAnnotationPresent( Nonnull.class );
                    TemplateType fieldType = new TemplateType( field.getGenericType(), nullable );
                    AstRenderField ast = new AstRenderField( field.getName(), fieldType, false, null );
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
                        || !method.getReturnType().isPrimitive() && !method.isAnnotationPresent( Nonnull.class );
                    TemplateType fieldType = new TemplateType( method.getGenericReturnType(), nullable );
                    AstRenderMethod ast = new AstRenderMethod( expr.name, fieldType, expr.arguments );
                    result.add( ast );
                    currentTemplateType = fieldType;
                }
            }

            AstRenderCaptureBoolean captureNode = new AstRenderCaptureBoolean( currentTemplateType );

            if( currentTemplateType.isOptional() ) {
                TemplateType actualType = currentTemplateType.getActualTypeArguments0();
                AstRenderOptional ast = new AstRenderOptional( actualType );
                ast.addChild( new AstRenderCaptureBoolean( actualType ) );
                result.add( ast );
            } else if( currentTemplateType.nullable ) {
                AstRenderNullable ast = new AstRenderNullable( currentTemplateType );
                ast.addChild( captureNode );
                result.add( ast );
            } else {
                result.add( captureNode );
            }

            return result.head();
        } catch( NoSuchFieldException | NoSuchMethodException e ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( e.getMessage(), e );
            return new AstRenderPathNotFound( e.getMessage() );
        }
    }

    @SuppressWarnings( { "checkstyle:ModifiedControlVariable", "checkstyle:ParameterAssignment" } )
    private static AstRender toCompareConditionAst( Exprs conditionExprs, String op, String literal,
                                                     TemplateType templateType, ErrorStrategy errorStrategy ) {
        try {
            TemplateType currentTemplateType = templateType;
            Chain result = new Chain();

            for( int i = 0; i < conditionExprs.exprs.size(); i++ ) {
                Expr expr = conditionExprs.exprs.get( i );

                if( currentTemplateType.isInstanceOf( Optional.class ) ) {
                    TemplateType valueType = currentTemplateType.getActualTypeArguments0();
                    AstRenderOptional ast = new AstRenderOptional( valueType );
                    i--;
                    result.add( ast );
                    currentTemplateType = valueType;
                } else if( currentTemplateType.nullable ) {
                    TemplateType newType = new TemplateType( currentTemplateType.type, false );
                    AstRenderNullable ast = new AstRenderNullable( newType );
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
                    java.lang.reflect.Field field = findField( parentClass, expr.name );
                    boolean nullable = field.isAnnotationPresent( Nullable.class )
                        || !field.getType().isPrimitive() && !field.isAnnotationPresent( Nonnull.class );
                    TemplateType fieldType = new TemplateType( field.getGenericType(), nullable );
                    AstRenderField ast = new AstRenderField( field.getName(), fieldType, false, null );
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
                        || !method.getReturnType().isPrimitive() && !method.isAnnotationPresent( Nonnull.class );
                    TemplateType fieldType = new TemplateType( method.getGenericReturnType(), nullable );
                    AstRenderMethod ast = new AstRenderMethod( expr.name, fieldType, expr.arguments );
                    result.add( ast );
                    currentTemplateType = fieldType;
                }
            }

            AstRenderCaptureCompare captureNode = new AstRenderCaptureCompare( currentTemplateType, op, literal );

            if( currentTemplateType.isOptional() ) {
                TemplateType actualType = currentTemplateType.getActualTypeArguments0();
                AstRenderOptional ast = new AstRenderOptional( actualType );
                ast.addChild( new AstRenderCaptureCompare( actualType, op, literal ) );
                result.add( ast );
            } else if( currentTemplateType.nullable ) {
                AstRenderNullable ast = new AstRenderNullable( currentTemplateType );
                ast.addChild( captureNode );
                result.add( ast );
            } else {
                result.add( captureNode );
            }

            return result.head();
        } catch( NoSuchFieldException | NoSuchMethodException e ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( e.getMessage(), e );
            return new AstRenderPathNotFound( e.getMessage() );
        }
    }

    private record ScopeAstResult( AstRender scopeAst, TemplateType scopeType ) {}

    @SuppressWarnings( { "checkstyle:ModifiedControlVariable", "checkstyle:ParameterAssignment" } )
    private static ScopeAstResult toScopeAst( Exprs scopeExprs, TemplateType templateType, ErrorStrategy errorStrategy ) {
        try {
            TemplateType currentTemplateType = templateType;
            Chain result = new Chain();

            for( int i = 0; i < scopeExprs.exprs.size(); i++ ) {
                Expr expr = scopeExprs.exprs.get( i );

                if( currentTemplateType.isInstanceOf( Optional.class ) ) {
                    TemplateType valueType = currentTemplateType.getActualTypeArguments0();
                    AstRenderOptional ast = new AstRenderOptional( valueType );
                    i--;
                    result.add( ast );
                    currentTemplateType = valueType;
                } else if( currentTemplateType.nullable ) {
                    TemplateType newType = new TemplateType( currentTemplateType.type, false );
                    AstRenderNullable ast = new AstRenderNullable( newType );
                    i--;
                    result.add( ast );
                    currentTemplateType = newType;
                } else if( !expr.method ) {
                    Class<?> parentClass = currentTemplateType.getTypeClass();
                    java.lang.reflect.Field field = findField( parentClass, expr.name );
                    boolean nullable = field.isAnnotationPresent( Nullable.class )
                        || !field.getType().isPrimitive() && !field.isAnnotationPresent( Nonnull.class );
                    TemplateType fieldType = new TemplateType( field.getGenericType(), nullable );
                    AstRenderField ast = new AstRenderField( field.getName(), fieldType, false, null );
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
                        || !method.getReturnType().isPrimitive() && !method.isAnnotationPresent( Nonnull.class );
                    TemplateType fieldType = new TemplateType( method.getGenericReturnType(), nullable );
                    AstRenderMethod ast = new AstRenderMethod( expr.name, fieldType, expr.arguments );
                    result.add( ast );
                    currentTemplateType = fieldType;
                }
            }

            TemplateType resolvedScopeType = currentTemplateType;
            if( resolvedScopeType.isOptional() ) resolvedScopeType = resolvedScopeType.getActualTypeArguments0();

            AstRenderCaptureScope captureNode = new AstRenderCaptureScope( resolvedScopeType );

            if( currentTemplateType.isOptional() ) {
                TemplateType actualType = currentTemplateType.getActualTypeArguments0();
                AstRenderOptional ast = new AstRenderOptional( actualType );
                ast.addChild( new AstRenderCaptureScope( actualType ) );
                result.add( ast );
            } else if( currentTemplateType.nullable ) {
                AstRenderNullable ast = new AstRenderNullable( currentTemplateType );
                ast.addChild( captureNode );
                result.add( ast );
            } else {
                result.add( captureNode );
            }

            return new ScopeAstResult( result.head(), resolvedScopeType );
        } catch( NoSuchFieldException | NoSuchMethodException e ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( e.getMessage(), e );
            return new ScopeAstResult( new AstRenderPathNotFound( e.getMessage() ), new TemplateType( Object.class, true ) );
        }
    }

    private static TemplateType findLastsTemplateType( AstRender astRender ) {
        if( astRender.children.isEmpty() ) return astRender.type;

        return findLastsTemplateType( astRender.children.get( 0 ) );
    }


    @SuppressWarnings( { "checkstyle:ModifiedControlVariable", "checkstyle:UnnecessaryParentheses", "checkstyle:OverloadMethodsDeclarationOrder", "checkstyle:ParameterAssignment" } )
    private static AstRender toAst( Exprs exprs, Func function, TemplateType templateType, TemplateType resultType,
                                    String castType, String defaultValue, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        return toAst( exprs, function, templateType, templateType, resultType, castType, defaultValue, builtInFunction, errorStrategy, Map.of() );
    }

    @SuppressWarnings( { "checkstyle:ModifiedControlVariable", "checkstyle:UnnecessaryParentheses", "checkstyle:OverloadMethodsDeclarationOrder", "checkstyle:ParameterAssignment" } )
    private static AstRender toAst( Exprs exprs, Func function, TemplateType templateType, TemplateType rootTemplateType, TemplateType resultType,
                                    String castType, String defaultValue, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        return toAst( exprs, function, templateType, rootTemplateType, resultType, castType, defaultValue, builtInFunction, errorStrategy, Map.of() );
    }

    @SuppressWarnings( { "checkstyle:ModifiedControlVariable", "checkstyle:UnnecessaryParentheses", "checkstyle:OverloadMethodsDeclarationOrder", "checkstyle:ParameterAssignment" } )
    private static AstRender toAst( Exprs exprs, Func function, TemplateType templateType, TemplateType rootTemplateType, TemplateType resultType,
                                    String castType, String defaultValue, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy,
                                    Map<String, TemplateType> rangeVarTypes ) {
        TemplateType currentTemplateType = templateType;
        Chain result = new Chain();

        if( exprs.varName != null ) {
            TemplateType varType = rangeVarTypes.getOrDefault( exprs.varName, new TemplateType( Object.class, false ) );
            result.add( new AstRenderVarRef( exprs.varName, varType ) );
            currentTemplateType = varType;
        } else if( exprs.rootScoped ) {
            result.add( new AstRenderSwitchToRoot( rootTemplateType ) );
        }

        try {
            FieldType castFieldType = castType != null ? FieldType.parse( castType ) : null;

            AstRendererDynamicMap astRendererDynamicMap = null;
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
                    astRendererDynamicMap = new AstRendererDynamicMap();
                } else if( !expr.method ) {
                    if( astRendererDynamicMap != null ) {
                        astRendererDynamicMap.addPath( expr.name );
                    } else {
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
                        AstRenderField ast = new AstRenderField( field.getName(), fieldType, forceCast,
                            i < exprs.exprs.size() - 1 || exprs.concatenation != null ? null : castFieldType );

                        result.add( ast );
                        currentTemplateType = fieldType;
                    }
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

            if( astRendererDynamicMap != null && astRendererDynamicMap.containsNestedFields() ) {
                result.add( astRendererDynamicMap );
                castFieldType = castType != null ? FieldType.parse( castType ) : new FieldType( Object.class );
                currentTemplateType = new TemplateType( currentTemplateType.type, true );
                resultType = new TemplateType( castFieldType.type );
            }

            if( currentTemplateType.isOptional() ) {
                TemplateType actualTypeArguments0 = currentTemplateType.getActualTypeArguments0();
                AstRenderOptional ast = new AstRenderOptional( actualTypeArguments0 );
                ast.addChild( wrap( exprs, function, actualTypeArguments0, resultType, defaultValue, builtInFunction, errorStrategy, castFieldType ) );
                ast.elseAstRender = new AstRenderPrintValue( resultType, defaultValue, castFieldType );
                result.add( ast );
            } else if( currentTemplateType.nullable ) {
                AstRenderNullable ast = new AstRenderNullable( currentTemplateType );
                ast.addChild( wrap( exprs, function, currentTemplateType, resultType, defaultValue, builtInFunction, errorStrategy, castFieldType ) );
                ast.elseAstRender = new AstRenderPrintValue( resultType, defaultValue, castFieldType );
                result.add( ast );
            } else
                result.add( wrap( exprs, function, currentTemplateType, resultType, defaultValue, builtInFunction, errorStrategy, castFieldType ) );

            return result.head();
        } catch( NoSuchFieldException | NoSuchMethodException | ClassNotFoundException e ) {
            if( errorStrategy == ErrorStrategy.ERROR ) throw new TemplateException( e.getMessage(), e );
            return new AstRenderPathNotFound( e.getMessage() );
        }
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private static AstRender wrap( Exprs exprs, Func function, TemplateType parentTemplateType, TemplateType resultType,
                                   String defaultValue, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy,
                                   FieldType castFieldType ) {
        Chain list = new Chain();

        if( exprs.concatenation != null ) {
            ArrayList<AstRender> items = new ArrayList<>();

            for( Object item : exprs.concatenation.items ) {
                if( item instanceof String si ) {
                    items.add( new AstRenderText( si ) );
                } else if( item instanceof Expr ei ) {
                    AstRender ast = toAst( new Exprs( List.of( ei ) ), function, parentTemplateType, resultType, null,
                        defaultValue, builtInFunction, errorStrategy );
                    items.add( ast );
                } else {
                    throw new TemplateException( "Unknown concatenation item " + item.getClass() );
                }
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

        AstRenderPrintField ast = new AstRenderPrintField( parentTemplateType, castFieldType );
        list.add( ast );


        return list.head();
    }

    @SuppressWarnings( "checkstyle:ModifiedControlVariable" )
    private static void applyWhitespaceTrim( List<Element> elements ) {
        for( int i = 0; i < elements.size(); i++ ) {
            Element el = elements.get( i );
            boolean ltrim = el instanceof ExpressionElement e && e.trimLeft
                || el instanceof BlockIfElement b && b.trimLeft;
            boolean rtrim = el instanceof ExpressionElement e3 && e3.trimRight;

            if( ltrim && i > 0 && elements.get( i - 1 ) instanceof TextElement prev ) {
                String trimmed = stripEnd( prev.text, " \t\r\n" );
                if( trimmed.isEmpty() ) {
                    elements.remove( i - 1 );
                    i--;
                } else {
                    elements.set( i - 1, new TextElement( trimmed ) );
                }
            }
            if( rtrim && i + 1 < elements.size() && elements.get( i + 1 ) instanceof TextElement next ) {
                String trimmed = stripStart( next.text, " \t\r\n" );
                if( trimmed.isEmpty() ) elements.remove( i + 1 );
                else elements.set( i + 1, new TextElement( trimmed ) );
            }
        }
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public static AstRenderRoot toAst( Elements elements, TemplateType templateType, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) {
        return toAst( elements, templateType, templateType, builtInFunction, errorStrategy, Map.of() );
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    private static AstRenderRoot toAst( Elements elements, TemplateType templateType, TemplateType rootTemplateType,
                                        Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy,
                                        Map<String, TemplateType> rangeVarTypes ) {
        applyWhitespaceTrim( elements.elements );
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
                    astRender.addChild( toAst( tree, templateType, rootTemplateType, tree.castType, tree.defaultValue, builtInFunction, errorStrategy, rangeVarTypes ) );
                } catch( Exception exp ) {
                    throw new TemplateException( e.expression + ": " + exp.getMessage(), exp );
                }
            } else if( element instanceof BlockIfElement b ) {
                try {
                    TemplateLexerExpression lexer = new TemplateLexerExpression( CharStreams.fromString( b.conditionPath ) );
                    TemplateGrammarExpression grammar = new TemplateGrammarExpression( new BufferedTokenStream( lexer ), builtInFunction, errorStrategy );
                    if( errorStrategy == ErrorStrategy.ERROR ) {
                        lexer.addErrorListener( ThrowingErrorListener.INSTANCE );
                        grammar.addErrorListener( ThrowingErrorListener.INSTANCE );
                    }
                    ConditionExpr conditionExpr = grammar.ifCondition().ret;
                    AstRender conditionAst = toConditionAst( conditionExpr, templateType, errorStrategy );
                    AstRenderRoot thenRoot = toAst( b.thenElements, templateType, rootTemplateType, builtInFunction, errorStrategy, rangeVarTypes );
                    AstRenderRoot elseRoot = b.elseElements != null
                        ? toAst( b.elseElements, templateType, rootTemplateType, builtInFunction, errorStrategy, rangeVarTypes )
                        : null;
                    astRender = new AstRenderBlockIf( templateType, conditionAst, thenRoot.children,
                        elseRoot != null ? elseRoot.children : null );
                } catch( Exception exp ) {
                    throw new TemplateException( b.conditionPath + ": " + exp.getMessage(), exp );
                }
            } else if( element instanceof BlockWithElement w ) {
                try {
                    TemplateLexerExpression lexer = new TemplateLexerExpression( CharStreams.fromString( w.scopePath ) );
                    TemplateGrammarExpression grammar = new TemplateGrammarExpression( new BufferedTokenStream( lexer ), builtInFunction, errorStrategy );
                    if( errorStrategy == ErrorStrategy.ERROR ) {
                        lexer.addErrorListener( ThrowingErrorListener.INSTANCE );
                        grammar.addErrorListener( ThrowingErrorListener.INSTANCE );
                    }
                    Exprs scopeExprs = grammar.exprs().ret;
                    ScopeAstResult scopeResult = toScopeAst( scopeExprs, templateType, errorStrategy );
                    AstRenderRoot bodyRoot = toAst( w.body, scopeResult.scopeType, rootTemplateType, builtInFunction, errorStrategy, rangeVarTypes );
                    astRender = new AstRenderBlockWith( templateType, scopeResult.scopeAst, scopeResult.scopeType, bodyRoot.children );
                } catch( Exception exp ) {
                    throw new TemplateException( w.scopePath + ": " + exp.getMessage(), exp );
                }
            } else if( element instanceof BlockRangeElement r ) {
                try {
                    astRender = buildRangeAst( r, templateType, rootTemplateType, builtInFunction, errorStrategy );
                } catch( Exception exp ) {
                    throw new TemplateException( r.rangeSpec + ": " + exp.getMessage(), exp );
                }
            } else {
                throw new TemplateException( "Unknown element " + element.getClass() );
            }
            astRoot.addChild( astRender );
        }
        return astRoot;
    }

    private static final Pattern STEP_PATTERN = Pattern.compile( "(?:^|\\s)step\\s" );

    private static AstRender buildRangeAst( BlockRangeElement r, TemplateType templateType, TemplateType rootTemplateType,
                                            Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy ) throws NoSuchFieldException {
        String spec = r.rangeSpec;
        int assignIdx = spec.indexOf( ":=" );

        if( assignIdx < 0 ) {
            // Implicit scope: "{{% range .list }}"
            String collPath = spec.startsWith( "." ) ? spec.substring( 1 ).trim() : spec.trim();
            return buildCollectionRange( collPath, List.of(), AstRenderBlockRange.Mode.IMPLICIT_SCOPE,
                templateType, rootTemplateType, r, builtInFunction, errorStrategy );
        }

        String varPart = spec.substring( 0, assignIdx ).trim();
        String sourcePart = spec.substring( assignIdx + 2 ).trim();

        List<String> varNames = new ArrayList<>();
        for( String v : varPart.split( "," ) ) {
            String vn = v.trim();
            if( vn.startsWith( "$" ) ) vn = vn.substring( 1 );
            varNames.add( vn );
        }

        if( sourcePart.contains( ".." ) ) {
            return buildIntervalRange( varNames, sourcePart, templateType, rootTemplateType, r, builtInFunction, errorStrategy );
        }

        String collPath = sourcePart.startsWith( "." ) ? sourcePart.substring( 1 ).trim() : sourcePart.trim();

        AstRenderBlockRange.Mode mode;
        if( varNames.size() == 2 ) {
            // Check if it's a map (key,value) or list (index,item) — determined later from collection type
            mode = AstRenderBlockRange.Mode.NAMED_INDEX_ITEM;
        } else {
            mode = AstRenderBlockRange.Mode.NAMED_ITEM;
        }

        return buildCollectionRange( collPath, varNames, mode, templateType, rootTemplateType, r, builtInFunction, errorStrategy );
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private static AstRender buildCollectionRange( String collPath, List<String> varNames, AstRenderBlockRange.Mode mode,
                                                   TemplateType templateType, TemplateType rootTemplateType,
                                                   BlockRangeElement r, Map<String, List<Method>> builtInFunction,
                                                   ErrorStrategy errorStrategy ) throws NoSuchFieldException {
        TemplateLexerExpression lexer = new TemplateLexerExpression( CharStreams.fromString( collPath ) );
        TemplateGrammarExpression grammar = new TemplateGrammarExpression( new BufferedTokenStream( lexer ), builtInFunction, errorStrategy );
        if( errorStrategy == ErrorStrategy.ERROR ) {
            lexer.addErrorListener( ThrowingErrorListener.INSTANCE );
            grammar.addErrorListener( ThrowingErrorListener.INSTANCE );
        }
        Exprs collExprs = grammar.exprs().ret;
        ScopeAstResult scopeResult = toScopeAst( collExprs, templateType, errorStrategy );
        TemplateType collectionType = scopeResult.scopeType;

        boolean isMap = collectionType.isInstanceOf( Map.class );

        TemplateType itemType;
        TemplateType keyType = null;
        TemplateType valueType = null;
        String itemVarName = null;
        String indexOrKeyVarName = null;

        Map<String, TemplateType> bodyRangeVarTypes = new HashMap<>();

        if( isMap ) {
            mode = AstRenderBlockRange.Mode.MAP_KEY_VALUE;
            keyType = collectionType.getActualTypeArguments0();
            valueType = collectionType.getActualTypeArguments1( false );
            itemType = valueType;
            if( varNames.size() >= 2 ) {
                indexOrKeyVarName = varNames.get( 0 );
                itemVarName = varNames.get( 1 );
                bodyRangeVarTypes.put( indexOrKeyVarName, keyType );
                bodyRangeVarTypes.put( itemVarName, valueType );
            }
        } else {
            itemType = collectionType.getActualTypeArguments0();
            if( mode == AstRenderBlockRange.Mode.NAMED_ITEM && !varNames.isEmpty() ) {
                itemVarName = varNames.get( 0 );
                bodyRangeVarTypes.put( itemVarName, itemType );
            } else if( mode == AstRenderBlockRange.Mode.NAMED_INDEX_ITEM && varNames.size() >= 2 ) {
                indexOrKeyVarName = varNames.get( 0 );
                itemVarName = varNames.get( 1 );
                bodyRangeVarTypes.put( indexOrKeyVarName, new TemplateType( int.class, false ) );
                bodyRangeVarTypes.put( itemVarName, itemType );
            }
        }

        TemplateType bodyTemplateType = mode == AstRenderBlockRange.Mode.IMPLICIT_SCOPE ? itemType : templateType;
        AstRenderRoot bodyRoot = toAst( r.body, bodyTemplateType, rootTemplateType, builtInFunction, errorStrategy, bodyRangeVarTypes );
        AstRenderRoot elseRoot = r.elseElements != null
            ? toAst( r.elseElements, templateType, rootTemplateType, builtInFunction, errorStrategy, Map.of() )
            : null;

        return new AstRenderBlockRange( templateType, scopeResult.scopeAst, collectionType,
            itemType, mode, itemVarName, indexOrKeyVarName, keyType, valueType,
            bodyRoot.children, elseRoot != null ? elseRoot.children : null );
    }

    private static AstRender buildIntervalRange( List<String> varNames, String sourcePart,
                                                 TemplateType templateType, TemplateType rootTemplateType,
                                                 BlockRangeElement r, Map<String, List<Method>> builtInFunction,
                                                 ErrorStrategy errorStrategy ) {
        int dotDotIdx = sourcePart.indexOf( ".." );
        String fromStr = sourcePart.substring( 0, dotDotIdx ).trim();
        String rest = sourcePart.substring( dotDotIdx + 2 ).trim();

        String toStr;
        String stepStr = "1";
        Matcher m = STEP_PATTERN.matcher( rest );
        if( m.find() ) {
            toStr = rest.substring( 0, m.start() ).trim();
            stepStr = rest.substring( m.end() ).trim();
        } else {
            toStr = rest.trim();
        }

        AstRenderBlockRangeInterval.IntRangeValue from = parseIntRangeValue( fromStr );
        AstRenderBlockRangeInterval.IntRangeValue to = parseIntRangeValue( toStr );
        AstRenderBlockRangeInterval.IntRangeValue step = parseIntRangeValue( stepStr );

        String varName = varNames.isEmpty() ? "_k" : varNames.get( 0 );
        Map<String, TemplateType> bodyRangeVarTypes = Map.of( varName, new TemplateType( int.class, false ) );
        AstRenderRoot bodyRoot = toAst( r.body, templateType, rootTemplateType, builtInFunction, errorStrategy, bodyRangeVarTypes );
        AstRenderRoot elseRoot = r.elseElements != null
            ? toAst( r.elseElements, templateType, rootTemplateType, builtInFunction, errorStrategy, Map.of() )
            : null;

        return new AstRenderBlockRangeInterval( templateType, varName, from, to, step,
            bodyRoot.children, elseRoot != null ? elseRoot.children : null );
    }

    private static AstRenderBlockRangeInterval.IntRangeValue parseIntRangeValue( String s ) {
        try {
            return new AstRenderBlockRangeInterval.IntRangeValue.Literal( Integer.parseInt( s ) );
        } catch( NumberFormatException e ) {
            return new AstRenderBlockRangeInterval.IntRangeValue.Field( s );
        }
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
