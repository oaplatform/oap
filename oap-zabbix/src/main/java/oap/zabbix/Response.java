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

package oap.zabbix;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by igor.petrenko on 28.09.2017.
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1927251369169425165L;

    private static final Pattern RESPONSE_PATERN = Pattern.compile( "Processed (\\d+) Failed (\\d+) Total (\\d+) Seconds spent ([\\d.]+)" );

    public final String response;
    public final String info;

    @JsonCreator
    public Response( @JsonProperty String response, @JsonProperty String info ) {
        this.response = response;
        this.info = info;
    }

    @JsonIgnore
    public final int getProcessed() {
        return get( 1, Integer::parseInt, () -> 0 );
    }

    private <T> T get( int group, Function<String, T> func, Supplier<T> defaultValue ) {
        final Matcher matcher = RESPONSE_PATERN.matcher( info );
        if( matcher.matches() ) {
            return func.apply( matcher.group( group ) );
        }

        return defaultValue.get();
    }

    @JsonIgnore
    public final int getFailed() {
        return get( 2, Integer::parseInt, () -> 2 );
    }

    @JsonIgnore
    public final int getTotal() {
        return get( 3, Integer::parseInt, () -> 3 );
    }

    @JsonIgnore
    public final long getSpent() {
        return get( 4, str -> ( long ) ( Double.parseDouble( str ) * 1000000 ), () -> 0L );
    }

}
