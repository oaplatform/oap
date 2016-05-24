package test;

import java.util.Optional;
import java.util.List;

import static java.util.Arrays.asList;

public enum TestDictionaryExternalIdAsCharacter {
  id1('A', true, Optional.of(1L), Optional.of("1"), true, Optional.of(asList(true, false)), Optional.of(asList("test1", "test2")), "title1"),
  id2('z', true, Optional.empty(), Optional.empty(), false, Optional.empty(), Optional.empty(), "title2");

  private final char externalId;
  private final boolean enabled;

  private final Optional<Long> p1;
  private final Optional<String> p2;
  private final boolean p3;
  private final Optional<List> p4;
  private final Optional<List> p5;
  private final String title;

  public final boolean enabled() {return enabled;}
  public final char externalId() {return externalId;}

  public final Optional<Long> p1(){return p1;}
  public final Optional<String> p2(){return p2;}
  public final boolean p3(){return p3;}
  public final Optional<List> p4(){return p4;}
  public final Optional<List> p5(){return p5;}
  public final String title(){return title;}

  TestDictionaryExternalIdAsCharacter( char externalId, boolean enabled, Optional<Long> p1, Optional<String> p2, boolean p3, Optional<List> p4, Optional<List> p5, String title ) {
    this.externalId = externalId;
    this.enabled = enabled;
    this.p1 = p1;
    this.p2 = p2;
    this.p3 = p3;
    this.p4 = p4;
    this.p5 = p5;
    this.title = title;
  }
}
