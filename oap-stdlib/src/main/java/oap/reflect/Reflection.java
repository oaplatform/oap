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

import com.google.common.base.Suppliers;
import com.google.common.reflect.TypeToken;
import oap.util.Arrays;
import oap.util.Functions;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Stream;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;
import static oap.util.Pair.__;

public class Reflection extends Annotated<Class<?>> {
    //todo why map?
    public final LinkedHashMap<String, Field> fields = new LinkedHashMap<>();
    private final Coercions coercions;
    private final TypeToken<?> typeToken;
    public List<Method> methods;
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
        Stream<A> interfaceDeclaredObjects = Stream.<Class<?>>traverse( clazz, Class::getSuperclass )
            .flatMap( s -> Stream.of( s.getInterfaces() ) )
            .flatMap( c -> Stream.of( collector.apply( c ) ) );
        return Stream.<Class<?>>traverse( clazz, Class::getSuperclass )
            .flatMap( c -> Stream.of( collector.apply( c ) ) )
            .concat( interfaceDeclaredObjects )
            .toList();
    }

    private static void trySetAccessible( AccessibleObject ao, boolean flag ) {
        try {
            ao.setAccessible( flag );
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

    @SuppressWarnings( "unchecked" )
    public <T> T newInstance() {
        return newInstance( Maps.empty() );
    }

    @SuppressWarnings( "unchecked" )
    public <T> T newInstance( Object... args ) {
        for( Constructor constructor : constructors )
            if( constructor.typeMatch( args ) ) return constructor.invoke( args );

        throw constructorNotFound( args );
    }

    @SuppressWarnings( "unchecked" )
    public <T> T newInstance( Map<String, Object> args ) {
        for( Constructor constructor : constructors )
            if( constructor.nameMatch( args ) ) return constructor.invoke( args );

        throw constructorNotFound( args );
    }

    private ReflectException constructorNotFound( Object args ) {
        List<String> candidates = Stream.of( constructors ).map( Constructor::toString ).toList();

        return new ReflectException( underlying + ": cannot find matching constructor: " + args + " candidates: " + candidates + ". Classes must be compiled with '-parameters' option of javac." );
    }

    public boolean assignableTo( Class<?> clazz ) {
        return clazz.isAssignableFrom( this.underlying );
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
     * @param name Method name
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
        return baseOf( typeToken.getRawType() )
            .filter( i -> Collection.class.isAssignableFrom( i ) && i.getTypeParameters().length > 0 )
            .map( i -> resolve( i.getTypeParameters()[0] ) )
            .findAny()
            .orElse( null );
    }

    public Pair<Reflection, Reflection> getMapComponentsType() {
        return baseOf( typeToken.getRawType() )
            .filter( i -> Map.class.isAssignableFrom( i ) && i.getTypeParameters().length > 1 )
            .map( i -> __( resolve( i.getTypeParameters()[0] ), resolve( i.getTypeParameters()[1] ) ) )
            .findAny()
            .orElse( null );
    }

    private static Stream<Class<?>> baseOf( Class<?> clazz ) {
        return Stream.flatTraverse( clazz,
            c -> {
                Stream<Class<?>> result = Stream.of( clazz.getInterfaces() );
                return c.getSuperclass() != null ? result.concat( c.getSuperclass() ) : result;
            } );
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

    public class Field extends Annotated<java.lang.reflect.Field> implements Comparable<Field> {
        private final Supplier<Reflection> type = Functions.memoize( () ->
            Reflect.reflect( typeToken.resolveType( this.underlying.getGenericType() ) ) );

        Field( java.lang.reflect.Field field ) {
            super( field );
            trySetAccessible( this.underlying, true );
        }

        public Object get( Object instance ) {
            try {
                return this.underlying.get( instance );
            } catch( IllegalAccessException e ) {
                throw new ReflectException( e );
            }
        }

        public void set( Object instance, Object value ) {
            try {
                this.underlying.set( instance, value );
            } catch( IllegalAccessException e ) {
                throw new ReflectException( e );
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

        public boolean isSynthetic() {
            return underlying.isSynthetic();
        }
    }

    public class Method extends Annotated<java.lang.reflect.Method> {
        public List<Parameter> parameters;
        private final Supplier<Reflection> returnType = Functions.memoize( () ->
            Reflect.reflect( typeToken.resolveType( this.underlying.getGenericReturnType() ) ) );

        Method( java.lang.reflect.Method method ) {
            super( method );
            trySetAccessible( this.underlying, true );
            this.parameters = Lists.map( method.getParameters(), Parameter::new );
        }

        public boolean hasParameter( String name ) {
            return Lists.find( this.parameters, p -> Objects.equals( p.name(), name ) ).isPresent();
        }

        public Parameter getParameter( String name ) {
            return parameters.stream().filter( p -> p.name().equals( name ) ).findFirst().orElse( null );
        }

        public String name() {
            return underlying.getName();
        }

        @SuppressWarnings( "unchecked" )
        public <T> T invoke( Object instance, Object... args ) {
            try {
                return ( T ) underlying.invoke( instance, args );
            } catch( IllegalAccessException | InvocationTargetException | IllegalArgumentException e ) {
                throw new ReflectException( e );
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

    public class Constructor extends Annotated<java.lang.reflect.Constructor<?>> {
        public final List<Parameter> parameters;
        private final Supplier<List<Reflection>> parameterTypes;
        private final List<String> parameterNames;

        Constructor( java.lang.reflect.Constructor<?> constructor ) {
            super( constructor );
            trySetAccessible( this.underlying, true );
            this.parameters = Lists.map( constructor.getParameters(), Parameter::new );
            this.parameterNames = Lists.map( constructor.getParameters(), java.lang.reflect.Parameter::getName );
            this.parameterTypes = Suppliers.memoize( () ->
                Stream.of( parameters ).map( Reflection.Parameter::type ).toList() );

        }

        public boolean hasParameter( String name ) {
            return Lists.find( this.parameters, p -> Objects.equals( p.name(), name ) ).isPresent();
        }

        public Parameter getParameter( String name ) {
            return parameters.stream().filter( p -> p.name().equals( name ) ).findFirst().orElse( null );
        }

        public String name() {
            return underlying.getName();
        }

        @SuppressWarnings( "unchecked" )
        public <T> T invoke( Object... args ) {
            try {
                return ( T ) underlying.newInstance( args );
            } catch( IllegalAccessException | InvocationTargetException | IllegalArgumentException | InstantiationException e ) {
                throw new ReflectException( this + ":" + java.util.Arrays.toString( args ), e );
            }
        }

        public <T> T invoke( Map<String, Object> args ) {
            //      @todo check match of parameter types
            try {
                Object[] cArgs = Stream.of( parameters )
                    .map( p -> coercions.cast( p.type(), args.get( p.name() ) ) )
                    .toArray();
                T instance = invoke( cArgs );

                for( String key : args.keySet() ) {
                    if( parameterNames.contains( key ) ) continue;

                    Optional<Field> f = field( key );
                    f.ifPresent( field -> {
                        Object arg = coercions.cast( field.type(), args.get( key ) );
                        field.set( instance, arg );
                    } );
                }

                return instance;

            } catch( Exception e ) {
                throw new ReflectException( this + ":" + args, e );
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

    public class Parameter extends Annotated<java.lang.reflect.Parameter> {
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
