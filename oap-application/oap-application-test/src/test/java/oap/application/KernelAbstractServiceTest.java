package oap.application;

import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;
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
                .hasMessage( "No implementation specified for abstract service <testImplementationNotDeclared1.service1> with interface oap.application.IService. Available implementations [<modules.testImplementationNotDeclared1.serviceImpl>,<modules.testImplementationNotDeclared2.serviceImpl>]" );
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

    @Test
    public void testAbstractImplementationDifferentModules() {
        List<URL> modules = List.of(
            urlOfTestResource( getClass(), "testAbstractImplementationDifferentModules1-module.conf" ),
            urlOfTestResource( getClass(), "testAbstractImplementationDifferentModules2-module.conf" )
        );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( pathOfTestResource( getClass(), "testAbstractImplementationDifferentModules-application.conf" ) );

            assertThat( kernel.service( "testAbstractImplementationDifferentModules1.service1" ) )
                .isPresent()
                .get()
                .isInstanceOf( ServiceOne.class )
                .extracting( "i" )
                .isEqualTo( 10 );
        }
    }

    @Test
    public void testReferenceToAbstractService() {
        List<URL> modules = List.of( urlOfTestResource( getClass(), "testReferenceToAbstractService-module.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( pathOfTestResource( getClass(), "testReferenceToAbstractService-application.conf" ) );

            ServiceRef ref = kernel.<ServiceRef>service( "testReferenceToAbstractService.ref" ).get();
            assertThat( ref.service )
                .isNotNull()
                .isInstanceOf( ServiceOne.class )
                .extracting( "i" )
                .isEqualTo( 10 );
            assertThat( ref.service2 )
                .isNotNull()
                .isInstanceOf( ServiceOne.class )
                .extracting( "i" )
                .isEqualTo( 10 );
            assertThat( ref.services )
                .isNotEmpty()
                .allMatch( s -> s instanceof ServiceOne )
                .allMatch( s -> ( ( ServiceOne ) s ).i == 10 );
        }
    }

    @Test
    public void testInvalidImplementationReference() {
        List<URL> modules = List.of( urlOfTestResource( getClass(), "testInvalidImplementationReference-module.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            assertThatThrownBy( () -> kernel.start( pathOfTestResource( getClass(), "testInvalidImplementationReference-application.conf" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "Unknown module unknown-module in reference <modules.unknown-module.serviceImpl>" );
        }

        try( var kernel = new Kernel( modules ) ) {
            assertThatThrownBy( () -> kernel.start( pathOfTestResource( getClass(), "testInvalidImplementationReference2-application.conf" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "Unknown service unknown-service in reference <modules.testInvalidImplementationReference.unknown-service>" );
        }
    }

    public static class ServiceRef {
        public final ArrayList<IService> services = new ArrayList<>();
        public IService service;
        public IService service2;

        public ServiceRef( IService service2 ) {
            this.service2 = service2;
        }
    }
}
