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
package oap.reflect;

import com.google.common.base.Joiner;
import com.google.common.base.Suppliers;
import com.google.common.reflect.TypeToken;
import oap.util.Arrays;
import oap.util.Lists;
import oap.util.Pair;
import oap.util.Stream;
import oap.util.function.Functions;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;
import static oap.util.Pair.__;

public class Reflection extends AbstractAnnotated<Class<?>> {
    public final Map<String, Field> fields = new LinkedHashMap<>();
    private final Coercions coercions;
    private final TypeToken<?> typeToken;
    public volatile List<Method> methods;
    public List<Reflection> typeParameters;
    public List<Constructor> constructors;

    Reflection( TypeToken<?> typeToken ) {
        this( typeToken, Coercions.basic().withIdentity() );
    }

    Reflection( TypeToken<?> typeToken, Coercions coercions ) {
        super( typeToken.getRawType() );
        this.coercions = coercions;
        this.typeToken = Objects.requireNonNull( typeToken );
    }

    static <A> List<A> declared( Class<?> clazz, Function<Class<?>, A[]> collector ) {
        var classes = Stream.<Class<?>>traverse( clazz, Class::getSuperclass ).toList();

        var declaredObjects = new ArrayList<A>();

        for( var c : classes ) {
            for( var i : c.getInterfaces() ) {
                Collections.addAll( declaredObjects, collector.apply( i ) );
            }
        }

        for( var c : classes ) {
            Collections.addAll( declaredObjects, collector.apply( c ) );
        }

        return declaredObjects;
    }

    private static void trySetAccessible( AccessibleObject ao ) {
        try {
            ao.trySetAccessible();
        } catch( SecurityException ignored ) {

        }
    }

   Reflection init() {
        if( this.methods == null ) {
            synchronized( this ) {
                if( this.methods == null ) {
                    this.methods = Lists.map( declared( typeToken.getRawType(), Class::getDeclaredMethods ), Method::new );

                    for( java.lang.reflect.Field field : declared( typeToken.getRawType(), Class::getDeclaredFields ) )
                        fields.put( field.getName(), new Field( field ) );

                    this.constructors = Stream.of( typeToken.getRawType().getDeclaredConstructors() )
                        .map( Constructor::new )
                        .sorted( Comparator.comparingInt( Constructor::parameterCount ).reversed() )
                        .toList();

                    this.typeParameters = Lists.map( typeToken.getRawType().getTypeParameters(), this::resolve );
                }
            }
        }
        return this;
    }

    public <T> T newInstance() {
        return newInstance( Map.of() );
    }

    public <T> T newInstance( Object... args ) {
        for( Constructor constructor : constructors )
            if( constructor.typeMatch( args ) ) return constructor.invoke( args );

        throw constructorNotFound( args );
    }

    public <T> T newInstance( Map<String, Object> args ) {
        for( Constructor constructor : constructors )
            if( constructor.nameMatch( args ) ) return constructor.invoke( args );

        throw constructorNotFound( args );
    }

    private ReflectException constructorNotFound( Object args ) {
        List<String> candidates = Stream.of( constructors ).map( Constructor::toString ).toList();

        return new ReflectException( underlying + ": cannot find matching constructor:\n" + args + "\n  candidates:\n"
            + String.join( "\n", candidates ) + "\n\nClasses must be compiled with '-parameters' option of javac." );
    }

    public boolean assignableTo( Class<?> clazz ) {
        return clazz.isAssignableFrom( this.underlying );
    }

    public Set<Class<?>> assignableTo() {
        return Stream.<Class<?>>flatTraverse( underlying, c -> {
            Stream<Class<?>> result = Stream.of( c.getInterfaces() );
            return c.getSuperclass() != null ? result.concat( c.getSuperclass() ) : result;
        } ).toSet();
    }

    public boolean assignableFrom( Class<?> clazz ) {
        return this.underlying.isAssignableFrom( clazz );
    }

    public boolean isEnum() {
        return this.typeToken.getRawType().isEnum();
    }

    public boolean isArray() {
        return this.typeToken.getRawType().isArray();
    }

    public boolean isOptional() {
        return Optional.class.equals( this.typeToken.getRawType() );
    }

    public Enum<?> enumValue( String value ) {
        return Arrays.find(
            constant -> Objects.equals( constant.name(), value ),
            ( Enum<?>[] ) this.typeToken.getRawType().getEnumConstants()
        ).orElseThrow( () -> new ReflectException( value + " is not a member of " + this ) );
    }

    //    todo cache all invokers of resolve (PERFORMANCE)
    Reflection resolve( Type type ) {
        return Reflect.reflect( typeToken.resolveType( type ) );
    }

    public Optional<Field> field( String name ) {
        return Optional.ofNullable( fields.get( name ) );
    }

    public Optional<Method> method( Predicate<Method> matcher ) {
        return this.methods.stream()
            .filter( matcher )
            .findFirst();
    }

    public Optional<Method> method( Predicate<Method> matcher, Comparator<Method> comparator ) {
        return this.methods.stream()
            .sorted( comparator )
            .filter( matcher )
            .findFirst();
    }

    /**
     * @param name       Method name
     * @param parameters list
     * @return {@link Method} - method wrapper of {@link java.lang.reflect.Method}
     */
    public Optional<Method> method( String name, List<Parameter> parameters ) {
        return method( m -> Objects.equals( m.name(), name ) && m.parameters.equals( parameters ) );
    }

    /**
     * @param name Method name
     * @return {@link Method} - method wrapper of {@link java.lang.reflect.Method}
     * Random is fine in most cases where oveerloading is not expected
     * Use {@link #method(String, List)} or {@link #method(java.lang.reflect.Method)} instead
     */
    public Optional<Method> method( String name ) {
        return method( m -> Objects.equals( m.name(), name ) );
    }

    public Optional<Method> method( java.lang.reflect.Method jmethod ) {
        return method( method ->
            Objects.equals( method.underlying.getName(), jmethod.getName() )
                && Objects.equals( method.underlying.getReturnType(), jmethod.getReturnType() )
                && method.underlying.getParameterCount() == jmethod.getParameterCount()
                && java.util.Arrays.equals( method.underlying.getParameterTypes(), jmethod.getParameterTypes() )
        );
    }

    public Type getType() {
        return typeToken.getType();
    }

    //    @todo check implementation via typetoken
    public Reflection getCollectionComponentType() {
        return assignableTo()
            .stream()
            .filter( i -> Collection.class.equals( i ) && i.getTypeParameters().length > 0 )
            .map( i -> resolve( i.getTypeParameters()[0] ) )
            .findAny()
            .orElse( null );
    }

    public Pair<Reflection, Reflection> getMapComponentsType() {
        return assignableTo()
            .stream()
            .filter( i -> Map.class.equals( i ) && i.getTypeParameters().length > 1 )
            .map( i -> __( resolve( i.getTypeParameters()[0] ), resolve( i.getTypeParameters()[1] ) ) )
            .findAny()
            .orElse( null );
    }

    @Override
    public String toString() {
        return "Reflection(" + typeToken + ")";
    }

    @Override
    public boolean equals( Object obj ) {
        return obj instanceof Reflection
            && this.typeToken.equals( ( ( Reflection ) obj ).typeToken );
    }

    @Override
    public int hashCode() {
        return this.typeToken.hashCode();
    }

    public String name() {
        return this.typeToken.getRawType().getCanonicalName();
    }

    public boolean isInterface() {
        return this.underlying.isInterface();
    }

    public boolean isPrimitive() {
        return this.underlying.isPrimitive();
    }

    public List<Field> annotatedFields( Class<? extends Annotation> annotation ) {
        return Stream.of( fields.values() )
            .filter( f -> f.isAnnotatedWith( annotation ) )
            .toList();
    }

    public List<Method> annotatedMethods( Class<? extends Annotation> annotation ) {
        return Stream.of( methods )
            .filter( m -> m.isAnnotatedWith( annotation ) )
            .toList();
    }

    public boolean implementationOf( Class<?> clazz ) {
        return this.typeToken.getTypes().interfaces().rawTypes().contains( clazz );
    }

    public class Field extends AbstractAnnotated<java.lang.reflect.Field> implements Comparable<Field> {
        private final Supplier<Reflection> type = Functions.memoize( () ->
            Reflect.reflect( typeToken.resolveType( this.underlying.getGenericType() ) ) );

        Field( java.lang.reflect.Field field ) {
            super( field );
            trySetAccessible( this.underlying );
        }

        public Object get( Object instance ) {
            try {
                return this.underlying.get( instance );
            } catch( ReflectiveOperationException e ) {
                throw new ReflectException( "Cannot invoke method 'get' "
                    + underlying.getName()
                    + " on instance of: " + instance.getClass().getCanonicalName(), e );
            }
        }

        public void set( Object instance, Object value ) {
            try {
                if( isFinal() && ( this.underlying.getType().isPrimitive() || this.underlying.getType().isAssignableFrom( String.class ) ) ) {
                    throw new IllegalAccessException( this + ": Constant Expressions. See: https://stackoverflow.com/questions/17506329/java-final-field-compile-time-constant-expression" );
                }
                this.underlying.set( instance, value );
            } catch( ReflectiveOperationException e ) {
                throw new ReflectException( "Cannot invoke method 'set' "
                        + underlying.getName()
                        + " on instance of: " + instance.getClass().getCanonicalName()
                        + " with value: " + argsToString( value ), e );
            }
        }

        public String name() {
            return underlying.getName();
        }


        public Reflection type() {
            return type.get();
        }

        @Override
        public int compareTo( @Nonnull Field o ) {
            return this.name().compareTo( o.name() );
        }

        public boolean isTransient() {
            return Modifier.isTransient( underlying.getModifiers() );
        }

        public boolean isStatic() {
            return Modifier.isStatic( underlying.getModifiers() );
        }

        public boolean isFinal() {
            return Modifier.isFinal( underlying.getModifiers() );
        }

        public boolean isSynthetic() {
            return underlying.isSynthetic();
        }

        public boolean isArray() {
            return underlying.getType().isArray();
        }
    }

    private String argsToString( Object... args ) {
        if ( args == null || args.length == 0 ) return "()";
        List<String> arguments = Stream.of( args ).map( arg -> arg.getClass().getCanonicalName() + "=" + arg ).toList();
        return "(" + Joiner.on( "," ).join( arguments ) + ")";
    }

    public class Method extends AbstractAnnotated<java.lang.reflect.Method> {
        public List<Parameter> parameters;
        private final Supplier<Reflection> returnType = Functions.memoize( () ->
            Reflect.reflect( typeToken.resolveType( this.underlying.getGenericReturnType() ) ) );

        Method( java.lang.reflect.Method method ) {
            super( method );
            trySetAccessible( this.underlying );
            this.parameters = Lists.map( method.getParameters(), Parameter::new );
        }

        public boolean hasParameter( String name ) {
            return Lists.contains( this.parameters, p -> Objects.equals( p.name(), name ) );
        }

        public Parameter getParameter( String name ) {
            return Lists.find2( parameters, p -> p.name().equals( name ) );
        }

        public String name() {
            return underlying.getName();
        }

        @SuppressWarnings( "unchecked" )
        public <T> T invoke( Object instance, Object... args ) throws ReflectException {
            try {
                return ( T ) underlying.invoke( instance, args );
            } catch( ReflectiveOperationException e ) {
                throw new ReflectException( "Cannot invoke method '"
                        + underlying.getName()
                        + "' on instance of class '" + instance.getClass().getCanonicalName()
                        + "' with parameters: " + argsToString( args ), e );
            }
        }

        public boolean isPublic() {
            return Modifier.isPublic( underlying.getModifiers() );
        }

        public boolean isVoid() {
            return underlying.getReturnType().equals( Void.TYPE );
        }

        public Reflection returnType() {
            return returnType.get();
        }
    }

    public class Constructor extends AbstractAnnotated<java.lang.reflect.Constructor<?>> {
        public final List<Parameter> parameters;
        private final Supplier<List<Reflection>> parameterTypes;
        private final Set<String> parameterNames;

        Constructor( java.lang.reflect.Constructor<?> constructor ) {
            super( constructor );
            trySetAccessible( this.underlying );
            this.parameters = Lists.map( constructor.getParameters(), Parameter::new );
            this.parameterNames = new LinkedHashSet<>( Lists.map( constructor.getParameters(), java.lang.reflect.Parameter::getName ) );
            this.parameterTypes = Suppliers.memoize( () -> Lists.map( parameters, Reflection.Parameter::type ) );
        }

        public boolean hasParameter( String name ) {
            return Lists.contains( this.parameters, p -> Objects.equals( p.name(), name ) );
        }

        public Parameter getParameter( String name ) {
            return Lists.find2( parameters, p -> p.name().equals( name ) );
        }

        public String name() {
            return underlying.getName();
        }

        @SuppressWarnings( "unchecked" )
        public <T> T invoke( Object... args ) throws ReflectException {
            try {
                return ( T ) underlying.newInstance( args );
            } catch( ReflectiveOperationException e ) {
                throw new ReflectException( "Cannot invoke constructor of class '"
                        + name()
                        + "' with constructor parameters: " + argsToString( args )
                        + "\nwhile expecting:\n" + parameterNames, e );
            }
        }

        public <T> T invoke( Map<String, Object> args ) throws ReflectException {
            //      @todo check match of parameter types
            Map<String, Object> extraParameters = new LinkedHashMap<>( args );
            try {
                //step 1: new instance
                Object[] cArgs = Stream.of( parameters )
                    .map( p -> coercions.cast( p.type(), args.get( p.name() ) ) )
                    .toArray();
                T instance = invoke( cArgs );
                //step 2: fill extra parameters
                for( String key : args.keySet() ) {
                    if( parameterNames.contains( key ) ) {
                        //skip constructor parameter
                        extraParameters.remove( key );
                        continue;
                    }
                    Optional<Field> f = field( key );
                    f.ifPresent( field -> {
                        Object arg = coercions.cast( field.type(), args.get( key ) );
                        field.set( instance, arg );
                    } );
                }
                return instance;
            } catch( Exception e ) {
                throw new ReflectException( "Cannot invoke constructor of class '"
                        + name()
                        + "' with extra parameters: " + argsToString( extraParameters ), e );
            }
        }

        public boolean isPublic() {
            return Modifier.isPublic( underlying.getModifiers() );
        }

        public String toString() {
            return underlying.getName() + "(" + Stream.of( parameters )
                .map( parameter -> parameter.type().typeToken.getType() + " " + parameter.name() )
                .collect( joining( "," ) ) + ")";
        }

        public int parameterCount() {
            return parameters.size();
        }

        public boolean typeMatch( Object... args ) {
            if( args.length != parameters.size() ) return false;
            Class<?>[] types = Arrays.map( Class.class, Object::getClass, args );
            List<Reflection> paramTypes = parameterTypes.get();
            for( int i = 0; i < types.length; i++ )
                if( !paramTypes.get( i ).assignableFrom( types[i] ) ) return false;
            return true;
        }

        public boolean nameMatch( Map<String, Object> args ) {
            return args.keySet().containsAll( parameterNames );
        }
    }

    public class Parameter extends AbstractAnnotated<java.lang.reflect.Parameter> {
        private final Supplier<Reflection> type = Functions.memoize( () ->
            Reflect.reflect( typeToken.resolveType( this.underlying.getParameterizedType() ) ) );

        Parameter( java.lang.reflect.Parameter parameter ) {
            super( parameter );
        }

        public Reflection type() {
            return type.get();
        }

        public String name() {
            return underlying.getName();
        }
    }

}
