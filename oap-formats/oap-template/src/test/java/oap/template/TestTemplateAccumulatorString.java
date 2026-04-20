package oap.template;

public class TestTemplateAccumulatorString extends TemplateAccumulatorString {
    @Override
    public void accept( String text ) {
        super.accept( text + "2" );
    }

    @Override
    public TemplateAccumulatorString newInstance() {
        return new TestTemplateAccumulatorString();
    }
}
