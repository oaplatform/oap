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

package oap.testng;

import com.google.common.base.Throwables;
import lombok.val;
import oap.testng.casesuite.CaseContext;
import oap.util.Stream;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.stream.Collectors;

public class TestListener implements ITestListener {
    @Override
    public void onTestStart( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );

        System.out.println( "##teamcity[testStarted name='" + Teamcity.escape( method ) + "' captureStandardOutput='true']" );
    }

    @Override
    public void onTestSuccess( ITestResult iTestResult ) {
        finish( iTestResult );
    }

    public void finish( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        val time = iTestResult.getEndMillis() - iTestResult.getStartMillis();
        System.out.println( "##teamcity[testFinished name='" + Teamcity.escape( method ) + "' duration='" + time + "']" );
    }

    private static String getClassName( ITestResult result ) {
        String className = null;
        val parameters = result.getParameters();
        if( parameters.length > 0 ) {
            if( parameters[0] != null && CaseContext.class.equals( parameters[0].getClass() ) ) {
                className = parameters[0].toString();
            }
        }

        return className != null ? className : result.getMethod().getRealClass().getSimpleName();
    }

    public String getMethodName( ITestResult iTestResult ) {
        String pStr;
        val parameters = iTestResult.getParameters();
        if( parameters.length > 0 ) {
            pStr = Stream.of( parameters )
                .filter( p -> p == null || !CaseContext.class.equals( p.getClass() ) )
                .map( Object::toString )
                .collect( Collectors.joining( ",", "(", ")" ) );
        } else {
            pStr = "." + iTestResult.getMethod().getMethodName();
        }

        return getClassName( iTestResult ) + pStr;
    }

    @Override
    public void onTestFailure( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        val t = iTestResult.getThrowable();
        val message = t != null ? t.getMessage() : "";
        val details = t != null ? Throwables.getStackTraceAsString( t ) : "";
        System.out.println( "##teamcity[testFailed type='comparisonFailure' name='" + Teamcity.escape( method ) + "' message='" + Teamcity.escape( message ) + "' details='" + Teamcity.escape( details ) + "']" );

        finish( iTestResult );
    }

    @Override
    public void onTestSkipped( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        System.out.println( "##teamcity[testIgnored name='" + Teamcity.escape( method ) + "' message='skipped']" );
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage( ITestResult iTestResult ) {
        finish( iTestResult );
    }

    @Override
    public void onStart( ITestContext context ) {
        onTestClass( context, "testSuiteStarted" );
    }

    @Override
    public void onFinish( ITestContext context ) {
        onTestClass( context, "testSuiteFinished" );
    }

    private static void onTestClass( ITestContext context, String method ) {
        String name = context.getName();
        val methods = context.getAllTestMethods();
        if( methods.length > 0 ) {
            name = methods[0].getTestClass().getName();
        }
        System.out.println( "##teamcity[" + method + " name='" + Teamcity.escape( name ) + "']" );
    }
}
