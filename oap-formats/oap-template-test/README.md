# oap-template-test

Test utilities for `oap-template`. Provides a TestNG fixture that manages a `TemplateEngine` lifecycle around tests.

Depends on: `oap-template`, `oap-stdlib-test`

## `TemplateEngineFixture`

An `AbstractFixture` that creates a `TemplateEngine` with a disk cache before each test scope and exposes it via the `templateEngine` field.

```java
@Listeners( Fixtures.class )
public class MyTemplateTest extends Fixtures {
    private final TemplateEngineFixture engine = fixture( new TemplateEngineFixture() );

    @Test
    public void rendersTemplate() {
        Template<MyBean, String, StringBuilder, TemplateAccumulatorString> tmpl =
            engine.templateEngine.getTemplate(
                "greeting",
                new TypeRef<MyBean>() {},
                "Hello, {{ name }}!",
                TemplateAccumulators.STRING,
                ErrorStrategy.ERROR
            );

        assertThat( tmpl.render( new MyBean( "World" ) ).get() )
            .isEqualTo( "Hello, World!" );
    }
}
```

The fixture creates the engine with:
- Disk cache at `/tmp/file-cache` — compiled classes survive JVM restarts across test runs
- TTL of 5 days — cache entries older than 5 days are evicted

No teardown action is needed; the engine holds no resources that require explicit release.
