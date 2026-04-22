package oap.template;

import oap.testng.AbstractFixture;
import oap.util.Dates;

import java.nio.file.Path;

public class TemplateEngineFixture extends AbstractFixture<TemplateEngineFixture> {
    public TemplateEngine templateEngine;

    @Override
    protected void before() {
        templateEngine = new TemplateEngine( Path.of( "/tmp/file-cache" ), Dates.d( 5 ) );
    }
}
