package test;

import java.util.Optional;
import java.util.List;

import static java.util.Arrays.asList;

public enum TestDictionary {
  id1(49, true, "title1"),
  id2(50, true, "title2");

  private final int externalId;
  private final boolean enabled;

  private final String title;

  public final boolean enabled() {return enabled;}
  public final int externalId() {return externalId;}

  public final String title(){return title;}

  TestDictionary( int externalId, boolean enabled, String title ) {
    this.externalId = externalId;
    this.enabled = enabled;
    this.title = title;
  }
}
