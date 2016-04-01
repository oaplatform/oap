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
package oap.concurrent;

import java.util.concurrent.Semaphore;

public class SynchronizedThread implements Runnable {
   private Thread thread = new Thread( this );
   private Runnable child;
   private Semaphore semaphore = new Semaphore( 0 );
   private boolean stopped = true;

   public SynchronizedThread( Runnable child ) {
      this.child = child;
   }

   public SynchronizedThread( String name, Runnable child ) {
      this( child );
      this.thread.setName( name );
   }

   @Override
   public void run() {
      semaphore.release();
      child.run();
   }

   public synchronized void start() {
      stopped = false;
      thread.start();
      try {
         semaphore.acquire();
      } catch( InterruptedException e ) {
         throw new ThreadException( e );
      }
   }

   public synchronized void stop() {
      try {
         stopped = true;
         thread.interrupt();
         thread.join();
      } catch( InterruptedException e ) {
         throw new ThreadException( e );
      }
   }

   public void setName( String name ) {
      this.thread.setName( name );
   }

   public String getName() {
      return this.thread.getName();
   }

   public boolean isRunning() {
      return !stopped;
   }
}
