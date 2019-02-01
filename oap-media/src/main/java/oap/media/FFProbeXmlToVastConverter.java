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

package oap.media;

import lombok.SneakyThrows;
import oap.util.Throwables;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class FFProbeXmlToVastConverter {
    private static final TransformerFactory transformerFactory;
    private static final Templates templates;

    static {
        try {
            transformerFactory = TransformerFactory.newInstance();
            templates = transformerFactory.newTemplates( new StreamSource(
                FFProbeXmlToVastConverter.class.getResourceAsStream( "/ffprobe-xml-to-vast.xslt" )
            ) );
        } catch( TransformerConfigurationException e ) {
            throw Throwables.propagate( e );
        }
    }

    @SneakyThrows
    public static String convert( String xml, String id, String contentType ) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        final Document ffprobeXml = documentBuilder.parse( new InputSource( new StringReader( xml ) ) );

        final StringWriter writer = new StringWriter();
        final Transformer transformer = templates.newTransformer();
        transformer.setParameter( "id", id );
        transformer.setParameter( "contentType", contentType );
        transformer.transform( new DOMSource( ffprobeXml ), new StreamResult( writer ) );
        return writer.toString();
    }
}
