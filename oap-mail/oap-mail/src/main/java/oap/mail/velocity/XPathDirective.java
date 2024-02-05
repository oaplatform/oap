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
package oap.mail.velocity;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.w3c.dom.Node.ATTRIBUTE_NODE;
import static org.w3c.dom.Node.CDATA_SECTION_NODE;
import static org.w3c.dom.Node.COMMENT_NODE;
import static org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE;
import static org.w3c.dom.Node.TEXT_NODE;

@Slf4j
public class XPathDirective extends Directive {
    private static final List<Short> TEXT_NODES = List.of( TEXT_NODE, ATTRIBUTE_NODE, COMMENT_NODE, CDATA_SECTION_NODE, PROCESSING_INSTRUCTION_NODE );

    public String getName() {
        return "xpath";
    }

    public int getType() {
        return LINE;
    }

    public boolean render( InternalContextAdapter context, Writer writer, org.apache.velocity.runtime.parser.node.Node node ) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        String var = node.jjtGetChild( 0 ).getFirstTokenImage().substring( 1 );
        Node document = ( Node ) node.jjtGetChild( 1 ).value( context );
        String xpath = String.valueOf( node.jjtGetChild( 2 ).value( context ) );
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Node element = ( Node ) xPath.evaluate( xpath, document, XPathConstants.NODE );
            if( element != null )
                if( TEXT_NODES.contains( element.getNodeType() ) )
                    context.put( var, element.getTextContent() );
                else context.put( var, element );
            else log.warn( "nothing found for xpath: " + xpath );
        } catch( Exception e ) {
            throw new IOException( "cannot evaluate xpath: " + xpath, e );
        }
        return true;
    }
}
