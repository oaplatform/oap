# oap-template
1. the generated code should be extremely fast
2. duplication is allowed to increase speed
3. AbstractTemplateEngineTest#listener allows you to get the generated code

TemplateEngineWithTest#testBlockWithMultipleFields

old code:
```
    // --- with ( child ) START BODY 
    // field
    if ( with_1 != null ) {
      java.lang.String s_with_1_field = with_1.field;
      if ( s_with_1_field != null ) {
        acc.accept( s_with_1_field );
      } else {
        acc.acceptNull( java.lang.String.class );
      }
    } else {
      acc.acceptNull( java.lang.String.class );
    }
    acc.acceptText( "-" );
    // field2
    if ( with_1 != null ) {
      java.lang.String s_with_1_field_ = with_1.field2;
      if ( s_with_1_field_ != null ) {
        acc.accept( s_with_1_field_ );
      } else {
        acc.acceptNull( java.lang.String.class );
      }
    } else {
      acc.acceptNull( java.lang.String.class );
    }
    // --- with ( child ) END body 

```

optimized code:

```
    // --- with ( child ) START BODY 
    // field
    if ( with_1 != null ) {
      java.lang.String s_with_1_field = with_1.field;
      if ( s_with_1_field != null ) {
        acc.accept( s_with_1_field );
      } else {
        acc.acceptNull( java.lang.String.class );
      }

      acc.acceptText( "-" );

      java.lang.String s_with_2_field_ = with_1.field2;
      if ( s_with_2_field_ != null ) {
        acc.accept( s_with_2_field_ );
      } else {
        acc.acceptNull( java.lang.String.class );
      }

   } else {
      acc.acceptNull( java.lang.String.class );

      acc.acceptText( "-" );

      acc.acceptNull( java.lang.String.class );
   }
    // --- with ( child ) END body 
```