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
package oap.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Numbers {
   private static final Pattern numberPattern = Pattern.compile( "(\\d+)\\s*([^\\d]*)" );

   public static long parseLongWithUnits( String value ) {
      final Matcher matcher = numberPattern.matcher( value );
      if( matcher.find() ) {
         final String mod = matcher.group( 2 );
         final String number = matcher.group( 1 );

         switch( mod.toLowerCase() ) {
            case "kb":
               return Long.parseLong( number ) * 1024;
            case "mb":
               return Long.parseLong( number ) * 1024 * 1024;
            case "gb":
               return Long.parseLong( number ) * 1024 * 1024 * 1024;
            case "ms":
               return Long.parseLong( number );
            case "s":
            case "second":
            case "seconds":
               return Long.parseLong( number ) * 1000;
            case "m":
            case "minute":
            case "minutes":
               return Long.parseLong( number ) * 1000 * 60;
            case "h":
            case "hour":
            case "hours":
               return Long.parseLong( number ) * 1000 * 60 * 60;
            case "d":
            case "day":
            case "days":
               return Long.parseLong( number ) * 1000 * 60 * 60 * 24;
            default:
               return Long.parseLong( number );
         }
      } else throw new NumberFormatException( value );
   }
}
