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

import java.util.List;
import java.util.regex.Pattern;

import static oap.util.Lists.Collectors.toArrayList;

/**
 * Created by Igor Petrenko on 01.12.2015.
 */
public class ReporterFilter implements MetricFilter {
    public final List<Pattern> include;
    public final List<Pattern> exclude;

    public ReporterFilter( List<String> include, List<String> exclude ) {
        this.include = include.stream().map( Pattern::compile ).collect( toArrayList() );
        this.exclude = exclude.stream().map( Pattern::compile ).collect( toArrayList() );
    }

    @Override
    public boolean matches( String name, Metric metric ) {
        if( !include.isEmpty() && include.stream().noneMatch( e -> e.matcher( name ).find() ) )
            return false;

        if( exclude.isEmpty() ) return true;

        return exclude.stream().noneMatch( e -> e.matcher( name ).find() );
    }
}
