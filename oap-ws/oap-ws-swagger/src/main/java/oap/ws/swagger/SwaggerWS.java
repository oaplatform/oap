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

package oap.ws.swagger;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;
import oap.io.Resources;
import oap.io.content.ContentReader;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.template.Template;
import oap.template.TemplateEngine;
import oap.ws.WsMethod;

import java.util.List;
import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.template.TemplateAccumulators.STRING;

/**
 * Serves a Swagger UI page rendered from {@link #urls}, a list of named OpenAPI document
 * endpoints (typically URLs served by {@code oap.ws.openapi.OpenapiWS} instances).
 */
@Slf4j
public class SwaggerWS {
    private final Template<Context, String, StringBuilder, ?, ?> template;

    /**
     * swagger-ui-dist version, e.g. "5.33.2".
     */
    public String version = "5.33.2";
    public List<SwaggerUrl> urls = List.of();
    public HttpServerExchange.HttpMethod loginMethod = HttpServerExchange.HttpMethod.POST;
    /**
     * Login request URL; contains the literal placeholders {@code {username}} and
     * {@code {password}}, filled in client-side from the login form on submit. Empty
     * disables the login form entirely.
     */
    public String urlTemplate = "";
    /**
     * POST login request body; same {@code {username}}/{@code {password}} placeholders.
     * Required when {@link #loginMethod} is POST and {@link #urlTemplate} is non-empty.
     */
    public Optional<String> requestTemplate = Optional.empty();

    public SwaggerWS( TemplateEngine templateEngine ) {
        String templateText = Resources.readOrThrow( getClass(), "swagger-ui.html.tmpl", ContentReader.ofString() );
        this.template = templateEngine.getRuntimeTemplate( "swagger-ui", new TypeRef<Context>() {}, templateText, STRING );
    }

    @WsMethod( method = GET, path = "/", produces = "text/html", description = "Renders the Swagger UI page." )
    public String render() {
        if( !urlTemplate.isEmpty() && loginMethod == POST && requestTemplate.isEmpty() )
            throw new IllegalStateException( "SwaggerWS: loginMethod=POST requires requestTemplate to be configured" );

        Context context = new Context();
        context.version = version;
        context.urls = urls;
        context.urlTemplate = urlTemplate;
        context.loginMethodJs = Binder.json.marshal( loginMethod.name() );
        context.urlTemplateJs = Binder.json.marshal( urlTemplate );
        context.requestTemplateJs = Binder.json.marshal( requestTemplate.orElse( "" ) );
        return template.render( context ).get();
    }

    /**
     * @param url OpenAPI JSON document URL, typically served by an {@code OpenapiWS} instance (e.g. "/system/openapi").
     */
    @ToString
    public static class SwaggerUrl {
        public String name;
        public String url;

        public SwaggerUrl() {
        }

        public SwaggerUrl( String name, String url ) {
            this.name = name;
            this.url = url;
        }
    }

    public static class Context {
        public String version;
        public List<SwaggerUrl> urls;
        public String urlTemplate;
        public String loginMethodJs;
        public String urlTemplateJs;
        public String requestTemplateJs;
    }
}
