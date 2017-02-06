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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Response;
import oap.util.Try;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.UploadContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Created by igor.petrenko on 06.02.2017.
 */
@Slf4j
public class WsFileUploader implements Handler {
    private final ArrayList<WsFileUploaderListener> listeners = new ArrayList<>();

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

    public synchronized void addListener( WsFileUploaderListener listener ) {
        listeners.add( listener );
    }

    public synchronized void removeListener( WsFileUploaderListener listener ) {
        listeners.remove( listener );
    }

    protected synchronized void fireUploaded( FileItem file ) {
        listeners.forEach( l -> l.uploaded( file ) );
    }

    @Override
    @SneakyThrows
    public void handle( Request request, Response response ) {
        final RequestContext ctx = new RequestUploadContext( request );
        if( FileUpload.isMultipartContent( ctx ) ) {
            val items = upload.parseRequest( ctx );
            for( val item : items ) {
                if( !item.isFormField() )
                    fireUploaded( new FileItem( item.getName(), item.getContentType(), Try.supply( item::getInputStream ) ) );
            }

            response.respond( HttpResponse.NO_CONTENT );
        } else {
            response.respond( HttpResponse.HTTP_BAD_REQUEST );
        }
    }

    public interface WsFileUploaderListener {
        void uploaded( FileItem file );
    }

    public static class FileItem {
        public final String name;
        public final String contentType;
        public final Supplier<InputStream> isF;

        public FileItem( String name, String contentType, Supplier<InputStream> isF ) {
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
