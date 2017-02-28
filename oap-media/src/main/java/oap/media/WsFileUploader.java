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

package oap.media;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Response;
import oap.util.Cuid;
import oap.util.Stream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.UploadContext;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.util.Collections.emptyList;

/**
 * Created by igor.petrenko on 06.02.2017.
 */
@Slf4j
public class WsFileUploader extends FileUploader implements Handler {
    private final FileUpload upload;

    public WsFileUploader( Path path, long maxMemorySize, long maxRequestSize ) {
        this( path, maxMemorySize, maxRequestSize, emptyList() );
    }

    public WsFileUploader( Path path, long maxMemorySize, long maxRequestSize, List<MediaProcessing> postprocessing ) {
        super( postprocessing );
        log.info( "file uploader path = {}", path );

        val factory = new DiskFileItemFactory();
        factory.setSizeThreshold( ( int ) maxMemorySize );
        factory.setRepository( path.toFile() );
        upload = new FileUpload( factory );
        upload.setSizeMax( maxRequestSize );
    }

    @Override
    @SneakyThrows
    public void handle( Request request, Response response ) {
        val ctx = new RequestUploadContext( request );
        if( FileUpload.isMultipartContent( ctx ) ) {
            val items = upload.parseRequest( ctx );

            if( items.stream().filter( i -> !i.isFormField() ).count() != 1 ) {
                log.trace( "Only one file allowed" );
                response.respond( HttpResponse.status( HTTP_BAD_REQUEST, "Only one file allowed" ) );
                return;
            }

            if( items.stream().filter( i -> i.isFormField() && "prefix".equals( i.getFieldName() ) ).count() != 1 ) {
                log.trace( "'prefix' field is required" );
                response.respond( HttpResponse.status( HTTP_BAD_REQUEST, "'prefix' field is required" ) );
                return;
            }

            val fileItem = items.stream().filter( i -> !i.isFormField() ).findAny().get();
            val prefixItem = items.stream().filter( FileItem::isFormField ).findAny().get();

            try {
                val id = Cuid.next();

                val prefix = prefixItem.getString();
                val fileName = fileItem.getName();
                val file = new Media(
                    ( prefix.endsWith( "/" ) ? prefix + id
                        : prefix + "/" + id ) + "." + FilenameUtils.getExtension( fileName ),
                    fileName,
                    fileItem.getContentType(),
                    ( ( DiskFileItem ) fileItem ).getStoreLocation().toPath()
                );
                log.debug( "new file = {}, isInMemory = {}", file, fileItem.isInMemory() );

                if( fileItem.isInMemory() ) {
                    fileItem.write( file.path.toFile() );
                }

                val mediaInfo = new MediaInfo();

                val media = Stream.of( postprocessing ).foldLeft( file, ( f, p ) -> p.process( f, mediaInfo ) );

                log.trace( "media = {}", media );
                log.trace( "info = {}", mediaInfo );

                fireUploaded( media, mediaInfo );

                response.respond( HttpResponse.ok( new MediaResponse( media.id, mediaInfo ) ) );
            } finally {
                Files.deleteIfExists( ( ( DiskFileItem ) fileItem ).getStoreLocation().toPath() );
            }
        } else {
            response.respond( HttpResponse.NOT_FOUND );
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class MediaResponse {
        public final String id;
        public final MediaInfo info;

        public MediaResponse( String id, MediaInfo info ) {
            this.id = id;
            this.info = info;
        }
    }

    private static class RequestUploadContext implements UploadContext {
        private final Request request;

        public RequestUploadContext( Request request ) {this.request = request;}

        @Override
        public long contentLength() {
            try {
                return Long.parseLong( request.header( HTTP.CONTENT_LEN ).orElse( "" ) );
            } catch( NumberFormatException e ) {
                return -1;
            }
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public String getContentType() {
            return request.header( HTTP.CONTENT_TYPE ).orElse( null );
        }

        @Override
        public int getContentLength() {
            try {
                return Integer.parseInt( request.header( HTTP.CONTENT_LEN ).orElse( "" ) );
            } catch( NumberFormatException e ) {
                return -1;
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return request.body.orElse( null );
        }
    }
}
