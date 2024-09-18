package oap.template;

public class TestTemplateMacros {
    public static String printDoubleWithArg( Double value, String arg ) {
        return value + arg;
    }

    public static Double toDouble( String value ) {
        return Double.parseDouble( value );
    }
}
