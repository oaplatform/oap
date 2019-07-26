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

import oap.util.Lists;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class KernelProfileTest {
    @BeforeMethod
    public void unregister() {
        Application.unregisterServices();
    }

    @Test
    public void profileName() {
        try( var kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module.yaml" ) ) ) ) {
            startWithProfile( kernel, "profile-name" );

            assertThat( kernel.<TestProfile1>service( "profile1" ) ).isNotNull();
            assertThat( kernel.<TestProfile2>service( "profile2" ) ).isNull();
            assertThat( kernel.<TestProfile3>service( "profile3" ) ).isNotNull();
        }
    }

    @Test
    public void serviceProfiles() {
        var modules = Lists.of( urlOfTestResource( getClass(), "module-profiles.yaml" ) );

        try( var kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "profile-name1" );
            assertThat( kernel.<TestProfile1>service( "profile" ) ).isNotNull();
        }
        try( var kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "profile-name1", "profile-name2" );
            assertThat( kernel.<TestProfile1>service( "profile" ) ).isNull();
        }
        try( var kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "profile-name2" );
            assertThat( kernel.<TestProfile1>service( "profile" ) ).isNull();
        }
        try( var kernel = new Kernel( modules ) ) {
            startWithProfile( kernel );
            assertThat( kernel.<TestProfile1>service( "profile" ) ).isNull();
        }
    }

    private void startWithProfile( Kernel kernel, String... profiles ) {
        var applicationConfiguration = ApplicationConfiguration.load();
        applicationConfiguration.profiles.addAll( asList( profiles ) );
        kernel.start( applicationConfiguration );
    }

    @Test
    public void profileName2() {
        try( var kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module.yaml" ) ) ) ) {
            startWithProfile( kernel, "profile-name-2" );

            assertThat( kernel.<TestProfile1>service( "profile1" ) ).isNull();
            assertThat( kernel.<TestProfile2>service( "profile2" ) ).isNotNull();
            assertThat( kernel.<TestProfile3>service( "profile3" ) ).isNotNull();
        }
    }

    @Test
    public void profile3() {
        try( var kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module3.yaml" ) ) ) ) {
            startWithProfile( kernel, "profile-name" );
            assertThat( kernel.<TestContainer>service( "container" ) ).isNotNull();
        }
    }

    @Test
    public void profile4() {
        try( var kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module4.yaml" ) ) ) ) {
            startWithProfile( kernel, "run" );
            assertThat( kernel.<Object>service( "container" ) ).isInstanceOf( TestContainer2.class );
        }
    }

    @Test
    public void moduleProfiles() {
        try( var kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module-profile.yaml" ) ) ) ) {
            startWithProfile( kernel, "test1" );
            assertThat( kernel.<Object>service( "module-profile" ) ).isInstanceOf( TestProfile1.class );
        }

        try( var kernel = new Kernel( Lists.of( urlOfTestResource( getClass(), "module-profile.yaml" ) ) ) ) {
            startWithProfile( kernel );
            assertThat( kernel.<Object>service( "module-profile" ) ).isNull();
        }
    }

    public interface TestProfile {

    }

    public static class TestProfile1 implements TestProfile {
    }

    public static class TestProfile2 implements TestProfile {
    }

    public static class TestProfile3 implements TestProfile {
    }

    public static class TestContainer {
        public TestContainer( TestProfile profile ) {
            assertThat( profile ).isNotNull();
        }
    }

    public static class TestContainer2 {
        public TestContainer2() {
        }
    }
}

