/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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

package oap.json;

import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class FormatterTest extends AbstractTest {
    @Test
    public void format() {
        String expected = "{\n\t\"a\": {\n\t\t\"xxxx\": \":y\\\" \\ry []\\n\\t\\t{}\"\n	},\n\t\"b\": [\n\t\t1,\n\t\t{\n\t\t\t\"xx\": null\n\t\t},\n\t\t3\n\t]\n}";
        String result = Formatter.format( "{\"a\": {\"xxxx\": \":y\\\" \\ry []\\n\\t\\t{}\"},\"b\":[1,{\"xx\":null},3]}" );
        assertEquals( expected, result );
        System.out.println( result );
    }
}

