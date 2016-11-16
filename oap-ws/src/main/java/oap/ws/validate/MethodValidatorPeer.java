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
package oap.ws.validate;

import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Stream;
import oap.ws.WsException;

import java.util.List;
import java.util.Map;

public class MethodValidatorPeer implements ValidatorPeer {
   private final List<Validator> validators;

   public MethodValidatorPeer( WsValidate validate, Object instance ) {
      this.validators = Stream.of( validate.value() )
         .<Validator>map( m -> new ParameterValidator( m, instance ) )
         .toList();
   }

   public MethodValidatorPeer( WsValidate validate, Reflection.Method targetMethod, Object instance ) {
      this.validators = Stream.of( validate.value() )
         .<Validator>map( m -> new MethodValidator( m, targetMethod, instance ) )
         .toList();
   }

   @Override
   public ValidationErrors validate( Object value ) {
      return Stream.of( validators )
         .foldLeft( ValidationErrors.empty(),
            ( e, v ) -> e.merge( v.validate( value ) ) );
   }

   private static abstract class Validator {
      protected final Reflection.Method method;
      protected final Object instance;

      protected Validator( String method, Object instance ) {
         this.method = Reflect.reflect( instance.getClass() )
            .method( method )
            .orElseThrow( () -> new WsException( "no such method " + method ) );
         this.instance = instance;
      }

      abstract ValidationErrors validate( Object value );
   }

   private static class ParameterValidator extends Validator {
      public ParameterValidator( String method, Object instatnce ) {
         super( method, instatnce );
      }

      @Override
      public ValidationErrors validate( Object value ) {
         return method.invoke( instance, value );
      }
   }

   private static class MethodValidator extends Validator {
      private final Map<String, Integer> validatorMethodParamIndices;

      protected MethodValidator( String method, Reflection.Method targetMethod, Object instance ) {
         super( method, instance );
         validatorMethodParamIndices = Stream.of( targetMethod.parameters )
            .map( Reflection.Parameter::name )
            .zipWithIndex()
            .filter( ( p, i ) -> this.method.hasParameter( p ) )
            .toMap();
      }

      @Override
      ValidationErrors validate( Object value ) {
         Object[] params = new Object[method.parameters.size()];
         for( int i = 0; i < params.length; i++ )
            params[i] = ( ( Object[] ) value )[validatorMethodParamIndices.get( method.parameters.get( i ).name() )];
         return method.invoke( instance, params );
      }
   }
}
