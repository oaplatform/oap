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

package oap.json.schema;

import lombok.val;
import oap.io.Resources;
import oap.json.Binder;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Map;

public final class ResourceSchemaStorage implements SchemaStorage {
    public static final SchemaStorage INSTANCE = new ResourceSchemaStorage();

    private ResourceSchemaStorage() {
    }

    @Override
    public String get( String name ) {

        val ext = FilenameUtils.getExtension( name );
        val prefix = FilenameUtils.removeExtension( name );
        val fileName = FilenameUtils.removeExtension( FilenameUtils.getName( name ) );

        String conf = Resources.readStringOrThrow( getClass(), name );
        if( "yaml".equalsIgnoreCase( ext ) ) conf = Binder.json.marshal( Binder.yaml.unmarshal( Map.class, conf ) );

        val extConf = Resources.readString( getClass(), prefix + "/" + fileName + ".conf" );
        val extJson = Resources.readString( getClass(), prefix + "/" + fileName + ".json" );
        val extYaml = Resources.readString( getClass(), prefix + "/" + fileName + ".yaml" );

        if( !( extConf.isPresent() ) && !( extJson.isPresent() ) && !( extYaml.isPresent() ) ) return conf;

        val list = new ArrayList<String>();
        extConf.ifPresent( list::add );
        extJson.ifPresent( list::add );
        extYaml.ifPresent( y -> list.add( Binder.json.marshal( Binder.yaml.unmarshal( Map.class, y ) ) ) );


        return Binder.json.marshal( Binder.hoconWithConfig( false, list )
            .unmarshal( Map.class, conf ) );
    }
}
