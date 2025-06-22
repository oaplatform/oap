package oap.testng;

import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TestUtils {
    private static final Set<Class<? extends Annotation>> TEST_ANNOTATIONS = Set.of(
        Test.class,
        BeforeMethod.class, AfterMethod.class,
        BeforeClass.class, AfterClass.class,
        BeforeTest.class, AfterTest.class,
        BeforeSuite.class, AfterSuite.class
    );

    private static final ConcurrentHashMap<String, String> rand = new ConcurrentHashMap<>();

    /**
     * @param pattern - ...{test_class_name}-{test_method_name}-{rand}...
     * @return
     */
    @SneakyThrows
    public static String randomName( String pattern ) throws IllegalArgumentException {
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        for( StackTraceElement element : stackTrace ) {
            Class<?> aClass = Class.forName( element.getClassName() );
            String methodName = element.getMethodName();
            Method[] methods = aClass.getMethods();
            for( Method method : methods ) {
                if( method.getName().equals( methodName ) && containsTestAnnotation( method ) ) {
                    boolean isTestMethod = method.getAnnotation( Test.class ) != null;

                    String name = pattern;
                    if( !isTestMethod && name.contains( "{test_method_name}" ) ) {
                        throw new IllegalArgumentException( "Pattern " + method.getName() + " is invalid." );
                    }
                    name = StringUtils.replace( name, "{test_method_name}", methodName );
                    name = StringUtils.replace( name, "{test_class_name}", aClass.getSimpleName() );
                    name = StringUtils.replace( name, "{rand}",
                        rand.computeIfAbsent( ( isTestMethod ? ( methodName + "-" ) : "" ) + aClass, k -> RandomStringUtils.secure().nextAlphabetic( 6 ) ) );

                    return name;
                }
            }
        }

        throw new IllegalArgumentException( "@org.testng.annotations.Test" );
    }

    private static boolean containsTestAnnotation( Method method ) {
        for( Annotation annotation : method.getAnnotations() ) {
            if( TEST_ANNOTATIONS.contains( annotation.annotationType() ) ) {
                return true;
            }
        }

        return false;
    }
}
