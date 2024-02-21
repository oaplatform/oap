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

import oap.io.Resources;
import oap.io.content.ContentReader;
import oap.json.Binder;
import oap.util.Lists;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Map;

public final class ResourceSchemaStorage implements SchemaStorage {
    public static final SchemaStorage INSTANCE = new ResourceSchemaStorage();

    private ResourceSchemaStorage() {
    }

    @Override
    public String get( String name ) {
        var ext = FilenameUtils.getExtension( name );
        var prefix = FilenameUtils.removeExtension( name );
        var fileName = FilenameUtils.removeExtension( FilenameUtils.getName( name ) );

        var conf = Resources.readOrThrow( getClass(), name, ContentReader.ofString() );
        if( "yaml".equalsIgnoreCase( ext ) ) conf = Binder.json.marshal( Binder.yaml.unmarshal( Map.class, conf ) );

        var extConf = Resources.readStrings( getClass(), prefix + "/" + fileName + ".conf" );
        var extJson = Resources.readStrings( getClass(), prefix + "/" + fileName + ".json" );
        var extYaml = Resources.readStrings( getClass(), prefix + "/" + fileName + ".yaml" );

        if( extConf.isEmpty() && extJson.isEmpty() && extYaml.isEmpty() ) return conf;

        var list = new ArrayList<String>();
        list.addAll( extConf );
        list.addAll( extJson );
        list.addAll( Lists.map( extYaml, y -> Binder.json.marshal( Binder.yaml.unmarshal( Map.class, y ) ) ) );


        return Binder.json.marshal( Binder.hoconWithConfig( false, list )
            .unmarshal( Map.class, conf ) );
    }
}
