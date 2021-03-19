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

import lombok.ToString;
import oap.application.ServiceStorage.ErrorStatus;
import oap.util.Result;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * Created by igor.petrenko on 2021-03-16.
 */
@ToString
public abstract class AbstractKernelCommand<TResult> {
    protected final Pattern pattern;

    protected AbstractKernelCommand( String pattern ) {
        this.pattern = Pattern.compile( pattern );
    }

    public boolean matches( Object value ) {
        return value instanceof String && pattern.matcher( ( CharSequence ) value ).matches();
    }

    public abstract Result<TResult, ErrorStatus> get( Object value, Kernel kernel, @Nullable ModuleItem moduleItem, ServiceStorage storage );
}
