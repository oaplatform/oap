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

package oap.template.runtime;

import oap.template.TemplateAccumulator;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime interpretation context — analogous to {@code Render} for code-generation,
 * but carries live Java objects instead of code fragments.
 *
 * <p>All with-methods produce a new immutable copy; the accumulator and captures
 * are shared mutable references intentionally (they are coordination points).
 */
@SuppressWarnings( "rawtypes" )
public final class RuntimeContext {
    /** The object currently being inspected (analogous to {@code Render.field}). */
    public final Object currentObject;
    /** The root input object (analogous to {@code Render.rootField}). */
    public final Object rootObject;
    /** The accumulator that receives rendered output. */
    public final TemplateAccumulator acc;
    /** Named range-loop variables (e.g. {@code $item}, {@code $k}). */
    public final Map<String, Object> rangeVars;
    /**
     * One-element array shared with the caller of {@code withBooleanCapture()}.
     * {@code AstRenderCaptureBoolean} writes into {@code [0]} when reached.
     * {@code null} when not inside a condition-path evaluation.
     */
    public final boolean[] booleanCapture;
    /**
     * One-element array shared with the caller of {@code withScopeCapture()}.
     * {@code AstRenderCaptureScope} writes into {@code [0]} when reached.
     * {@code null} when not resolving a scope/collection path.
     */
    public final Object[] scopeCapture;
    /**
     * One-element array shared with the caller of {@code withTryEmpty()}.
     * Set to {@code true} by {@code AstRenderNullable/Optional} when the null branch is taken.
     * {@code null} when not inside an Or-candidate evaluation.
     */
    public final boolean[] tryEmpty;

    public RuntimeContext( Object currentObject, Object rootObject,
                           TemplateAccumulator acc,
                           Map<String, Object> rangeVars,
                           boolean[] booleanCapture,
                           Object[] scopeCapture,
                           boolean[] tryEmpty ) {
        this.currentObject = currentObject;
        this.rootObject = rootObject;
        this.acc = acc;
        this.rangeVars = rangeVars;
        this.booleanCapture = booleanCapture;
        this.scopeCapture = scopeCapture;
        this.tryEmpty = tryEmpty;
    }

    /** Create the initial context for interpreting a template against {@code obj}. */
    public static RuntimeContext root( Object obj, TemplateAccumulator acc ) {
        return new RuntimeContext( obj, obj, acc, Map.of(), null, null, null );
    }

    public RuntimeContext withCurrentObject( Object obj ) {
        return new RuntimeContext( obj, rootObject, acc, rangeVars, booleanCapture, scopeCapture, tryEmpty );
    }

    @SuppressWarnings( "unchecked" )
    public RuntimeContext withAcc( TemplateAccumulator newAcc ) {
        return new RuntimeContext( currentObject, rootObject, newAcc, rangeVars, booleanCapture, scopeCapture, tryEmpty );
    }

    public RuntimeContext withRangeVar( String name, Object value ) {
        var newVars = new HashMap<>( rangeVars );
        newVars.put( name, value );
        return new RuntimeContext( currentObject, rootObject, acc, newVars, booleanCapture, scopeCapture, tryEmpty );
    }

    public RuntimeContext withBooleanCapture( boolean[] capture ) {
        return new RuntimeContext( currentObject, rootObject, acc, rangeVars, capture, scopeCapture, tryEmpty );
    }

    public RuntimeContext withScopeCapture( Object[] capture ) {
        return new RuntimeContext( currentObject, rootObject, acc, rangeVars, booleanCapture, capture, tryEmpty );
    }

    public RuntimeContext withTryEmpty( boolean[] tryEmptyFlag ) {
        return new RuntimeContext( currentObject, rootObject, acc, rangeVars, booleanCapture, scopeCapture, tryEmptyFlag );
    }
}
