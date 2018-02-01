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
package oap.application;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

@ToString
@EqualsAndHashCode
public class ServiceTwo implements Hello, ActionListener {
    public ServiceOne one;
    int j;
    boolean started;
    String test;
    List<TestBean> beans = new ArrayList<>();


    public ServiceTwo( ServiceOne one ) {
        this.one = one;
    }

    public void start() {
        System.out.println( "started" );
        started = true;
    }

    @Override
    public List<TestBean> hello( List<TestBean> beans ) {
        this.beans = new ArrayList<>( beans );
        return this.beans;
    }

    @Override
    public void voidMethod( String test ) {
        this.test = test;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {}
}
