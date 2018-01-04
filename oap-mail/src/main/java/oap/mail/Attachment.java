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
package oap.mail;


import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;

@ToString
public class Attachment {
    private final String contentId;
    private final String file;
    private final String contentType;
    private final String name;
    private String content;

    public Attachment( String contentType, String content ) {
        this( contentType, content, null, null, null );
    }

    @JsonCreator
    public Attachment( String contentType, String content, String contentId, String file, String name ) {
        checkArgument( contentType.startsWith( "text/" ) || file != null, "contentType.startsWith( text/ ) || file != null" );
        this.contentType = contentType;
        this.content = content;
        this.contentId = contentId;
        this.file = file;
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent( String content ) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentId() {
        return contentId;
    }

    public String getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

}
