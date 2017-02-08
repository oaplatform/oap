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

package oap.ws;

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
import oap.util.Try;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.UploadContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Supplier;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

/**
 * Created by igor.petrenko on 06.02.2017.
 */
@Slf4j
public class WsFileUploader extends FileUploader implements Handler {
    private final FileUpload upload;

    public WsFileUploader( Path path, long maxMemorySize, long maxRequestSize ) {
        log.info( "file uploader path = {}", path );

        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold( ( int ) maxMemorySize );
        factory.setRepository( path.toFile() );
        factory.setFileCleaningTracker( new FileCleaningTracker() );
        upload = new FileUpload( factory );
        upload.setSizeMax( maxRequestSize );
    }

    @Override
    @SneakyThrows
    public void handle( Request request, Response response ) {
        final RequestContext ctx = new RequestUploadContext( request );
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

            val id = Cuid.next();

            final Item file = new Item(
                prefixItem.getString(),
                id,
                fileItem.getName(),
                fileItem.getContentType(),
                Try.supply( fileItem::getInputStream )
            );
            log.debug( "new file = {}", file );
            fireUploaded( file );

            response.respond( HttpResponse.ok( new IdResponse( id ) ) );
        } else {
            response.respond( HttpResponse.NOT_FOUND );
        }
    }

    public interface WsFileUploaderListener {
        void uploaded( Item file );
    }

    @ToString
    @EqualsAndHashCode
    public static class IdResponse {
        public final String id;

        public IdResponse( String id ) {
            this.id = id;
        }
    }

    @ToString( exclude = { "isF" } )
    public static class Item {
        public final String prefix;
        public final String id;
        public final String name;
        public final String contentType;
        public final Supplier<InputStream> isF;

        public Item( String prefix, String id, String name, String contentType, Supplier<InputStream> isF ) {
            this.prefix = prefix;
            this.id = id;
            this.name = name;
            this.contentType = contentType;
            this.isF = isF;
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
