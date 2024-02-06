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

package oap.storage.dynamo.client.streams;

import lombok.Builder;
import oap.storage.dynamo.client.DynamodbClient;
import oap.storage.dynamo.client.annotations.API;
import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.services.dynamodb.model.DescribeStreamRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeStreamResponse;
import software.amazon.awssdk.services.dynamodb.model.GetRecordsRequest;
import software.amazon.awssdk.services.dynamodb.model.GetRecordsResponse;
import software.amazon.awssdk.services.dynamodb.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.dynamodb.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.dynamodb.model.Shard;
import software.amazon.awssdk.services.dynamodb.model.ShardIteratorType;
import software.amazon.awssdk.services.dynamodb.model.StreamDescription;
import software.amazon.awssdk.services.dynamodb.model.TrimmedDataAccessException;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;
import software.amazon.awssdk.services.dynamodb.model.Record;

import java.util.List;
import java.util.Objects;

@Builder( builderMethodName = "hiddenBuilder" )
@API
public class DynamodbStreamsRecordProcessor {
    private DynamoDbStreamsClient streamClient;
    private DynamodbClient dynamodbClient;

    public static DynamodbStreamsRecordProcessor.DynamodbStreamsRecordProcessorBuilder builder( DynamodbClient dynamodbClient ) {
        return hiddenBuilder().dynamodbClient( dynamodbClient ).streamClient( dynamodbClient.getStreamClient() );
    }

    @API
    public Record processRecords( String streamArn, RecordWorker recordWorker ) {
        Objects.requireNonNull( recordWorker, "Worker must be presented" );
        Objects.requireNonNull( streamArn, "Make sure you have the stream opened" );
        String lastEvaluatedShardId = null;
        do {
            DescribeStreamResponse describeStreamResponse = streamClient.describeStream(
                DescribeStreamRequest.builder().streamArn( streamArn ).exclusiveStartShardId( lastEvaluatedShardId ).build()
            );
            StreamDescription streamDescription = describeStreamResponse.streamDescription();
            if ( null == streamDescription ) {
                break;
            }
            // If LastEvaluatedShardId is set, then there is
            // at least one more page of shard IDs to retrieve
            lastEvaluatedShardId = streamDescription.lastEvaluatedShardId();

            List<Shard> shards = streamDescription.shards();
            for ( Shard shard : shards ) {
                processShard( streamArn, shard, recordWorker );
            }
        } while ( lastEvaluatedShardId != null );
        return null;
    }

    private void processShard( String streamArn, Shard shard, RecordWorker recordWorker ) {
        String shardId = shard.shardId();
        String currentShardIterator = shardIterator( shardId, streamArn, shard.sequenceNumberRange().startingSequenceNumber() );
        pollShardIterator( streamArn, shard, currentShardIterator, recordWorker );
    }

    private String shardIterator( String shardId, String streamArn, String sequenceNumber ) {
        if ( null == sequenceNumber ) {
            return null;
        }
        GetShardIteratorRequest shardIteratorRequest = GetShardIteratorRequest.builder()
                .streamArn( streamArn )
                .shardId( shardId )
                .sequenceNumber( sequenceNumber )
                .shardIteratorType( ShardIteratorType.AFTER_SEQUENCE_NUMBER )
                .build();
        GetShardIteratorResponse getShardIteratorResult = streamClient.getShardIterator( shardIteratorRequest );

        try {
            if ( null == getShardIteratorResult.shardIterator() ) {
                return null;
            }
            return getShardIteratorResult.shardIterator();

        } catch ( Exception ex ) {
            if ( ex.getMessage().contains( "Invalid SequenceNumber" ) || ex instanceof TrimmedDataAccessException ) {
                shardIteratorRequest = GetShardIteratorRequest.builder()
                        .streamArn( streamArn )
                        .shardId( shardId )
                        .shardIteratorType( ShardIteratorType.TRIM_HORIZON )
                        .build();
                return streamClient.getShardIterator( shardIteratorRequest ).shardIterator();
            }
            throw ex;
        }
    }

    private void pollShardIterator( String streamArn, Shard shard, String currentShardIteratorArg, RecordWorker recordWorker ) {
        int iterationWithoutRecords = 0;
        String currentShardIterator = currentShardIteratorArg;
        while ( null != currentShardIterator ) {
            GetRecordsResponse recordsResponse = getRecordsResponse( streamArn, shard.shardId(), currentShardIterator );
            if ( null == recordsResponse ) {
                return;
            }
            List<Record> records = recordsResponse.records();
            if ( CollectionUtils.isEmpty( records ) && iterationWithoutRecords++ > dynamodbClient.emptyReadLimit ) {
                break;
            }
            records
                    .stream()
                    .filter( rec -> rec.dynamodb().keys().entrySet()
                    .stream()
                    .noneMatch( k -> k.getValue().s().startsWith( "fictiveRecordForTable:" ) ) )
                    .forEach( recordWorker );
            currentShardIterator = recordsResponse.nextShardIterator();
        }
    }

    private GetRecordsResponse getRecordsResponse( String streamArn, String shardId, String currentShardIteratorArg ) {
        // Use the shard iterator to read the stream records
        GetRecordsResponse recordsResponse;
        String currentShardIterator = currentShardIteratorArg;
        try {
            recordsResponse = streamClient.getRecords( GetRecordsRequest.builder()
                    .limit( dynamodbClient.maxRowsPerSingleRead )
                    .shardIterator( currentShardIterator ).build() );
        } catch ( Exception ex ) {
            if ( ex.getMessage().contains( "Invalid SequenceNumber" ) || ex instanceof TrimmedDataAccessException ) {
                GetShardIteratorRequest shardIteratorRequest = GetShardIteratorRequest.builder()
                        .shardId( shardId )
                        .streamArn( streamArn )
                        .shardIteratorType( ShardIteratorType.TRIM_HORIZON )
                        .build();

                currentShardIterator = streamClient.getShardIterator( shardIteratorRequest ).shardIterator();
                recordsResponse = streamClient.getRecords( GetRecordsRequest.builder()
                        .limit( dynamodbClient.maxRowsPerSingleRead )
                        .shardIterator( currentShardIterator ).build() );
            } else {
                throw ex;
            }
        }
        return recordsResponse;
    }

}
