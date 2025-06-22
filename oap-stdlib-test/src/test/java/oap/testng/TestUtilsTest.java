package oap.testng;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestUtilsTest {
    @BeforeMethod
    public void beforeMethod() {
        String rand = TestUtils.randomName( "{rand}" );
        assertThat( TestUtils.randomName( "{test_class_name}-{rand}" ) ).isEqualTo( "TestUtilsTest-" + rand );

        assertThatThrownBy( () -> TestUtils.randomName( "{test_class_name}-{test_method_name}-{rand}" ) )
            .isInstanceOf( IllegalArgumentException.class );
    }


    @Test
    public void testRandomName() {
        String rand = TestUtils.randomName( "{rand}" );
        assertThat( TestUtils.randomName( "{test_class_name}-{test_method_name}-{rand}" ) ).isEqualTo( "TestUtilsTest-testRandomName-" + rand );
    }
}
