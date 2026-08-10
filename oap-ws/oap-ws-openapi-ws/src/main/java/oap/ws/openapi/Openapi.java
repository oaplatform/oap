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

package oap.ws.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import oap.ws.WebServices;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class Openapi {

    private final WebServices webServices;
    public ApiInfo info;

    public Openapi( WebServices webServices ) {
        this.webServices = webServices;
    }

    public Openapi( WebServices webServices, ApiInfo info ) {
        this( webServices );
        this.info = info;
    }
    public OpenAPI generateOpenApi() {
        return generateOpenApi( true );
    }

    public OpenAPI generateOpenApi( boolean skipDeprecated ) {
        return generateOpenApi( skipDeprecated, Optional.empty() );
    }

    public OpenAPI generateOpenApi( boolean skipDeprecated, Optional<String> port ) {
        OpenapiGenerator openapiGenerator = new OpenapiGenerator(
            info.title,
            info.description,
            new OpenapiGenerator.Settings( OpenapiGenerator.Settings.OutputType.JSON, skipDeprecated ) );
        openapiGenerator.beforeProcesingServices();
        for( Map.Entry<String, Object> ws : webServices.services.entrySet() ) {
            if( !webServices.servicePorts.getOrDefault( ws.getKey(), Optional.empty() ).equals( port ) ) continue;
            openapiGenerator.processWebservice( ws.getValue().getClass(), ws.getKey() );
        }
        openapiGenerator.afterProcesingServices();
        return openapiGenerator.build();
    }
}
