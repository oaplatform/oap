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
import oap.util.Pair;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static oap.util.Pair.__;

public final class ResourceSchemaStorage implements SchemaStorage {
    public static final SchemaStorage INSTANCE = new ResourceSchemaStorage();

    private ResourceSchemaStorage() {
    }

    @Override
    public String get( String name ) {
        String prefix = FilenameUtils.removeExtension( name );
        String fileName = FilenameUtils.removeExtension( FilenameUtils.getName( name ) );

        Pair<String, Binder> origConf =
            Resources.read( getClass(), name + ".conf", ContentReader.ofString() ).map( str -> __( str, Binder.hoconWithoutSystemProperties ) )
                .or( () -> Resources.read( getClass(), name + ".yaml", ContentReader.ofString() ).map( str -> __( str, Binder.yaml ) ) )
                .or( () -> Resources.read( getClass(), name + ".json", ContentReader.ofString() ).map( str -> __( str, Binder.json ) ) )

                .or( () -> Resources.read( getClass(), name, ContentReader.ofString() ).map( str -> __( str, Binder.Format.of( name, false ).binder ) ) )
                .orElseThrow( () -> new JsonSchemaException( "resource not found " + name + "[|.conf|.json|.yaml] for context class " + getClass() ) );

        String conf = Binder.json.marshal( origConf._2.unmarshal( Map.class, origConf._1 ) );

        List<String> extConf = Resources.readStrings( getClass(), prefix + "/" + fileName + ".conf" );
        List<String> extJson = Resources.readStrings( getClass(), prefix + "/" + fileName + ".json" );
        List<String> extYaml = Resources.readStrings( getClass(), prefix + "/" + fileName + ".yaml" );

        if( extConf.isEmpty() && extJson.isEmpty() && extYaml.isEmpty() ) {
            return conf;
        }

        ArrayList<String> list = new ArrayList<>();
        list.addAll( extConf );
        list.addAll( extJson );
        list.addAll( Lists.map( extYaml, y -> Binder.json.marshal( Binder.yaml.unmarshal( Map.class, y ) ) ) );


        return Binder.json.marshal( Binder.hoconWithConfig( false, list )
            .unmarshal( Map.class, conf ) );
    }
}
