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

package oap.openapi.maven;

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class OpenApiGeneratorPluginTest extends Fixtures {

    public OpenApiGeneratorPluginTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void execute() throws Exception {
        OpenApiGeneratorPlugin mojo = new OpenApiGeneratorPlugin();
        mojo.setOutputPath( "swagger" );
        mojo.setOutputType( "JSON" );
        mojo.setExcludeModules( new ArrayList<>() );

        mojo.execute();
    }

    @Test
    public void execute2() throws Exception {
        OpenApiGeneratorPlugin mojo = new OpenApiGeneratorPlugin();
        mojo.setOutputPath( "swagger" );
        mojo.setOutputType( "JSON" );

        mojo.execute();
    }

    @Test
    public void execute3() throws Exception {
        OpenApiGeneratorPlugin mojo = new OpenApiGeneratorPlugin();
        mojo.setOutputPath( "swagger" );
        mojo.setOutputType( "JSON" );
        mojo.setExcludeModules( Arrays.asList( "oap-ws" ) );

        mojo.execute();
    }
}
