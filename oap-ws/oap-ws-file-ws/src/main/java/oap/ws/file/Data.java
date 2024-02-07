/*
  The MIT License (MIT)
  <p>
  Copyright (c) Open Application Platform Authors
  <p>
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  <p>
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.
  <p>
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */

package oap.ws.file;

import oap.io.MimeTypes;
import oap.util.Pair;

import java.util.Base64;

import static oap.util.Strings.split;

/**
 * todo create separate fields
 */
public class Data {
    public String mimeType;
    public String content;
    public String name;

    public Data() {
    }

    public Data( String name, String mimeType, String content ) {
        this.name = name;
        this.mimeType = mimeType;
        this.content = content;
    }

    public void setBase64Data( String base64Data ) {
        Pair<String, String> data = split( base64Data, ";" );
        this.mimeType = split( data._1, ":" )._2;
        this.content = split( data._2, "," )._2;
    }

    public byte[] decoded() {
        return Base64.getDecoder().decode( content );
    }

    public String extension() {
        return MimeTypes.extensionOf( mimeType ).orElse( "bin" );
    }

    public String nameOrConstruct( String prefix ) {
        return name != null ? name : prefix + "." + extension();
    }
}
