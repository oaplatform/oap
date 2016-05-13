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

import oap.util.Either;
import oap.util.Functions;

import java.util.List;
import java.util.Optional;

public class JsonValidatorProperties {
   public final Optional<String> path;
   public final Optional<Boolean> additionalProperties;
   public final boolean ignore_required_default;
   public final Object rootJson;
   public final SchemaAST rootSchema;

   public final Functions.TriFunction<JsonValidatorProperties, SchemaAST, Object, Either<List<String>, Object>>
      validator;

   public JsonValidatorProperties(
      SchemaAST rootSchema,
      Object rootJson,
      Optional<String> path,
      Optional<Boolean> additionalProperties,
      boolean ignore_required_default,
      Functions.TriFunction<JsonValidatorProperties, SchemaAST, Object, Either<List<String>, Object>> validator ) {
      this.rootSchema = rootSchema;
      this.rootJson = rootJson;
      this.path = path;
      this.additionalProperties = additionalProperties;
      this.ignore_required_default = ignore_required_default;
      this.validator = validator;
   }

   public JsonValidatorProperties withPath( String path ) {
      return new JsonValidatorProperties( rootSchema, rootJson, this.path.map( p -> Optional.of( p + "/" + path ) ).orElse(
         Optional.of( path ) ), additionalProperties, ignore_required_default, validator );
   }

   public JsonValidatorProperties withAdditionalProperties( Optional<Boolean> additionalProperties ) {
      return additionalProperties.map( ap -> new JsonValidatorProperties( rootSchema, rootJson, path, additionalProperties,
         ignore_required_default, validator ) )
         .orElse( this );
   }


   public String error( String message ) {
      return path.map( p -> "/" + p + ": " ).orElse( "" ) + message;
   }
}
