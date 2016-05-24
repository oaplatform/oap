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
import oap.util.Lists;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Validators {
   private ConcurrentHashMap<Reflection.Parameter, Validator> forParams = new ConcurrentHashMap<>();
   private ConcurrentHashMap<Reflection.Method, Validator> forMethods = new ConcurrentHashMap<>();

   public Validator forParameter( Reflection.Parameter parameter, Object instance ) {
      return forParams.computeIfAbsent( parameter, p -> {
         Validator validator = new Validator();
         for( Annotation a : parameter.annotations() )
            Reflect.reflect( a.annotationType() ).findAnnotation( Peer.class )
               .ifPresent( va -> validator.peers.add( Reflect.newInstance( va.value(), a, instance ) ) );
         return validator;
      } );
   }

   public Validator forMethod( Reflection.Method method, Object instance ) {
      return forMethods.computeIfAbsent( method, p -> {
         Validator validator = new Validator();
         for( Annotation a : method.annotations() )
            Reflect.reflect( a.annotationType() ).findAnnotation( Peer.class )
               .ifPresent( va -> validator.peers.add( Reflect.newInstance( va.value(), a, instance ) ) );
         return validator;
      } );

   }

   public static class Validator {
      private final List<ValidatorPeer> peers = new ArrayList<>();

      public ValidationErrors validate( Object value ) {
         ValidationErrors total = ValidationErrors.create( Lists.empty() );
         for( ValidatorPeer peer : peers ) {
            ValidationErrors result = peer.validate( value );
            if( result.isFailed() && !result.hasDefaultCode() ) return result;
            total.merge( result );
         }
         return total;
      }
   }

}
