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

package oap.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by Igor Petrenko on 01.12.2015.
 */
public class ReporterFilter implements MetricFilter {
    public final ArrayList<Pattern> include;
    public final ArrayList<Pattern> exclude;

    public ReporterFilter() {
        this( new ArrayList<>(), new ArrayList<>() );
    }

    public ReporterFilter( ArrayList<Pattern> include, ArrayList<Pattern> exclude ) {
        this.include = include;
        this.exclude = exclude;
    }

    @Override
    public boolean matches( String name, Metric metric ) {
        if( !include.isEmpty() && !include.stream().filter( e -> e.matcher( name ).find() ).findAny().isPresent() )
            return false;

        if( exclude.isEmpty() ) return true;

        return !exclude.stream().filter( e -> e.matcher( name ).find() ).findAny().isPresent();
    }
}
