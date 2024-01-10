package test;

import oap.dictionary.Dictionary;

import java.util.Map;
import java.util.Optional;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Arrays.asList;

public enum TestDictionaryExternalIdAsCharacter implements Dictionary {
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

  public final Optional<Long> p1() { return p1; }
  public final Optional<String> p2() { return p2; }
  public final boolean p3() { return p3; }
  public final Optional<List> p4() { return p4; }
  public final Optional<List> p5() { return p5; }
  public final String title() { return title; }

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

  public static TestDictionaryExternalIdAsCharacter valueOf( int externalId ) {
    switch( externalId ) {
      case 65: return id1;
      case 122: return id2;
      default: throw new java.lang.IllegalArgumentException( "Unknown id " + externalId );
    }
  }

  @Override
    public int getOrDefault( String id, int defaultValue ) {
      return defaultValue;
    }

    @Override
    public Integer get( String id ) {
      return null;
    }

    @Override
    public String getOrDefault( int externlId, String defaultValue ) {
      return defaultValue;
    }

    @Override
    public boolean containsValueWithId( String id ) {
      return false;
    }

    @Override
    public List<String> ids() {
      return emptyList();
    }

    @Override
    public int[] externalIds() {
      return new int[0];
    }

    @Override
    public Map<String, Object> getProperties() {
      return emptyMap();
    }

    @Override
    public Optional<? extends Dictionary> getValueOpt( String name ) {
      return Optional.empty();
    }

    @Override
    public Dictionary getValue( String name ) {
      return null;
    }

    @Override
    public Dictionary getValue( int externalId ) {
      return null;
    }

    @Override
    public List<? extends Dictionary> getValues() {
      return emptyList();
    }

    @Override
    public String getId() {
      return name();
    }

    @Override
    public Optional<Object> getProperty( String name ) {
      return Optional.empty();
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public int getExternalId() {
      return externalId;
    }

    @Override
    public boolean containsProperty( String name ) {
      return false;
    }

  @Override
  public TestDictionaryExternalIdAsCharacter cloneDictionary() {
    return this;
  }

  public char externalId() {
    return externalId;
  }

}
