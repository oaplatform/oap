package oap.io;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class KafkaLZ4BlockUtils {

    /**
     * Read an unsigned integer stored in little-endian format from the {@link InputStream}.
     *
     * @param in The stream to read from
     * @return The integer read (MUST BE TREATED WITH SPECIAL CARE TO AVOID SIGNEDNESS)
     */
    public static int readUnsignedIntLE( InputStream in ) throws IOException {
        return in.read()
            | ( in.read() << 8 )
            | ( in.read() << 8 * 2 )
            | ( in.read() << 8 * 3 );
    }


    /**
     * Read an unsigned integer stored in little-endian format from a byte array
     * at a given offset.
     *
     * @param buffer The byte array to read from
     * @param offset The position in buffer to read from
     * @return The integer read (MUST BE TREATED WITH SPECIAL CARE TO AVOID SIGNEDNESS)
     */
    public static int readUnsignedIntLE( byte[] buffer, int offset ) {
        int off = offset;
        return ( buffer[off++] )
            | ( buffer[off++] << 8 )
            | ( buffer[off++] << 8 * 2 )
            | ( buffer[off] << 8 * 3 );
    }

    /**
     * Write an unsigned integer in little-endian format to the {@link OutputStream}.
     *
     * @param out   The stream to write to
     * @param value The value to write
     */
    public static void writeUnsignedIntLE( OutputStream out, int value ) throws IOException {
        out.write( value );
        out.write( value >>> 8 );
        out.write( value >>> 8 * 2 );
        out.write( value >>> 8 * 3 );
    }

    /**
     * Write an unsigned integer in little-endian format to a byte array
     * at a given offset.
     *
     * @param buffer The byte array to write to
     * @param offset The position in buffer to write to
     * @param value  The value to write
     */
    public static void writeUnsignedIntLE( byte[] buffer, int offset, int value ) {
        int off = offset;
        buffer[off++] = ( byte ) value;
        buffer[off++] = ( byte ) ( value >>> 8 );
        buffer[off++] = ( byte ) ( value >>> 8 * 2 );
        buffer[off] = ( byte ) ( value >>> 8 * 3 );
    }


}

