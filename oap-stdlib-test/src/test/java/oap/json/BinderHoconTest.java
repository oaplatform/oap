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

package oap.json;

import com.typesafe.config.impl.ConfigImpl;
import org.testng.annotations.Test;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class BinderHoconTest {
    @Test
    public void numericKeysAndNulls() {
        var mapper = JsonMapper.builder( new OapHoconFactory() ).build();
        var tree = mapper.readTree( "list { 10 = ten, 2 = two, 0 = zero }, empty = {}, nil = null, flag = true, number = 1.5" );
        assertThat( tree.get( "list" ).toString() ).isEqualTo( "[\"zero\",\"two\",\"ten\"]" );
        assertThat( tree.get( "empty" ).isObject() ).isTrue();
        assertThat( tree.get( "nil" ).isNull() ).isTrue();
        assertThat( tree.get( "flag" ).booleanValue() ).isTrue();
        assertThat( tree.get( "number" ).doubleValue() ).isEqualTo( 1.5 );
    }

    @Test
    public void relativeIncludeAndSubstitution() {
        var url = getClass().getResource( "BinderHoconTest/main.conf" );
        var bean = Binder.hoconWithoutSystemProperties.unmarshal( BeanPattern.class, url );
        assertThat( bean.list ).containsExactly( "included", "resolved" );
    }

    @Test
    public void fallbackSurvivesFactoryCopyAndRebuild() {
        var factory = new HoconFactoryWithFallback( false, org.slf4j.LoggerFactory.getLogger( getClass() ), Map.of( "answer", 42 ) );
        for( var copy : List.of( factory, factory.copy(), factory.rebuild().build() ) ) {
            var tree = JsonMapper.builder( copy ).build().readTree( "answer = 1, original = true, copied = ${answer}" );
            assertThat( tree.get( "answer" ).intValue() ).isEqualTo( 42 );
            assertThat( tree.get( "copied" ).intValue() ).isEqualTo( 42 );
            assertThat( tree.get( "original" ).booleanValue() ).isTrue();
        }
    }

    @Test
    public void sourceOwnershipAndParseFailure() {
        for( boolean autoClose : List.of( true, false ) ) {
            var mapper = JsonMapper.builder( new OapHoconFactory() ).configure( StreamReadFeature.AUTO_CLOSE_SOURCE, autoClose ).build();
            AtomicBoolean closed = new AtomicBoolean();
            StringReader reader = new StringReader( "value = [" ) {
                @Override
                public void close() {
                    closed.set( true );
                    super.close();
                }
            };
            assertThatThrownBy( () -> mapper.readTree( reader ) ).isInstanceOf( com.typesafe.config.ConfigException.Parse.class );
            assertThat( closed.get() ).isEqualTo( autoClose );
        }
    }

    @Test
    public void sourceLocationIncludesUrlAndLine() throws IOException {
        var url = getClass().getResource( "BinderHoconTest/main.conf" );
        var mapper = JsonMapper.builder( new OapHoconFactory() ).build();
        try( var reader = OapHoconFactory.open( url ); var parser = mapper.createParser( reader ) ) {
            while( parser.nextToken() != null ) {
                if( "source".equals( parser.currentName() ) ) {
                    assertThat( parser.currentLocation().getLineNr() ).isEqualTo( 4 );
                    assertThat( parser.currentLocation().toString() ).contains( "main.conf" );
                    return;
                }
            }
        }
        throw new AssertionError( "Source property was not read" );
    }

    @Test
    public void pattern() {
        String pattern = "{test = \"[^a]+\"}";
        BeanPattern obj = Binder.hocon.<BeanPattern>unmarshal( BeanPattern.class, pattern );

        assertThat( obj.test.pattern() ).isEqualTo( "[^a]+" );
    }

    @Test
    public void envList() {
        String json = "{list = [${?LIST_ENV}]}";
        System.setProperty( "LIST_ENV", "1a,2a" );

        ConfigImpl.reloadEnvVariablesConfig();
        ConfigImpl.reloadSystemPropertiesConfig();

        BeanPattern obj = Binder.hocon.<BeanPattern>unmarshal( BeanPattern.class, json );

        assertThat( obj.list ).isEqualTo( List.of( "1a,2a" ) );
    }

    public static class BeanPattern {
        public Pattern test;
        public List<String> list;
    }
}
