/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oap.json.schema;

import oap.validation.ValidationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.khbd.interp4j.core.Interpolations.s;

/**
 * Every validation message of the JSON schema validators. Each message has a unique code, starting at 100, a
 * template and the keyword whose custom {@code errorMessage} may replace it. The template starts with
 * {@code ${prefix}} (the {@code /path: } of the failing value, empty at the root) and has {@code ${name}} placeholders.
 */
public enum JsonMessage implements ValidationMessage {
    TYPE( 100, "type", "${prefix}instance type is ${actualType}, but allowed type is ${schemaType}" ),
    MIN_LENGTH( 101, "minLength", "${prefix}string ${value} is shorter than minLength ${minLength}" ),
    MAX_LENGTH( 102, "maxLength", "${prefix}string ${value} is longer than maxLength ${maxLength}" ),
    PATTERN( 103, "pattern", "${prefix}string ${value} does not match specified regex ${pattern}" ),
    MINIMUM( 104, "minimum", "${prefix}number ${value} is lower than the required minimum ${minimum}" ),
    MAXIMUM( 105, "maximum", "${prefix}number ${value} is greater than the required maximum ${maximum}" ),
    MINIMUM_EXCLUSIVE( 106, "minimum", "${prefix}number ${value} is not strictly greater than the required minimum ${minimum}" ),
    MAXIMUM_EXCLUSIVE( 107, "maximum", "${prefix}number ${value} is not strictly lower than the required maximum ${maximum}" ),
    MIN_ITEMS( 108, "minItems", "${prefix}array ${value} has less than minItems elements ${minItems}" ),
    MAX_ITEMS( 109, "maxItems", "${prefix}array ${value} has more than maxItems elements ${maxItems}" ),
    REQUIRED( 110, "required", "${prefix}required property is missing" ),
    ADDITIONAL_PROPERTIES_NOT_PERMITTED( 111, "additionalProperties", "${prefix}additional properties are not permitted ${additionalProperties}" ),
    DATE( 112, "date", "${prefix}${error}" ),
    DICTIONARY_NOT_FOUND( 113, null, "${prefix}dictionary ${name} not found" ),
    DICTIONARY_NO_MATCH( 114, null, "${prefix}instance of '${value}' does not match any member resolve the enumeration ${ids}" ),
    ENUM( 115, "enum", "${prefix}instance of '${value}' does not match any member resolve the enumeration ${enumeration}" ),
    CONST( 116, "const", "${prefix}instance does not equal const value '${constValue}'" ),
    ANY_OF( 117, null, "${prefix}instance does not match any schema in anyOf" ),
    ONE_OF( 118, null, "${prefix}instance must match exactly one schema in oneOf, matched ${matched}" ),
    NOT( 119, null, "${prefix}instance must not be valid against the schema in not" );

    /** The placeholder bound to the path prefix; it is not a value of the message itself. */
    public static final String PREFIX = "prefix";

    private static final Pattern PLACEHOLDER = Pattern.compile( "\\$\\{(\\w+)}" );

    private final int code;
    /** Keyword whose custom {@code errorMessage} replaces this message, or {@code null} when not customizable. */
    public final String keyword;
    private final String template;

    JsonMessage( int code, String keyword, String template ) {
        this.code = code;
        this.keyword = keyword;
        this.template = template;
    }

    /** The message with the given code, as a string (see {@link ValidationMessage#code()}); fails for an unknown code. */
    public static JsonMessage of( String code ) {
        for( JsonMessage message : values() ) {
            if( message.code().equals( code ) ) return message;
        }
        throw new IllegalArgumentException( s( "unknown JSON message code ${code}" ) );
    }

    @Override
    public String code() {
        return "JSON-" + code;
    }

    @Override
    public String message() {
        return template;
    }

    /** Placeholder names in template order, without {@link #PREFIX}. */
    public List<String> placeholders() {
        List<String> names = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher( template );
        while( matcher.find() ) {
            if( !PREFIX.equals( matcher.group( 1 ) ) ) {
                names.add( matcher.group( 1 ) );
            }
        }
        return names;
    }
}
