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


import org.testng.annotations.Test;

import static oap.mail.Template.Type.TEXT;
import static org.testng.Assert.assertEquals;

public class TemplateTest {

    @Test
    public void buildMessage() {
        Template template = new Template( Template.Type.TEXT, "--subject--\n$subj is evaluated\n--body--\nsome $body variable" );
        template.bind( "subj", "subject" );
        template.bind( "body", "content" );
        Message message = template.buildMessage();
        assertEquals( "subject is evaluated", message.getSubject() );
        assertEquals( "some content variable", message.getBody() );
    }

    @Test
    public void callingJava() {
        Template template = new Template( Template.Type.TEXT, "--subject--\n$subj is evaluated\n--body--\nsome ${body.replaceAll(' ','%20')} variable" );
        template.bind( "subj", "subject" );
        template.bind( "body", "cont ent" );
        Message message = template.buildMessage();
        assertEquals( "subject is evaluated", message.getSubject() );
        assertEquals( "some cont%20ent variable", message.getBody() );
    }

    @Test
    public void publicFields() {
        Template template = new Template( TEXT, "--subject--\nsubj\n--body--\n${bean.pub}" );
        template.bind( "bean", new Bean() );
        Message message = template.buildMessage();
        assertEquals( "pub", message.getBody() );
    }

    public static class Bean {
        public String pub = "pub";
    }
}
