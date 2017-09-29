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

package oap.zabbix.logback;

import lombok.val;
import oap.json.Binder;
import oap.zabbix.Request;
import org.apache.commons.lang3.NotImplementedException;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by igor.petrenko on 29.09.2017.
 */
public class ZabbixRequest implements Externalizable {
    private static final byte header[] = { 'Z', 'B', 'X', 'D', '\1' };

    public final Request request;

    public ZabbixRequest( Request request ) {
        this.request = request;
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {
        out.write( header );

        val bRequest = Binder.json.marshal( request ).getBytes();
        val bRequestLength = bRequest.length;

        out.write( new byte[] { ( byte ) ( bRequestLength & 0xFF ),
            ( byte ) ( ( bRequestLength >> 8 ) & 0x00FF ),
            ( byte ) ( ( bRequestLength >> 16 ) & 0x0000FF ),
            ( byte ) ( ( bRequestLength >> 24 ) & 0x000000FF ),
            '\0', '\0', '\0', '\0' } );
        out.write( bRequest );

        out.flush();
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
        throw new NotImplementedException( "" );
    }
}
