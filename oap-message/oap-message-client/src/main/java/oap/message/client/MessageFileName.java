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

package oap.message.client;

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;

/**
 * Name ( without extension ) of a persisted message file:
 * <ul>
 *     <li>{@code <md5Hex>} - legacy, version 1, default priority</li>
 *     <li>{@code <md5Hex>-<version>} - default priority</li>
 *     <li>{@code <md5Hex>-<version>-<priority>} - priority may be negative ( {@code abc-1--5} )</li>
 * </ul>
 * A message of the default priority is written without the priority, readable by the older versions.
 */
record MessageFileName( String md5Hex, short version, int priority ) {
    static String of( Message message ) {
        String name = message.getHexMd5() + '-' + message.version;
        return message.priority == MessageSender.DEFAULT_PRIORITY ? name : name + '-' + message.priority;
    }

    /**
     * @throws NumberFormatException invalid version or priority
     */
    static MessageFileName parse( Path messagePath ) throws NumberFormatException {
        String name = FilenameUtils.removeExtension( FilenameUtils.getName( messagePath.toString() ) );

        // md5 hex has no '-'
        String[] parts = name.split( "-", 3 );
        short version = parts.length > 1 ? Short.parseShort( parts[1] ) : 1;
        int priority = parts.length > 2 ? Integer.parseInt( parts[2] ) : MessageSender.DEFAULT_PRIORITY;

        return new MessageFileName( parts[0], version, priority );
    }
}
