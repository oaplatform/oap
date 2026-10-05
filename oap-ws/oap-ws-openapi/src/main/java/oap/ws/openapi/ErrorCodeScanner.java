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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the HTTP error codes a web method can return by reading its bytecode with ASM.
 * <p>
 * Scanned: the web method itself and the methods named in {@link WsValidate} (on the method and on its parameters).
 * A literal {@code int} code passed to {@link ValidationErrors}, {@link WsClientException}, {@link Response}
 * or {@code oap.http.Response}
 * is recorded when it is {@code >= 400}. Calls to methods of the scanned class hierarchy and to {@link Response}
 * are followed, up to {@link #MAX_DEPTH} levels. {@code Response.build401()}, {@code build403()} and
 * {@code build404()} always yield their status code.
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
    private static final int MIN_ERROR_CODE = 400;
    static final int MAX_DEPTH = 5;
    private static final Set<String> CODE_OWNERS = Set.of(
        Type.getInternalName( ValidationErrors.class ),
        Type.getInternalName( WsClientException.class ),
        Type.getInternalName( Response.class ),
        Type.getInternalName( oap.http.Response.class ) );

    private final Map<Class<?>, ClassNode> classNodes = new ConcurrentHashMap<>();

    /**
     * @return sorted distinct error codes ({@code >= 400}) the method can produce
     */
    public SortedSet<Integer> errorCodes( java.lang.reflect.Method method ) {
        SortedSet<Integer> codes = new TreeSet<>();
        Set<String> visited = new HashSet<>();
        Class<?> scanned = method.getDeclaringClass();

        scanMethod( scanned, scanned, method.getName(), Type.getMethodDescriptor( method ), 0, visited, codes );

        for( String validatorName : validatorNames( method ) ) {
            var validator = Reflect.reflect( scanned ).method( validatorName ).map( m -> m.underlying );
            if( validator.isEmpty() ) {
                log.warn( "@WsValidate method '{}' not found in {}", validatorName, scanned.getName() );
                continue;
            }
            java.lang.reflect.Method validatorMethod = validator.get();
            scanMethod( validatorMethod.getDeclaringClass(), scanned, validatorMethod.getName(),
                Type.getMethodDescriptor( validatorMethod ), 0, visited, codes );
        }

        return codes;
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

    private void scanMethod( Class<?> owner, Class<?> scanned, String name, String desc,
                             int depth, Set<String> visited, Set<Integer> codes ) {
        if( depth > MAX_DEPTH || owner == null || owner == Object.class
            || !visited.add( owner.getName() + "#" + name + desc ) ) return;

        ClassNode classNode = classNode( owner );
        if( classNode == null ) return;

        MethodNode methodNode = findMethod( classNode, name, desc );
        if( methodNode == null ) {
            scanMethod( owner.getSuperclass(), scanned, name, desc, depth, visited, codes );
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

        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        for( int i = 0; i < instructions.length; i++ ) {
            if( instructions[i] instanceof MethodInsnNode invoke && frames[i] != null ) {
                scanInvoke( invoke, frames[i], owner, scanned, depth, visited, codes );
            }
        }
    }

    private void scanInvoke( MethodInsnNode invoke, Frame<SourceValue> frame, Class<?> owner, Class<?> scanned,
                             int depth, Set<String> visited, Set<Integer> codes ) {
        Integer builderCode = Type.getInternalName( Response.class ).equals( invoke.owner )
            ? ERROR_BUILDERS.get( invoke.name ) : null;
        if( builderCode != null ) {
            codes.add( builderCode );
            return;
        }
        if( CODE_OWNERS.contains( invoke.owner ) ) collectCode( invoke, frame, owner, codes );
        if( CONSTRUCTOR.equals( invoke.name ) ) return;

        Class<?> target = resolve( invoke.owner, scanned.getClassLoader() );
        if( target != null && followable( target, scanned ) ) {
            scanMethod( target, scanned, invoke.name, invoke.desc, depth + 1, visited, codes );
        }
    }

    private void collectCode( MethodInsnNode invoke, Frame<SourceValue> frame, Class<?> owner, Set<Integer> codes ) {
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
        if( code >= MIN_ERROR_CODE ) codes.add( code );
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
