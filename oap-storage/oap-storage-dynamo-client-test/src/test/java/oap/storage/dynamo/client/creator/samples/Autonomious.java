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
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@ToString( exclude = "listOfBinaries", callSuper = true )
public class Autonomious extends Supernatural {
    private static String staticVar;
    private String id;
    private final String finalVar;
    public Long publicLongVar;
    public Integer publicIntVar;
    public Float publicFloatVar;
    public Double publicDoubleVar;
    public String publicStringVar;
    public Boolean publicBooleanVar;
    public Number publicNumberVar;
    public byte[] publicBytesVar;

    private Integer intVar;
    private Long longVar;
    private Float floatVar;
    private Double doubleVar;
    private String stringVar;
    private Number numberVar;
    private Boolean booleanVar;
    private byte[] bytesVar;
    private List<String> listOfStrings;
    private List<byte[]> listOfBinaries;
    private List<Integer> listOfIntegers;
    private Map<String, Object> mapOfObjects;

    public Autonomious( String finalVar ) {
        this.finalVar = finalVar;
    }

}
