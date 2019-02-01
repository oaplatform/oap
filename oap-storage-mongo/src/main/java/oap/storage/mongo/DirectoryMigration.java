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

package oap.storage.mongo;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.Files;
import oap.util.Maps;
import org.bson.Document;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

@Slf4j
public class DirectoryMigration implements Migration {
    private static final FindOneAndUpdateOptions UPSERT = new FindOneAndUpdateOptions().upsert( true );

    private final Path directory;
    public HashMap<String, String> variables = new HashMap<>();

    public DirectoryMigration( Path directory ) {
        this.directory = directory;
    }

    private void nextMigration( MongoDatabase db, int fromVersion, String functions ) {
        log.info( "directory {} ...", directory );
        val versionDirectory = directory.resolve( String.valueOf( fromVersion ) );
        log.debug( "try version directory {} ...", versionDirectory );
        if( java.nio.file.Files.isDirectory( versionDirectory ) ) {
            log.info( "{} exists", versionDirectory );
            for( val file : Files.fastWildcard( versionDirectory, "*.js" ) ) {
                log.info( "file {} ...", file );

                val script = Files.readString( file );

                val vars = variables
                    .entrySet()
                    .stream()
                    .map( entry -> "var " + entry.getKey() + " = " + entry.getValue() + ";" )
                    .collect( Collectors.joining( "\n" ) );

                val eval = new Document( "eval", "function() {\n" + functions + "\n" + vars + "\n" + script + "\n}\n" );
                log.trace( "eval = {}", eval );
                final Document response = db.runCommand( eval );
                val ok = response.getDouble( "ok" );
                if( ok < 1.0 ) {
                    log.debug( "response ok={}, code={}, errmsg={}", response.get( "ok" ), response.get( "code" ), response.get( "errmsg" ) );
                    throw new MigrationExceptin( response.get( "code" ) + ": " + response.get( "errmsg" ) );
                }

                log.info( "file {} ... Done", file );
            }

            log.info( "directory {} ... Done", versionDirectory );
        }
    }

    @Override
    public void run( MongoDatabase database ) {
        val toVersion = Integer.parseInt( Files.readString( directory.resolve( "version.txt" ) ) );
        val versionCollection = database.getCollection( "version" );
        Document versionDocument = versionCollection.find( eq( "_id", "version" ) ).first();
        if( versionDocument == null ) {
            versionDocument = new Document( Maps.of2( "_id", "version", "value", 0 ) );
        }
        int fromVersion = versionDocument.getInteger( "value" );

        log.info( "migration version = {}, database version = {}", toVersion, fromVersion );

        String func = "";
        final Path functions = directory.resolve( "functions.js" );
        if( java.nio.file.Files.exists( functions ) ) {
            func = Files.readString( functions );
        }

        while( toVersion >= fromVersion + 1 ) {
            log.info( "migration from {} to {}", fromVersion, fromVersion + 1 );
            fromVersion = fromVersion + 1;
            nextMigration( database, fromVersion, func );
            versionCollection.findOneAndUpdate( eq( "_id", "version" ), set( "value", fromVersion ), UPSERT );
        }
    }
}
