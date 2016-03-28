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

package oap.application.remote;

import org.nustaq.serialization.*;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by Igor Petrenko on 15.01.2016.
 */
class FST {
   FSTConfiguration conf;

   public FST() {
      conf = FSTConfiguration.createFastBinaryConfiguration();
      conf.registerClass( RemoteInvocation.class );
      conf.registerSerializer( Optional.class, new FSTOptionalSerializer(), false );
   }

   private class FSTOptionalSerializer extends FSTBasicObjectSerializer {
      @Override
      public void writeObject( FSTObjectOutput out, Object o, FSTClazzInfo fstClazzInfo, FSTClazzInfo.FSTFieldInfo fstFieldInfo, int i ) throws IOException {
         final Optional opt = ( Optional ) o;
         if( opt.isPresent() ) {
            out.writeObject( opt.get() );
         } else {
            out.writeObject( null );
         }
      }

      @Override
      public void readObject( FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy ) throws Exception {
      }

      @Override
      public Object instantiate( Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition ) throws Exception {
         return Optional.ofNullable( in.readObject() );
      }
   }
}
