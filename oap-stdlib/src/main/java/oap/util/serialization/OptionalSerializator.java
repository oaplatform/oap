/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.util.serialization;

import lombok.SneakyThrows;
import oap.reflect.Reflect;
import oap.reflect.Reflection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;

/**
 * This class allows to use serialization mechanism for Optional fields in classes.
 * You have to implement OptionalSerializator and provide default methods read/write like below.
 * Using this interface you may mitigate challenge for Optional in Serialization classes.
 * See https://stackoverflow.com/questions/24547673/why-java-util-optional-is-not-serializable-how-to-serialize-the-object-with-suc
 * <p>
 * Usage:
 * public class Demo implements Serializable, OptionalSerializator {
 * private Optional<String> field = Optional.empty();
 * <p>
 * private void writeObject( @NotNull ObjectOutputStream oos ) throws IOException {
 * writeObjectTemplate( oos );
 * }
 * <p>
 * private void readObject( @NotNull ObjectInputStream ois ) throws IOException, ClassNotFoundException {
 * readObjectTemplate( ois );
 * }
 * }
 * <p>
 * <p>
 * You may also implement your own fieldShouldBeSkipped and valueShouldBeSkipped to modify/extend serialization/deserialization mechanism.
 * Note: if class contains any collection of Optional - you should implement it ser/deser manually in appropriate methods.
 */
public interface OptionalSerializator extends Serializable {

    default boolean fieldShouldBeSkipped( Reflection.Field field ) {
        return field.isTransient() || field.isStatic();
    }

    default boolean valueShouldBeSkipped( Reflection.Field field, Object innerObject ) {
        return false;
    }

    default void writeObjectTemplate( ObjectOutputStream oos ) throws IOException {
        Reflection reflectClass = Reflect.reflect( this.getClass() );
        reflectClass.fields.forEach( ( name, field ) -> {
            if( fieldShouldBeSkipped( field ) ) return;
            Object innerObject = field.get( this );
            if( valueShouldBeSkipped( field, innerObject ) ) return;
            if( field.type().isOptional() ) {
                innerObject = ( ( Optional ) innerObject ).orElse( null );
            }
            writeObject( oos, ( Serializable ) innerObject );
        } );
    }

    @SneakyThrows
    private void writeObject( ObjectOutputStream oos, Serializable innerObject ) {
        oos.writeObject( innerObject );
    }

    default void readObjectTemplate( ObjectInputStream ois ) throws IOException, ClassNotFoundException {
        Reflection reflectClass = Reflect.reflect( this.getClass() );
        reflectClass.fields.forEach( ( name, field ) -> {
            if( fieldShouldBeSkipped( field ) ) return;
            Object innerObject = readInnerObject( ois );
            if( valueShouldBeSkipped( field, innerObject ) ) return;
            if( field.type().isOptional() ) {
                field.set( this, Optional.ofNullable( innerObject ) );
            } else {
                field.set( this, innerObject );
            }
        } );
    }

    @SneakyThrows
    private Object readInnerObject( ObjectInputStream ois ) {
        return ois.readObject();
    }
}
