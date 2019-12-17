package oap.io;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class FileWalker {
    private final Path basePath;
    private final String[] paths;
    private final boolean[] wildcard;
    private final boolean[] any;
    private FileWalkerCache cache;

    public FileWalker( Path basePath, String wildcard ) {
        this( basePath, wildcard, new FileWalkerCache() );
    }

    public FileWalker( Path basePath, String wildcard, FileWalkerCache cache ) {
        this.basePath = basePath;
        this.paths = StringUtils.split( wildcard, "/\\" );
        this.cache = cache;
        this.wildcard = new boolean[paths.length];
        this.any = new boolean[paths.length];

        for( int i = 0; i < paths.length; i++ ) {
            final boolean w = StringUtils.indexOfAny( paths[i], '*', '?' ) >= 0;
            this.wildcard[i] = w;
            any[i] = w && paths[i].length() == 1 && paths[i].charAt( 0 ) == '*';
        }
    }

    public void walkFileTree( Consumer<Path> visitor ) {
        walkFileTree( basePath, 0, visitor );
    }

    private void walkFileTree( Path path, int position, Consumer<Path> visitor ) {
        if( wildcard[position] ) {
            if( !cache.isDirectory( path ) ) return;

            var anyPosition = any[position];
            var pathPosition = paths[position];

            try( DirectoryStream<Path> stream = cache.newDirectoryStream(
                path,
                entry -> anyPosition || Files.wildcardMatch( entry.getFileName().toString(), pathPosition ) )
            ) {
                if( position < paths.length - 1 ) {
                    stream.forEach( p -> walkFileTree( p, position + 1, visitor ) );
                } else {
                    stream.forEach( visitor );
                }
            } catch( NoSuchFileException ignore ) {
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        } else {
            var resolve = path.resolve( paths[position] );
            if( position < wildcard.length - 1 ) {
                walkFileTree( resolve, position + 1, visitor );
            } else {
                if( cache.exists( resolve ) )
                    visitor.accept( resolve );
            }
        }
    }
}
