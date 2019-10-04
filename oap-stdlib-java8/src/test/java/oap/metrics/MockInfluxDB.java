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

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;

public class MockInfluxDB implements InfluxDB {
    public ArrayList<Point> writes = new ArrayList<>();

    @Override
    public InfluxDB setLogLevel( LogLevel logLevel ) {
        return this;
    }

    @Override
    public InfluxDB enableGzip() {
        return this;
    }

    @Override
    public InfluxDB disableGzip() {
        return this;
    }

    @Override
    public boolean isGzipEnabled() {
        return false;
    }

    @Override
    public InfluxDB enableBatch() {
        return this;
    }

    @Override
    public InfluxDB enableBatch( BatchOptions batchOptions ) {
        return this;
    }

    @Override
    public InfluxDB enableBatch( int actions, int flushDuration, TimeUnit flushDurationTimeUnit ) {
        return this;
    }

    @Override
    public InfluxDB enableBatch( int i, int i1, TimeUnit timeUnit, ThreadFactory threadFactory ) {
        return this;
    }

    @Override
    public InfluxDB enableBatch( int actions, int flushDuration, TimeUnit flushDurationTimeUnit, ThreadFactory threadFactory, BiConsumer<Iterable<Point>, Throwable> exceptionHandler, ConsistencyLevel consistency ) {
        return this;
    }

    @Override
    public InfluxDB enableBatch( int i, int i1, TimeUnit timeUnit, ThreadFactory threadFactory, BiConsumer<Iterable<Point>, Throwable> biConsumer ) {
        return this;
    }

    @Override
    public void disableBatch() {

    }

    @Override
    public boolean isBatchEnabled() {
        return false;
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
    public void write( Point point ) {

    }

    @Override
    public void write( String s ) {

    }

    @Override
    public void write( List<String> list ) {

    }

    @Override
    public void write( String database, String retentionPolicy, Point point ) {
        writes.add( point );
    }

    @Override
    public void write( int i, Point point ) {

    }

    @Override
    public void write( BatchPoints batchPoints ) {
        writes.addAll( batchPoints.getPoints() );

    }

    @Override
    public void writeWithRetry( BatchPoints batchPoints ) {

    }

    @Override
    public void write( String database, String retentionPolicy, ConsistencyLevel consistency, String records ) {

    }

    @Override
    public void write( String database, String retentionPolicy, ConsistencyLevel consistency, TimeUnit precision, String records ) {

    }

    @Override
    public void write( String database, String retentionPolicy, ConsistencyLevel consistency, List<String> records ) {

    }

    @Override
    public void write( String database, String retentionPolicy, ConsistencyLevel consistency, TimeUnit precision, List<String> records ) {

    }

    @Override
    public void write( int i, String s ) {

    }

    @Override
    public void write( int i, List<String> list ) {

    }

    @Override
    public QueryResult query( Query query ) {
        return new QueryResult();
    }

    @Override
    public void query( Query query, Consumer<QueryResult> onSuccess, Consumer<Throwable> onFailure ) {

    }

    @Override
    public void query( Query query, int i, Consumer<QueryResult> consumer ) {

    }

    @Override
    public void query( Query query, int chunkSize, BiConsumer<Cancellable, QueryResult> onNext ) {

    }

    @Override
    public void query( Query query, int chunkSize, Consumer<QueryResult> onNext, Runnable onComplete ) {

    }

    @Override
    public void query( Query query, int chunkSize, BiConsumer<Cancellable, QueryResult> onNext, Runnable onComplete ) {

    }

    @Override
    public void query( Query query, int chunkSize, BiConsumer<Cancellable, QueryResult> onNext, Runnable onComplete, Consumer<Throwable> onFailure ) {

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
    public boolean databaseExists( String s ) {
        return true;
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public InfluxDB setConsistency( ConsistencyLevel consistencyLevel ) {
        return this;
    }

    @Override
    public InfluxDB setDatabase( String s ) {
        return this;
    }

    @Override
    public InfluxDB setRetentionPolicy( String s ) {
        return this;
    }

    @Override
    public void createRetentionPolicy( String rpName, String database, String duration, String shardDuration, int replicationFactor, boolean isDefault ) {

    }

    @Override
    public void createRetentionPolicy( String rpName, String database, String duration, int replicationFactor, boolean isDefault ) {

    }

    @Override
    public void createRetentionPolicy( String rpName, String database, String duration, String shardDuration, int replicationFactor ) {

    }

    @Override
    public void dropRetentionPolicy( String rpName, String database ) {

    }
}
