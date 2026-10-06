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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every validation message of the JSON schema validators. Each message has a unique code, starting at 1000, a
 * template with {@code ${name}} placeholders, and the keyword whose custom {@code errorMessage} may replace it.
 */
public enum JsonSchemaValidatorErrors {
    TYPE( 1000, "type", "instance type is ${actualType}, but allowed type is ${schemaType}" ),
    MIN_LENGTH( 1001, "minLength", "string ${value} is shorter than minLength ${minLength}" ),
    MAX_LENGTH( 1002, "maxLength", "string ${value} is longer than maxLength ${maxLength}" ),
    PATTERN( 1003, "pattern", "string ${value} does not match specified regex ${pattern}" ),
    MINIMUM( 1004, "minimum", "number ${value} is lower than the required minimum ${minimum}" ),
    MAXIMUM( 1005, "maximum", "number ${value} is greater than the required maximum ${maximum}" ),
    MINIMUM_EXCLUSIVE( 1006, "minimum", "number ${value} is not strictly greater than the required minimum ${minimum}" ),
    MAXIMUM_EXCLUSIVE( 1007, "maximum", "number ${value} is not strictly lower than the required maximum ${maximum}" ),
    MIN_ITEMS( 1008, "minItems", "array ${value} has less than minItems elements ${minItems}" ),
    MAX_ITEMS( 1009, "maxItems", "array ${value} has more than maxItems elements ${maxItems}" ),
    REQUIRED( 1010, "required", "required property is missing" ),
    ADDITIONAL_PROPERTIES_NOT_PERMITTED( 1011, "additionalProperties", "additional properties are not permitted ${additionalProperties}" ),
    DATE( 1012, "date", "${error}" ),
    DICTIONARY_NOT_FOUND( 1013, null, "dictionary ${name} not found" ),
    DICTIONARY_NO_MATCH( 1014, null, "instance of '${value}' does not match any member resolve the enumeration ${ids}" ),
    ENUM( 1015, "enum", "instance of '${value}' does not match any member resolve the enumeration ${enumeration}" ),
    CONST( 1016, "const", "instance does not equal const value '${constValue}'" ),
    ANY_OF( 1017, null, "instance does not match any schema in anyOf" ),
    ONE_OF( 1018, null, "instance must match exactly one schema in oneOf, matched ${matched}" ),
    NOT( 1019, null, "instance must not be valid against the schema in not" );

    private static final Pattern PLACEHOLDER = Pattern.compile( "\\$\\{(\\w+)}" );

    public final int code;
    /** Keyword whose custom {@code errorMessage} replaces this message, or {@code null} when not customizable. */
    public final String keyword;
    public final String template;

    JsonSchemaValidatorErrors( int code, String keyword, String template ) {
        this.code = code;
        this.keyword = keyword;
        this.template = template;
    }

    /** Placeholder names in template order. */
    public List<String> placeholders() {
        List<String> names = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher( template );
        while( matcher.find() ) names.add( matcher.group( 1 ) );
        return names;
    }
}
