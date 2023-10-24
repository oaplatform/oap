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

package oap.template.render;

import lombok.ToString;

@ToString( callSuper = true )
public class AstRenderRoot extends AstRender {
    public AstRenderRoot( TemplateType parentType ) {
        super( parentType );
    }

    @Override
    public void render( Render render ) {
        var className = type.getTypeName().replace( '$', '.' );

        var templateAccumulatorClassName = render.templateAccumulator.getClass().getTypeName().replace( '$', '.' );
        render.append( """
                package oap.template;

                import oap.util.Strings;

                import java.util.*;
                import oap.util.function.TriConsumer;
                import java.util.function.Supplier;
                import java.util.function.BooleanSupplier;
                import com.google.common.base.CharMatcher;

                public class\s""" ).append( render.nameEscaped() )
            .append( " implements TriConsumer<%s, Map<String, Supplier<String>>, %s>", className, templateAccumulatorClassName )
            .append( """
                {

                 @Override
                 public void accept(\s""".indent( 1 ) ).append( className ).append( " s, Map<String, Supplier<String>> m, " ).append( templateAccumulatorClassName ).append( " acc ) {\n" );

        Render childRender = render.tabInc().tabInc().tabInc().withField( "s" ).withTemplateAccumulatorName( "acc" ).withParentType( type );

        children.forEach( child -> child.render( childRender ) );

        render.append( """
              }
            }""".stripIndent() );
    }
}
