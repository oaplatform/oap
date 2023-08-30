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

package oap.fs;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
public class S3FileManager extends AbstractFileManager  {

    private AmazonS3 s3client;
    private TransferManager transferManager;
    private String region;

    public S3FileManager( Map<String, Path> buckets, String region ) {
        super( buckets );
        this.region = region;
        log.info( "Init s3-file-manager" );
        if( this.s3client == null ) {
            this.s3client = AmazonS3ClientBuilder.standard().withRegion( this.region ).withCredentials( new ProfileCredentialsProvider() ).build();
        }
        transferManager = TransferManagerBuilder.standard().withS3Client( this.s3client ).build();
    }

    public S3FileManager( Map<String, Path> buckets, AmazonS3 s3client ) {
        super( buckets );
        this.s3client = s3client;
        this.transferManager = TransferManagerBuilder.standard().withS3Client( s3client ).build();
    }

    public Upload uploadStream( String bucket, S3Data data ) {
        var metadata = new ObjectMetadata();
        metadata.setContentLength( data.contentLength );
        var path = buckets.get( bucket ).resolve( data.name ).toString();
        return transferManager.upload( bucket, path, data.io, metadata );
    }

    public Upload uploadFile( String bucket, S3Data data ) {
        var metadata = new ObjectMetadata();
        metadata.setContentLength( data.contentLength );
        var path = buckets.get( bucket ).resolve( data.name ).toString();
        return transferManager.upload( bucket, path, data.io, metadata );
    }

    public Download download( String bucket, String relativePath, File destination ) {
        return transferManager.download( bucket, relativePath, destination );
    }

    public void copyFromTo( String src, String dist ) {
        log.warn( "Not implemented for S3" );
    }
}
