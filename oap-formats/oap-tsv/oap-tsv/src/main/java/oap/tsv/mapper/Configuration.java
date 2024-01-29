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

package oap.tsv.mapper;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.tsv.TsvStream;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
@ToString
@Slf4j
public class Configuration {
    public List<Column> columns = new ArrayList<>();
    public boolean hasHeaders = true;
    public boolean skipErrors = true;
    public int columnsNumber = 0;
    public boolean validateInput = false;

    public Configuration() {
    }

    public Configuration( List<Column> columns ) {
        this.columns = columns;
    }

    public Configuration( Column... columns ) {
        this( List.of( columns ) );
    }

    public Configuration withColumnsNumber( int columnsNumber ) {
        this.columnsNumber = columnsNumber;
        return this;
    }

    public Configuration withValidateInput( boolean validateInput ) {
        this.validateInput = validateInput;
        return this;
    }

    public TsvStream configure( TsvStream stream ) {
        var result = stream;
        if( hasHeaders ) result = result.withHeaders();
        if( validateInput ) {
            result = result.filter( line -> {
                if( line.size() != columnsNumber ) {
                    log.error( "erroneous line in configuration '{}'", line );
                    if( skipErrors ) return false;
                    throw new IllegalArgumentException( "erroneous line " + line );
                }
                return true;
            } );
        }
        return result;
    }

    public record Column( int index, String name ) {}
}
