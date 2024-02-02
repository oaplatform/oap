package oap.mail.velocity;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;

import java.io.IOException;
import java.io.Writer;

public class FormatDateDirective extends Directive {
    @Override
    public String getName() {
        return "formatDate";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render( InternalContextAdapter context, Writer writer, Node node ) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        ReadableInstant date = ( ReadableInstant ) node.jjtGetChild( 0 ).value( context );
        String pattern = node.jjtGetChild( 1 ).literal();
        pattern = pattern.substring( 1, pattern.length() - 1 );
        writer.write( DateTimeFormat.forPattern( pattern ).print( date ) );
        return true;
    }
}
