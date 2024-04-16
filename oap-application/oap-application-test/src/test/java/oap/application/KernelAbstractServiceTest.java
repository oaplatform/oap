package oap.application;

import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KernelAbstractServiceTest {
    @Test
    public void testAbstractFalseError() {
        List<URL> modules = List.of( urlOfTestResource( getClass(), "testAbstractFalseError-module.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            assertThatThrownBy( () -> kernel.start( Map.of( "boot.main", "testAbstractFalseError" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "Service <testAbstractFalseError.service1> has an abstract implementation, but the \"abstract = true\" property is missing" );
        }
    }

    @Test
    public void testImplementationNotFound() {
        List<URL> modules = List.of( urlOfTestResource( getClass(), "testImplementationNotFound-module.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            assertThatThrownBy( () -> kernel.start( Map.of( "boot.main", "testImplementationNotFound" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "No implementation has been declared for the abstract service <testImplementationNotFound.service1> with interface oap.application.IService" );
        }
    }

    @Test
    public void testImplementationNotDeclared() {
        List<URL> modules = List.of(
            urlOfTestResource( getClass(), "testImplementationNotDeclared1-module.conf" ),
            urlOfTestResource( getClass(), "testImplementationNotDeclared2-module.conf" )
        );

        try( var kernel = new Kernel( modules ) ) {
            assertThatThrownBy( () -> kernel.start( Map.of( "boot.main", "testImplementationNotDeclared2" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "No implementation specified for abstract service <testImplementationNotDeclared1.service1> with interface oap.application.IService. Declared implementations [<modules.testImplementationNotDeclared1.serviceImpl>,<modules.testImplementationNotDeclared2.serviceImpl>]" );
        }
    }

    @Test
    public void testAbstractImplementation() {
        List<URL> modules = List.of( urlOfTestResource( getClass(), "testAbstractImplementation-module.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( pathOfTestResource( getClass(), "testAbstractImplementation-application.conf" ) );

            assertThat( kernel.service( "testAbstractImplementation.service1" ) )
                .isPresent()
                .get()
                .isInstanceOf( ServiceOne.class )
                .extracting( "i" )
                .isEqualTo( 10 );
        }
    }
}
