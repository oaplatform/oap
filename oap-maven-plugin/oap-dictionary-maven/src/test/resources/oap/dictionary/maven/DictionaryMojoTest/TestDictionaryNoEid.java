package test;

import oap.dictionary.Dictionary;

import java.util.Map;
import java.util.Optional;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Arrays.asList;

public enum TestDictionaryNoEid implements Dictionary {
  id1(3, true, "title1"),
  id2(4, true, "title2");

  private final int externalId;
  private final boolean enabled;

  private final String title;

  public final String title() { return title; }

  TestDictionaryNoEid( int externalId, boolean enabled, String title ) {
    this.externalId = externalId;
    this.enabled = enabled;
    this.title = title;
  }

  public static TestDictionaryNoEid valueOf( int externalId ) {
    switch( externalId ) {
      case 3: return id1;
      case 4: return id2;
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
  public TestDictionaryNoEid cloneDictionary() {
    return this;
  }

  public int externalId() {
    return externalId;
  }

}
