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

package oap.io;

import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import static java.util.Arrays.asList;

public abstract class FileSync implements Runnable {
    private final HashSet<String> protocols;
    private final ArrayList<FileDownloaderListener> listeners = new ArrayList<>();
    protected URI uri;
    protected Path localFile;

    protected FileSync( String... protocols ) {
        this.protocols = new HashSet<>( asList( protocols ) );
    }

    @SneakyThrows
    public static FileSync create( String url, Path localFile ) {
        return create( new URI( url ), localFile );
    }

    @SneakyThrows
    public static FileSync create( URI uri, Path localFile ) {
        var protocol = uri.getScheme();

        final ServiceLoader<FileSync> load = ServiceLoader.load( FileSync.class );
        for( FileSync fs : load ) {
            if( fs.accept( protocol ) ) {
                fs.init( uri, localFile );
                return fs;
            }
        }

        throw new IOException( "unknown protocol: " + protocol );
    }

    public Set<String> getProtocols() {
        return protocols;
    }

    protected boolean accept( String protocol ) {
        return protocols.contains( protocol );
    }

    void init( URI uri, Path localFile ) {
        this.uri = uri;
        this.localFile = localFile;
    }

    public URI getUri() {
        return uri;
    }

    public Path getLocalFile() {
        return localFile;
    }

    protected void fireDownloaded( Path path ) {
        this.listeners.forEach( l -> l.downloaded( path ) );
    }

    protected void fireNotModified() {
        this.listeners.forEach( FileDownloaderListener::notModified );
    }

    public void addListener( FileDownloaderListener listener ) {
        this.listeners.add( listener );
    }

    public void removeListener( FileDownloaderListener listener ) {
        this.listeners.remove( listener );
    }

    @Override
    public synchronized void run() {
        final Optional<Path> downloaded = download();

        if( downloaded.isPresent() ) {
            final Path path = downloaded.get();

            fireDownloaded( path );
        } else
            fireNotModified();
    }

    protected abstract Optional<Path> download();

    public interface FileDownloaderListener {
        void downloaded( Path path );

        default void notModified() {
        }
    }
}
