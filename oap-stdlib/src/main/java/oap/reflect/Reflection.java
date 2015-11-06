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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.reflect.TypeToken;
import oap.util.Arrays;
import oap.util.Lists;
import oap.util.Stream;
import oap.util.Try;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

public class Reflection extends Annotated<Class<?>> {
    //    @todo constructors (PERFORMANCE)
    private static Coercions coercions = Coercions.basic().withIdentity();
    public final Class<?> underlying;
    public final List<Field> fields;
    public final List<Method> methods;
    public final List<Reflection> typeParameters;
    private final TypeToken<?> typeToken;

    Reflection( TypeToken<?> typeToken ) {
        super( typeToken.getRawType() );
        this.typeToken = Objects.requireNonNull( typeToken );
        this.methods =
            Lists.map( ReflectUtils.declared( typeToken.getRawType(), Class::getDeclaredMethods ), Method::new
            );
        this.fields = Lists.map( ReflectUtils.declared( typeToken.getRawType(), Class::getDeclaredFields ), Field::new
        );
        this.underlying = typeToken.getRawType();
        this.typeParameters = Lists.map( typeToken.getRawType().getTypeParameters(), this::resolve );
    }

    @SuppressWarnings( "unchecked" )
    public <T> T newInstance() {
        try {
            Class<?> rawType = typeToken.getRawType();
            Constructor<?> constructor = rawType.getDeclaredConstructor();
            constructor.setAccessible( true );
            return (T) constructor.newInstance();
        } catch( InstantiationException | IllegalAccessException |
            NoSuchMethodException | InvocationTargetException e ) {
            throw new ReflectException( e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T newInstance( Object... args ) {
        return ReflectUtils.findExecutableByParamTypes(
            Arrays.map( Class.class, Object::getClass, args ),
            typeToken.getRawType().getConstructors() )
            .map( Try.map( constructor -> {
                constructor.setAccessible( true );
                return (T) constructor.newInstance( args );
            } ) )
            .orElseThrow( () -> new ReflectException(
                "cannot find matching constructor: " + java.util.Arrays.toString( args ) + " in " +
                    typeToken.getRawType() ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T> T newInstance( Map<String, Object> args ) {
        Constructor<?>[] constructors = typeToken.getRawType().getConstructors();
//              @todo initialization of constructorless classes
        java.util.Arrays.sort( constructors, Comparator
            .<Constructor<?>>comparingInt( Constructor::getParameterCount )
            .reversed() );
        for( Constructor<?> constructor : constructors ) {
            List<String> paramNames = Lists.of( Arrays.map( String.class,
                java.lang.reflect.Parameter::getName,
                constructor.getParameters() ) );
//      @todo check correspondence of parameter types
            if( args.keySet().containsAll( paramNames ) ) try {
                constructor.setAccessible( true );
                ArrayList<java.lang.reflect.Parameter> params = Lists.of( constructor.getParameters() );
                T instance = (T) constructor.newInstance( params
                    .stream()
                    .map( p -> coercions.cast(
                        resolve( p.getParameterizedType() ),
                        args.get( p.getName() ) ) )
                    .collect( toList() )
                    .toArray() );
                args.keySet()
                    .stream()
                    .filter( ((Predicate<Object>) paramNames::contains).negate() )
                    .forEach( key -> field( key ).ifPresent( f ->
                            f.set( instance, coercions
                                .cast( f.type(), args.get( key ) ) )
                    ) );
                return instance;
            } catch( Exception e ) {
                throw new ReflectException( constructor.toString() + ":", e );
            }
        }
        throw new ReflectException( "cannot find matching constructor: " + args + " in " + typeToken.getRawType() );
    }

    public boolean assignableTo( Class<?> clazz ) {
        return clazz.isAssignableFrom( this.typeToken.getRawType() );
    }

    public boolean assignableFrom( Class<?> clazz ) {
        return this.typeToken.isAssignableFrom( clazz );
    }

    public boolean isEnum() {
        return this.typeToken.getRawType().isEnum();
    }

    public boolean isOptional() {
        return Optional.class.equals( this.typeToken.getRawType() );
    }

    public Optional<? extends Enum<?>> enumValue( String value ) {
        return Arrays.find(
            constant -> Objects.equals( constant.name(), value ),
            (Enum<?>[]) this.typeToken.getRawType().getEnumConstants()
        );
    }

    //    todo cache all invokers of resolve (PERFORMANCE)
    Reflection resolve( Type type ) {
        return Reflect.reflect( typeToken.resolveType( type ) );
    }


    public Optional<Field> field( String name ) {
        return Lists.find( fields, f -> f.name().equals( name ) );
    }

    public Optional<Method> method( Predicate<Method> matcher ) {
        return this.methods.stream().filter( matcher ).findFirst();
    }

    public Reflection getCollectionComponentType() {
        return Stream.<Class<?>>traverse( typeToken.getRawType(), Class::getSuperclass )
            .filter( i -> Collection.class.isAssignableFrom( i ) && i.getTypeParameters().length > 0 )
            .map( i -> resolve( i.getTypeParameters()[0] ) )
            .findAny()
            .orElse( null );
    }

    @Override
    public String toString() {
        return "Reflection(" + typeToken + ")";
    }

    @Override
    public boolean equals( Object obj ) {
        return obj != null
            && obj instanceof Reflection
            && this.typeToken.equals( ((Reflection) obj).typeToken );
    }

    @Override
    public int hashCode() {
        return this.typeToken.hashCode();
    }

    public String name() {
        return this.typeToken.getRawType().getCanonicalName();
    }

    public class Field extends Annotated<java.lang.reflect.Field> implements Comparable<Field> {
        private final Supplier<Reflection> type = Suppliers.memoize( () ->
            Reflect.reflect( typeToken.resolveType( this.underlying.getGenericType() ) ) );

        Field( java.lang.reflect.Field field ) {
            super( field );
            this.underlying.setAccessible( true );
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
        public int compareTo( Field o ) {
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
        public List<Parameter> paramerers;

        Method( java.lang.reflect.Method method ) {
            super( method );
            this.underlying.setAccessible( true );
            this.paramerers = Lists.map( method.getParameters(), Parameter::new );
        }

        public String name() {
            return underlying.getName();
        }

        @SuppressWarnings( "unchecked" )
        public <T> T invoke( Object instance, Object... args ) {
            try {
                return (T) underlying.invoke( instance, args );
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
    }

    public class Parameter extends Annotated<java.lang.reflect.Parameter> {
        private final Supplier<Reflection> type = Suppliers.memoize( () ->
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
