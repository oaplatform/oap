package oap.io;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Created by Igor Petrenko on 14.01.2016.
 */
public class FileWalker {
   private final Path basePath;
   private final String[] paths;
   private final boolean[] wildcard;

   public FileWalker( Path basePath, String wildcard ) {
      this.basePath = basePath;
      this.paths = StringUtils.split( wildcard, "/\\" );
      this.wildcard = new boolean[paths.length];

      for( int i = 0; i < paths.length; i++ ) {
         this.wildcard[i] = StringUtils.indexOfAny( paths[i], '*', '?' ) >= 0;
      }
   }

   public void walkFileTree( Consumer<Path> visitor ) {
      walkFileTree( basePath, 0, visitor );
   }

   private void walkFileTree( Path path, int position, Consumer<Path> visitor ) {
      if( wildcard[position] ) {
         if( !java.nio.file.Files.isDirectory( path ) ) return;

         try( DirectoryStream<Path> stream = java.nio.file.Files.newDirectoryStream( path, entry -> {
            return FilenameUtils.wildcardMatch( entry.getFileName().toString(), paths[position] );
         } ) ) {
            if( position < paths.length - 1 ) {
               stream.forEach( p -> walkFileTree( p, position + 1, visitor ) );
            } else {
               stream.forEach( visitor::accept );
            }
         } catch( NoSuchFileException ignore ) {
         } catch( IOException e ) {
            throw new UncheckedIOException( e );
         }
      } else {
         final Path resolve = path.resolve( paths[position] );
         if( position < wildcard.length - 1 ) {
            walkFileTree( resolve, position + 1, visitor );
         } else {
            if( java.nio.file.Files.exists( resolve ) )
               visitor.accept( resolve );
         }
      }
   }
}
