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
package oap.mail;

import oap.io.Files;
import oap.io.Resources;
import oap.mail.message.MessageParser;
import oap.mail.message.TextMessageParser;
import oap.mail.message.xml.XmlMessageParser;
import oap.mail.velocity.VelocityTemplateTransformer;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Template {
    private Type type;
    private final String content;
    private Map<String, Object> parameters = new HashMap<>();
    private static VelocityTemplateTransformer transformer = new VelocityTemplateTransformer();

    public Template( Type type, String content ) {
        this.type = type;
        this.content = content;
    }

    public void bind( String name, Object value ) {
        parameters.put( name, value );
    }

    public Message buildMessage() {
        return type.parser.parse( transformer.transform( this ) );
    }

    public void clear() {
        parameters.clear();
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String getContent() {
        return content;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        TEXT( ".mail", new TextMessageParser() ), XML( ".xmail", new XmlMessageParser() );
        public final String suffix;
        public final MessageParser parser;

        Type( String suffix, MessageParser parser ) {
            this.suffix = suffix;
            this.parser = parser;
        }

        public static Type of( Path path ) {
            for( Type type : values() ) if( path.toString().endsWith( type.suffix ) ) return type;
            throw new IllegalArgumentException( path.toString() );
        }
    }

    public static Optional<Template> of( String name ) {
        return Stream.of( Type.values() )
            .map( type -> Resources.readString( Template.class, name + type.suffix )
                .map( content -> new Template( type, content ) ) )
            .filter( Optional::isPresent )
            .map( Optional::get )
            .findFirst();
    }

    public static Optional<Template> of( Path path ) {
        return path.toFile().exists() ? Optional.of( new Template( Type.of( path ), Files.readString( path ) ) )
            : Optional.empty();
    }
}
