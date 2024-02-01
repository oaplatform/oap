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
package oap.application.cli;


import oap.util.Pair;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class Cli {
    private final ArrayList<Group> groups = new ArrayList<>();

    public static Cli create() {
        return new Cli();
    }

    public Cli group( String name, Consumer<Map<String, Object>> action, Option<?>... options ) {
        Group group = new Group( name, action, options );
        groups.add( group );
        return this;
    }

    public void act( String[] args ) {
        act( String.join( " ", args ) );
    }

    public void act( String args ) {
        try {
            List<Pair<String, String>> parameters = new CliParser( new CommonTokenStream( new CliLexer( new ANTLRInputStream( args ) ) ) ).parameters().list;
            Optional<Group> group = groups.stream().filter( g -> g.matches( parameters ) ).findFirst();
            if( group.isPresent() ) group.get().act( parameters ).ifFailure( failure -> {
                System.out.println( "Error: " + failure );
                printHelp();
            } );
            else printHelp();
        } catch( RecognitionException e ) {
            System.err.println( "Error: " + e.getMessage() );
            printHelp();
        }
    }

    private void printHelp() {
        for( Group group : groups ) {
            System.out.println( group.name );
            for( Option<?> option : group.options() ) {
                System.out.println( "\t\t" + option );
            }
        }
    }
}
