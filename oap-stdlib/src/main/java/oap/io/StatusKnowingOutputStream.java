package oap.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class StatusKnowingOutputStream extends FilterOutputStream {
    private boolean closed = false;

    public StatusKnowingOutputStream( OutputStream out ) {
        super( out );
    }

    public void close() throws IOException {
        out.close();
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void flush() throws IOException {
        if( !closed ) super.flush();
    }
}
