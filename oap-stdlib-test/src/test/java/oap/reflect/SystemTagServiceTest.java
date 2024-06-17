package oap.reflect;

import lombok.Builder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

public class SystemTagServiceTest {

    @Builder
    @SystemTagSupport
    static class TestObject {
        public String name;
        @SystemTag( "create date" )
        public DateTime createdAt;
        @SystemTag( "update date" )
        public DateTime updatedAt;
        @SystemTag( "archived" )
        public boolean archived;
    }

    private static SystemTagsService systemTagsService = new SystemTagsService();

    @Test
    void shouldProcessTags() {
        DateTime now = new DateTime( DateTimeUtils.currentTimeMillis() );
        TestObject testObject = TestObject.builder()
                .name( "TestName" )
                .createdAt( now )
                .updatedAt( now )
                .build();
        Map<String, Object> stringObjectMap = systemTagsService.collectSystemTags( testObject );
        assertEquals( now, stringObjectMap.get( "create date" ) );
        assertEquals( now, stringObjectMap.get( "update date" ) );
        assertEquals( false, stringObjectMap.get( "archived" ) );
    }
}
