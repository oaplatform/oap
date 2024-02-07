/*
  The MIT License (MIT)
  <p>
  Copyright (c) Open Application Platform Authors
  <p>
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  <p>
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.
  <p>
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */

package oap.ws.file;

import oap.http.Http;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.validate.WsValidateJson;
import org.apache.commons.io.FilenameUtils;

import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.io.MimeTypes.mimetypeOf;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.QUERY;

public class FileWS {
    public static final String DATA_SCHEMA = "/oap/ws/file/schema/data.conf";
    private final BucketManager bucketManager;

    public FileWS( BucketManager bucketManager ) {
        this.bucketManager = bucketManager;
    }

    @WsMethod( method = POST, path = "/", produces = "text/plain" )
    public String upload( @WsParam( from = BODY ) @WsValidateJson( schema = DATA_SCHEMA ) Data data, Optional<String> bucket ) {
        return bucket.map( b -> bucketManager.put( b, data ) )
            .orElseGet( () -> bucketManager.put( data ) );
    }

    @WsMethod( method = GET, path = "/" )
    public Response download( @WsParam( from = QUERY ) String path, Optional<String> bucket ) {
        byte[] bytes = bucket.map( b -> bucketManager.get( b, path ) )
            .orElseGet( () -> bucketManager.get( path ) )
            .orElse( null );
        if( bytes == null ) {
            return Response.notFound();
        } else {
            var contentType = mimetypeOf( FilenameUtils.getExtension( path ) ).orElse( Http.ContentType.APPLICATION_OCTET_STREAM );

            return Response.ok().withContentType( contentType ).withBody( bytes );
        }
    }

}
