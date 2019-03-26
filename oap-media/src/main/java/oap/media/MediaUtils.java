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
import org.apache.tika.parser.AutoDetectParser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class MediaUtils {
    private static final AutoDetectParser AUTO_DETECT_PARSER = new AutoDetectParser();
    private static final Detector DETECTOR = AUTO_DETECT_PARSER.getDetector();

    @SneakyThrows
    public static String getContentType( Path file, Optional<String> fileName ) {
        log.trace( "file = {}", file );

        try( var is = new FileInputStream( file.toFile() );
             var bis = new BufferedInputStream( is ) ) {
            var md = new Metadata();
            md.add( Metadata.RESOURCE_NAME_KEY, fileName.orElse( file.toString() ) );
            var mediaType = DETECTOR.detect( bis, md );
            return mediaType.toString();
        }
    }
}
