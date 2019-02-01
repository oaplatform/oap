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

package oap.media.postprocessing;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.media.FFProbeXmlToVastConverter;
import oap.media.Media;
import oap.media.MediaContext;
import oap.media.MediaInfo;
import oap.media.MediaProcessing;
import oap.media.MediaUtils;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Slf4j
public class VastMediaProcessing implements MediaProcessing {
    private final List<String> command;
    private final long timeout;

    public VastMediaProcessing( List<String> command, long timeout ) {
        this.command = command;
        this.timeout = timeout;
    }

    @Override
    @SneakyThrows
    public Media process( Media media, MediaInfo mediaInfo, MediaContext mediaContext ) {
        log.debug( "ffprobe {}...", media.path );
        final ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream( true );
        final List<String> cmd = command.stream().map( i -> i.replace( "{FILE}", media.path.toString() ) ).collect( toList() );
        builder.command( cmd );

        log.trace( "cmd = {}", cmd );
        Process p = builder.start();
        try {
            final String xml = IOUtils.toString( p.getInputStream(), StandardCharsets.UTF_8 );
            log.trace( "ffprobe: {}", xml );

            final String contentType = MediaUtils.getContentType( media.path, Optional.of( media.name ) );

            final String vast = FFProbeXmlToVastConverter.convert( xml, media.id, contentType );
            mediaInfo.put( "vast", vast );
            mediaInfo.put( "Content-Type", contentType );
            p.waitFor( timeout, TimeUnit.MILLISECONDS );
        } finally {
            p.destroy();
        }

        return media;
    }
}
