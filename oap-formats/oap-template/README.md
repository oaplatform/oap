# OAP template

An OAP template library is based on ANother Tool for Language Recognition (*ANTLR*).

*ABOUT ANTLR* is a powerful parser generator for reading, processing, executing, 
or translating structured text or binary files. It's widely used to build languages,
tools, and frameworks. From a grammar, ANTLR generates a parser that can build 
parseable trees and also generates a listener interface (or visitor) that makes it easy to
respond to the recognition of phrases of interest.

For more information please visit this link https://github.com/antlr/antlr4/blob/master/doc/index.md

### Motivation

To generate java objects on the fly from dynamic user configurations via help of template engine

### Use cases

- Speed-up response generation by creating java objects from user configuration
- Use template generator with log configuration(`config.v1.conf`) to serialize/deserialize data from/to different sources
- To use pre-compiled functions which can be used by user in dynamic configurations. 
- Such as `urlencode(0)`, `urlencode()`, `toUpperCase()`, etc.

### Macro examples

- Render string text
- Escape variables/expressions
- Get fields/properties/className/alias/chains
- Support Optional/OrOptional/OrCollection/Nullable functions
- Support default variables
- Concatenation
- Concatenation with dotes
- Sum
- Comments
- Support primitive as objects 

Please take a look at [TemplateEngineTest](src/test/java/oap/template/TemplateEngineTest.java). 
You will find a more detailed overview of how it can be used in code