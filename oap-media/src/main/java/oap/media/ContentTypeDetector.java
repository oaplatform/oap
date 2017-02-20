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

package oap.media;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Created by igor.petrenko on 20.02.2017.
 */
@Slf4j
public class ContentTypeDetector {
    @SneakyThrows
    public static String get( Path file, Optional<String> fileName ) {
        log.trace( "file = {}", file );

        try( InputStream is = new FileInputStream( file.toFile() );
             BufferedInputStream bis = new BufferedInputStream( is ) ) {
            AutoDetectParser parser = new AutoDetectParser();
            Detector detector = parser.getDetector();
            Metadata md = new Metadata();
            md.add( Metadata.RESOURCE_NAME_KEY, fileName.orElse( file.toString() ) );
            MediaType mediaType = detector.detect( bis, md );
            return mediaType.toString();
        }
    }
}
