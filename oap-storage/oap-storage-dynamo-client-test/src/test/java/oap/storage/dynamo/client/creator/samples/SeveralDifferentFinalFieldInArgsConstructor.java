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

package oap.storage.dynamo.client.creator.samples;

import lombok.Data;

@Data
public class SeveralDifferentFinalFieldInArgsConstructor {
    private final String finalField1;
    private final boolean finalField2;
    private final Integer finalField3;
    private String field;

    public SeveralDifferentFinalFieldInArgsConstructor( String finalField1, Boolean finalField2, int finalField3 ) {
        this.finalField1 = finalField1 + "_second";
        this.finalField2 = finalField2;
        this.finalField3 = finalField3;
    }

    public SeveralDifferentFinalFieldInArgsConstructor( String finalField1, boolean finalField2, Integer finalField3 ) {
        this.finalField1 = finalField1;
        this.finalField2 = finalField2;
        this.finalField3 = finalField3;
    }
}
