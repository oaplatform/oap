package oap.template;

import com.fasterxml.jackson.annotation.JsonAlias;

public class TestTemplateMacros {
    @JsonAlias( "testIncAlias" )
    public static int testInc( int value ) {
        return value + 1;
    }
}
