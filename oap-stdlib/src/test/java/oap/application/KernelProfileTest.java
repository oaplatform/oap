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

import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class KernelProfileTest {
    @Test
    public void profileName() {
        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module.yaml" ) ) ) ) {
            startWithProfile( kernel, "test-profile", "profile-name" );

            assertThat( kernel.<TestProfile1>service( "*.profile1" ) ).isPresent();
            assertThat( kernel.<TestProfile2>service( "*.profile2" ) ).isNotPresent();
            assertThat( kernel.<TestProfile3>service( "*.profile3" ) ).isPresent();
        }
    }

    @Test
    public void serviceProfiles() {
        var modules = List.of( urlOfTestResource( getClass(), "module-profiles.yaml" ) );

        try( var kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "test-profile", "profile-name1" );
            assertThat( kernel.<TestProfile1>service( "*.profile" ) ).isPresent();
        }
        try( var kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "test-profile", "profile-name1", "profile-name2" );
            assertThat( kernel.<TestProfile1>service( "*.profile" ) ).isNotPresent();
        }
        try( var kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "test-profile", "profile-name2" );
            assertThat( kernel.<TestProfile1>service( "*.profile" ) ).isNotPresent();
        }
        try( var kernel = new Kernel( modules ) ) {
            startWithProfile( kernel, "test-profile" );
            assertThat( kernel.<TestProfile1>service( "*.profile" ) ).isNotPresent();
        }
    }

    private void startWithProfile( Kernel kernel, String bootMain, String... profiles ) {
        var applicationConfiguration = ApplicationConfiguration.load( Map.of( "boot.main", bootMain ) );
        applicationConfiguration.profiles.addAll( asList( profiles ) );
        kernel.start( applicationConfiguration );
    }

    @Test
    public void profileName2() {
        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module.yaml" ) ) ) ) {
            startWithProfile( kernel, "test-profile", "profile-name-2" );

            assertThat( kernel.<TestProfile1>service( "*.profile1" ) ).isNotPresent();
            assertThat( kernel.<TestProfile2>service( "*.profile2" ) ).isPresent();
            assertThat( kernel.<TestProfile3>service( "*.profile3" ) ).isPresent();
        }
    }

    @Test
    public void profile3() {
        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module3.conf" ) ) ) ) {
            startWithProfile( kernel, "test-profile3", "profile-name" );
            assertThat( kernel.<TestContainer>service( "*.container" ) ).isPresent();
        }
    }

    @Test
    public void profile4() {
        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module4.yaml" ) ) ) ) {
            startWithProfile( kernel, "test-profile4", "run" );
            assertThat( kernel.service( "*.container" ) ).isPresent().get().isInstanceOf( TestContainer2.class );
        }
    }

    @Test
    public void profile5() {
        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module5.yaml" ) ) ) ) {
            startWithProfile( kernel, "test-profile5", "run" );
            assertThat( kernel.service( "*.container" ) ).isPresent().get().isInstanceOf( TestContainer3.class );
            assertThat( kernel.<TestContainer3>service( "*.container" ).get().profile ).isInstanceOf( TestProfile2.class );
        }
    }

    @Test
    public void profile6() {
        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module6.conf" ) ) ) ) {
            startWithProfile( kernel, "test-profile6", "run" );
            assertThat( kernel.<TestContainer>service( "*.container1" ).get().profile ).isInstanceOf( TestProfile1.class );
        }
    }

    @Test
    public void moduleProfiles() {
        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module-profile.yaml" ) ) ) ) {
            startWithProfile( kernel, "test-module-profile", "test1" );
            assertThat( kernel.service( "*.module-profile" ) ).isPresent().get().isInstanceOf( TestProfile1.class );
        }

        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module-profile.yaml" ) ) ) ) {
            startWithProfile( kernel, "test-module-profile" );
            assertThat( kernel.service( "*.module-profile" ) ).isNotPresent();
        }
    }

    @Test
    public void testProfileNameWithVariableFix() {
        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module-name-with-variable-fix.conf" ) ) ) ) {
            startWithProfile( kernel, "module-name-with-variable-fix", "truetest1" );
            assertThat( kernel.service( "*.module-profile" ) ).isPresent().get().isInstanceOf( TestProfile1.class );
        }


        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module-name-with-variable-fix.conf" ) ) ) ) {
            startWithProfile( kernel, "module-name-with-variable-fix", "true-test1" );
            assertThat( kernel.service( "*.module-profile" ) ).isEmpty();
        }

        try( var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "module-name-with-variable-fix.conf" ) ) ) ) {
            startWithProfile( kernel, "module-name-with-variable-fix", "false-test1" );
            assertThat( kernel.service( "*.module-profile" ) ).isPresent().get().isInstanceOf( TestProfile1.class );
        }
    }

    public interface TestProfile {

    }

    public static class TestProfile1 implements TestProfile {
        public TestProfile1 profile1;
    }

    public static class TestProfile2 implements TestProfile {
    }

    public static class TestProfile3 implements TestProfile {
    }

    public static class TestContainer {
        public final TestProfile profile;

        public TestContainer( TestProfile profile ) {
            this.profile = profile;
            assertThat( profile ).isNotNull();
        }
    }

    public static class TestContainer2 {
        public TestContainer2() {
        }
    }

    public static class TestContainer3 {
        public final TestProfile profile;

        public TestContainer3( TestProfile profile ) {
            this.profile = profile;
            assertThat( profile ).isNotNull();
        }
    }

}

