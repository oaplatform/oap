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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.media.Media;
import oap.media.MediaInfo;
import oap.media.MediaProcessing;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by igor.petrenko on 10.02.2017.
 */
@Slf4j
public class FFProbeMediaProcessing implements MediaProcessing {
    private final List<String> command;
    private final long timeout;

    public FFProbeMediaProcessing( List<String> command, long timeout ) {
        this.command = command;
        this.timeout = timeout;
    }

    @Override
    @SneakyThrows
    public Media process( Media media, MediaInfo mediaInfo ) {
        log.debug( "ffprobe {}...", media.path );
        final ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream( true );
        final ArrayList<String> cmd = new ArrayList<>( command );
        cmd.add( media.path.toString() );
        builder.command( cmd );
        Process p = builder.start();
        try {
            final String json = IOUtils.toString( p.getInputStream(), StandardCharsets.UTF_8 );
            final Map<String, Object> info = Binder.json.unmarshal( new TypeReference<Map<String, Object>>() {}, json );
            mediaInfo.put( "ffprobe", info );
            p.waitFor( timeout, TimeUnit.MILLISECONDS );
        } finally {
            p.destroy();
        }

        return media;
    }
}
