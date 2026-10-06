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

package oap.ws.openapi;

import lombok.extern.slf4j.Slf4j;
import oap.reflect.Reflect;
import oap.ws.Response;
import oap.ws.WsClientException;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.ValidationErrorsBuilder;
import oap.ws.validate.WsValidate;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the HTTP error responses a web method can produce by reading its bytecode with ASM.
 * <p>
 * Scanned: the web method itself and the methods named in {@link WsValidate} (on the method and on its parameters).
 * A literal {@code int} code passed to {@link ValidationErrors}, {@link WsClientException}, {@link Response}
 * or {@code oap.http.Response} is an HTTP status code, recorded when it is {@code >= 400}.
 * Calls to methods of the scanned class hierarchy and to {@link Response} are followed, up to {@link #MAX_DEPTH} levels.
 * {@code Response.build401()}, {@code build403()} and {@code build404()} always yield their status code.
 * <p>
 * Messages: {@code statusCode( x ).error( text )} and {@code statusCode( x ).error( code, text )} on
 * {@link ValidationErrorsBuilder} add a message to status {@code x}. The message code is optional: without one it is
 * {@code null}, with one it must be a literal. The text is kept when it is a literal, otherwise it is {@code null}.
 * Formatted messages ({@code error( text, Map )} / {@code error( code, text, Map )}) keep their template text
 * (for example {@code ${name}} placeholders), since the rendered text is only known at runtime.
 * Messages added through lists are not listed, since their count is unknown.
 * <p>
 * Limits: a code held in a local variable or computed at runtime is not resolved and is logged as a warning.
 */
@Slf4j
public class ErrorCodeScanner {
    private static final Map<String, Integer> ERROR_BUILDERS = Map.of(
        "build401", 401,
        "build403", 403,
        "build404", 404 );
    private static final String CONSTRUCTOR = "<init>";
    private static final String STATUS_CODE = "statusCode";
    private static final String ERROR = "error";
    private static final String MESSAGE_DESC_PREFIX = "(ILjava/lang/String;";
    private static final String PLAIN_MESSAGE_DESC_PREFIX = "(Ljava/lang/String;";
    private static final int MIN_ERROR_CODE = 400;
    static final int MAX_DEPTH = 5;
    private static final Set<String> CODE_OWNERS = Set.of(
        Type.getInternalName( ValidationErrors.class ),
        Type.getInternalName( WsClientException.class ),
        Type.getInternalName( Response.class ),
        Type.getInternalName( oap.http.Response.class ) );

    private final Map<Class<?>, ClassNode> classNodes = new ConcurrentHashMap<>();

    /**
     * A message of an error response: its message code (may be {@code null}) and its text (may be {@code null}
     * when the text is not a literal).
     */
    public record ScannedMessage( Integer code, String text ) {
    }

    /**
     * @return sorted distinct error codes ({@code >= 400}) the method can produce
     */
    public SortedSet<Integer> errorCodes( java.lang.reflect.Method method ) {
        return new TreeSet<>( errorResponses( method ).keySet() );
    }

    /**
     * @return HTTP status code ({@code >= 400}) to the messages the method adds under it; a status without messages maps to an empty set
     */
    public SortedMap<Integer, Set<ScannedMessage>> errorResponses( java.lang.reflect.Method method ) {
        SortedMap<Integer, Set<ScannedMessage>> results = new TreeMap<>();
        Set<String> visited = new HashSet<>();
        Class<?> scanned = method.getDeclaringClass();

        scanMethod( scanned, scanned, method.getName(), Type.getMethodDescriptor( method ), 0, visited, results );

        for( String validatorName : validatorNames( method ) ) {
            var validator = Reflect.reflect( scanned ).method( validatorName ).map( m -> m.underlying );
            if( validator.isEmpty() ) {
                log.warn( "@WsValidate method '{}' not found in {}", validatorName, scanned.getName() );
                continue;
            }
            java.lang.reflect.Method validatorMethod = validator.get();
            scanMethod( validatorMethod.getDeclaringClass(), scanned, validatorMethod.getName(),
                Type.getMethodDescriptor( validatorMethod ), 0, visited, results );
        }

        return results;
    }

    private static List<String> validatorNames( java.lang.reflect.Method method ) {
        List<String> names = new ArrayList<>();
        WsValidate onMethod = method.getAnnotation( WsValidate.class );
        if( onMethod != null ) names.addAll( List.of( onMethod.value() ) );
        for( var parameter : method.getParameters() ) {
            WsValidate onParameter = parameter.getAnnotation( WsValidate.class );
            if( onParameter != null ) names.addAll( List.of( onParameter.value() ) );
        }
        return names;
    }

    /** The analysed body of one method: frame and instruction at the same index. */
    private record Body( Frame<SourceValue>[] frames, AbstractInsnNode[] instructions ) {
    }

    private void scanMethod( Class<?> owner, Class<?> scanned, String name, String desc,
                             int depth, Set<String> visited, Map<Integer, Set<ScannedMessage>> results ) {
        if( depth > MAX_DEPTH || owner == null || owner == Object.class
            || !visited.add( owner.getName() + "#" + name + desc ) ) return;

        ClassNode classNode = classNode( owner );
        if( classNode == null ) return;

        MethodNode methodNode = findMethod( classNode, name, desc );
        if( methodNode == null ) {
            scanMethod( owner.getSuperclass(), scanned, name, desc, depth, visited, results );
            return;
        }
        if( methodNode.instructions.size() == 0 ) return;

        Frame<SourceValue>[] frames;
        try {
            frames = new Analyzer<>( new SourceInterpreter() ).analyze( classNode.name, methodNode );
        } catch( AnalyzerException e ) {
            log.warn( "Cannot analyze bytecode of {}#{}{}", owner.getName(), name, desc, e );
            return;
        }

        Body body = new Body( frames, methodNode.instructions.toArray() );
        for( int i = 0; i < body.instructions().length; i++ ) {
            if( body.instructions()[i] instanceof MethodInsnNode invoke && frames[i] != null ) {
                scanInvoke( invoke, frames[i], body, owner, scanned, depth, visited, results );
            }
        }
    }

    private void scanInvoke( MethodInsnNode invoke, Frame<SourceValue> frame, Body body, Class<?> owner, Class<?> scanned,
                             int depth, Set<String> visited, Map<Integer, Set<ScannedMessage>> results ) {
        Integer builderCode = Type.getInternalName( Response.class ).equals( invoke.owner )
            ? ERROR_BUILDERS.get( invoke.name ) : null;
        if( builderCode != null ) {
            status( results, builderCode );
            return;
        }
        if( CODE_OWNERS.contains( invoke.owner ) ) collectCode( invoke, frame, owner, results );
        if( isMessage( invoke ) ) collectMessage( invoke, frame, body, owner, results );
        if( CONSTRUCTOR.equals( invoke.name ) ) return;

        Class<?> target = resolve( invoke.owner, scanned.getClassLoader() );
        if( target != null && followable( target, scanned ) ) {
            scanMethod( target, scanned, invoke.name, invoke.desc, depth + 1, visited, results );
        }
    }

    private static Set<ScannedMessage> status( Map<Integer, Set<ScannedMessage>> results, int code ) {
        return results.computeIfAbsent( code, c -> new LinkedHashSet<>() );
    }

    private void collectCode( MethodInsnNode invoke, Frame<SourceValue> frame, Class<?> owner, Map<Integer, Set<ScannedMessage>> results ) {
        Type[] args = Type.getArgumentTypes( invoke.desc );
        int codeIndex = -1;
        for( int i = 0; i < args.length; i++ ) {
            if( args[i].getSort() == Type.INT ) {
                codeIndex = i;
                break;
            }
        }
        if( codeIndex < 0 ) return;

        SourceValue value = frame.getStack( frame.getStackSize() - args.length + codeIndex );
        Integer code = constantValue( value );
        if( code == null ) {
            log.warn( "Non-constant error code passed to {}.{} in {}, skipped", invoke.owner, invoke.name, owner.getName() );
            return;
        }
        if( code >= MIN_ERROR_CODE ) status( results, code );
    }

    private static boolean isMessage( MethodInsnNode invoke ) {
        return Type.getInternalName( ValidationErrorsBuilder.class ).equals( invoke.owner )
            && ERROR.equals( invoke.name )
            && ( invoke.desc.startsWith( MESSAGE_DESC_PREFIX ) || invoke.desc.startsWith( PLAIN_MESSAGE_DESC_PREFIX ) );
    }

    private void collectMessage( MethodInsnNode invoke, Frame<SourceValue> frame, Body body, Class<?> owner,
                                 Map<Integer, Set<ScannedMessage>> results ) {
        int argc = Type.getArgumentTypes( invoke.desc ).length;
        int top = frame.getStackSize();

        Integer statusCode = statusCodeOf( frame.getStack( top - argc - 1 ), body );
        if( statusCode == null ) {
            log.warn( "Message in {} is not added to a literal statusCode(...), skipped", owner.getName() );
            return;
        }
        if( statusCode < MIN_ERROR_CODE ) return;

        boolean withCode = invoke.desc.startsWith( MESSAGE_DESC_PREFIX );
        Integer messageCode = null;
        if( withCode ) {
            messageCode = constantValue( frame.getStack( top - argc ) );
            if( messageCode == null ) {
                log.warn( "Non-constant message code in {}, skipped", owner.getName() );
                return;
            }
        }

        int textIndex = withCode ? 1 : 0;
        AbstractInsnNode textProducer = single( frame.getStack( top - argc + textIndex ) );
        String text = textProducer instanceof LdcInsnNode ldc && ldc.cst instanceof String literal ? literal : null;

        status( results, statusCode ).add( new ScannedMessage( messageCode, text ) );
    }

    /** The literal status code of the {@code statusCode( int )} call that produced the receiver, or null. */
    private static Integer statusCodeOf( SourceValue receiver, Body body ) {
        AbstractInsnNode producer = single( receiver );
        if( !( producer instanceof MethodInsnNode call ) || !STATUS_CODE.equals( call.name )
            || !Type.getInternalName( ValidationErrors.class ).equals( call.owner ) ) return null;

        Frame<SourceValue> frame = frameOf( call, body );
        if( frame == null ) return null;
        return constantValue( frame.getStack( frame.getStackSize() - 1 ) );
    }

    private static Frame<SourceValue> frameOf( AbstractInsnNode insn, Body body ) {
        for( int i = 0; i < body.instructions().length; i++ ) {
            if( body.instructions()[i] == insn ) return body.frames()[i];
        }
        return null;
    }

    private static AbstractInsnNode single( SourceValue value ) {
        return value.insns.size() == 1 ? value.insns.iterator().next() : null;
    }

    private static Integer constantValue( SourceValue value ) {
        if( value.insns.size() != 1 ) return null;
        AbstractInsnNode insn = value.insns.iterator().next();
        int opcode = insn.getOpcode();
        if( opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5 ) return opcode - Opcodes.ICONST_0;
        if( ( opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH ) && insn instanceof IntInsnNode intInsn )
            return intInsn.operand;
        if( insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer constant ) return constant;
        return null;
    }

    private static boolean followable( Class<?> target, Class<?> scanned ) {
        return target == Response.class
            || target == oap.http.Response.class
            || target != Object.class && target.isAssignableFrom( scanned );
    }

    private static Class<?> resolve( String internalName, ClassLoader loader ) {
        try {
            return Class.forName( internalName.replace( '/', '.' ), false, loader );
        } catch( ClassNotFoundException | LinkageError e ) {
            return null;
        }
    }

    private ClassNode classNode( Class<?> clazz ) {
        ClassNode cached = classNodes.get( clazz );
        if( cached != null ) return cached;

        try( InputStream in = clazz.getResourceAsStream( "/" + clazz.getName().replace( '.', '/' ) + ".class" ) ) {
            if( in == null ) {
                log.warn( "Bytecode not found for {}", clazz.getName() );
                return null;
            }
            ClassNode node = new ClassNode();
            new ClassReader( in ).accept( node, 0 );
            classNodes.put( clazz, node );
            return node;
        } catch( IOException e ) {
            log.warn( "Cannot read bytecode of {}", clazz.getName(), e );
            return null;
        }
    }

    private static MethodNode findMethod( ClassNode classNode, String name, String desc ) {
        for( MethodNode method : classNode.methods ) {
            if( method.name.equals( name ) && method.desc.equals( desc ) ) return method;
        }
        return null;
    }
}
