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

package oap.application.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.batch.JobExecutionExitCodeGenerator;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringKernelTest {
    @Test
    public void boot() {
        SpringBoot.main( new String[] {
            "--config=classpath:oap/application/spring/SpringKernelTest/application.conf" } );

        SpringKernel springKernel = SpringBoot.applicationContext.getBean( SpringKernel.class );
        assertThat( springKernel ).isNotNull();
        Optional<TestService> service = springKernel.kernel.service( "test" );
        assertThat( service ).isPresent().get()
            .satisfies( s -> assertThat( SpringBoot.applicationContext.getBean( "test" ) ).isSameAs( s ) );

        SpringApplication.exit( SpringBoot.applicationContext, new JobExecutionExitCodeGenerator() );
    }

    @SuppressWarnings( "unused" )
    public static class TestService {

    }


}
