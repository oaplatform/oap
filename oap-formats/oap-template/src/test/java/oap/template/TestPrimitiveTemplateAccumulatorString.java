package oap.template;

public class TestPrimitiveTemplateAccumulatorString extends TemplateAccumulatorString {
    @Override
    public void accept( boolean b ) {
        super.accept( b + "_b" );
    }

    @Override
    public void accept( int i ) {
        super.accept( i + "_i" );
    }

    @Override
    public TemplateAccumulatorString newInstance() {
        return new TestPrimitiveTemplateAccumulatorString();
    }
}
