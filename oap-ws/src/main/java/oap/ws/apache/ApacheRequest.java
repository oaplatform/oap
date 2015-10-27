/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
package oap.ws.apache;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ListMultimap;
import oap.util.Arrays;
import oap.util.Maps;
import oap.util.Strings;
import oap.ws.Context;
import oap.ws.Request;
import oap.ws.Url;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class ApacheRequest extends Request {
    private final ListMultimap<String, String> params;
    private final String requestLine;
    private final Request.HttpMethod httpMethod;
    private final String baseUrl;
    private final Context context;
    private final Optional<InputStream> body;
    private final Header[] headers;
    private final InetAddress remoteAddress;

    ApacheRequest( HttpRequest req, Context context ) throws IOException {
        this.context = context;
        this.params = params( req );
        this.headers = req.getAllHeaders();
        this.requestLine = Strings.substringBefore( req.getRequestLine().getUri(), "?" ).substring(
            context.serviceLocation.length() );
        this.httpMethod = Request.HttpMethod.valueOf( req.getRequestLine().getMethod().toUpperCase() );
        this.body = req instanceof HttpEntityEnclosingRequest ?
            Optional.of( ((HttpEntityEnclosingRequest) req).getEntity().getContent() )
            : Optional.empty();
        this.baseUrl = "http://" + req.getFirstHeader( "Host" ).getValue();
        this.remoteAddress = context.remoteAddress;

    }

    public List<String> parameters( String name ) {
        return params.containsKey( name ) ? params.get( name ) : Collections.emptyList();

    }

    @Override
    public Optional<InputStream> body() {
        return body;
    }

    @Override
    public Optional<String> header( String name ) {
        return Arrays.find( h -> name.equalsIgnoreCase( h.getName() ), headers )
            .map( Header::getValue );
    }

    @Override
    public boolean isBodyJson() {
        return header( "Content-Type" ).map( h -> h.contains( ContentType.APPLICATION_JSON.getMimeType() ) ).orElse(
            false );
    }

    @Override
    public InetAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public String requestLine() {
        return requestLine;
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    @Override
    public HttpMethod httpMethod() {
        return httpMethod;
    }

    @Override
    public Context context() {
        return context;
    }

    public Optional<String> parameter( String name ) {
        return parameters( name ).stream().findFirst();
    }

    static ListMultimap<String, String> params( HttpRequest req ) throws IOException {
        ListMultimap<String, String> query =
            Url.parseQuery( Strings.substringAfter( req.getRequestLine().getUri(), "?" ) );
        if( req.getFirstHeader( "Content-Type" ) != null
            && req.getFirstHeader( "Content-Type" ).getValue().startsWith( "application/x-www-form-urlencoded" )
            && req instanceof HttpEntityEnclosingRequest )
            return Maps.add( query,
                Url.parseQuery( EntityUtils.toString( ((HttpEntityEnclosingRequest) req).getEntity() ) ) );
        else return query;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper( this )
            .add( "baseUrl", baseUrl )
            .add( "requestLine", requestLine )
            .add( "method", httpMethod )
            .add( "params", params )
            .omitNullValues()
            .toString();
    }
}
