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

import org.influxdb.InfluxDB;
import org.influxdb.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

public class MockInfluxDB implements InfluxDB {
    public ArrayList<Point> writes = new ArrayList<>();

    @Override
    public InfluxDB setLogLevel( LogLevel logLevel ) {
        return this;
    }

    @Override
    public InfluxDB enableBatch( int actions, int flushDuration, TimeUnit flushDurationTimeUnit ) {
        return this;
    }

    @Override
    public void disableBatch() {

    }

    @Override
    public Pong ping() {
        return new Pong();
    }

    @Override
    public String version() {
        return "mock";
    }

    @Override
    public void write( String database, String retentionPolicy, Point point ) {
        writes.add( point );
    }

    @Override
    public void write( BatchPoints batchPoints ) {
        writes.addAll( batchPoints.getPoints() );

    }

    @Override
    public QueryResult query( Query query ) {
        return new QueryResult();
    }

    @Override
    public QueryResult query( Query query, TimeUnit timeUnit ) {
        return new QueryResult();
    }

    @Override
    public void createDatabase( String name ) {

    }

    @Override
    public void deleteDatabase( String name ) {

    }

    @Override
    public List<String> describeDatabases() {
        return emptyList();
    }

    @Override
    public void setConnectTimeout( long connectTimeout, TimeUnit timeUnit ) {
    }

    @Override
    public void setReadTimeout( long readTimeout, TimeUnit timeUnit ) {
    }

    @Override
    public void setWriteTimeout( long writeTimeout, TimeUnit timeUnit ) {

    }
}
