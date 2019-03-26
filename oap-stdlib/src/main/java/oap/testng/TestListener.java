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
import oap.testng.casesuite.CaseContext;
import oap.util.Stream;
import org.testng.IClassListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.stream.Collectors;

public class TestListener implements ITestListener, ISuiteListener, IClassListener {
    private static final boolean DEBUG = false;

    private static String getClassName( ITestResult result ) {
        String className = null;
        var parameters = result.getParameters();
        if( parameters.length > 0 ) {
            if( parameters[0] != null && CaseContext.class.equals( parameters[0].getClass() ) ) {
                className = parameters[0].toString();
            }
        }

        return className != null ? className : result.getMethod().getRealClass().getSimpleName();
    }

    @Override
    public void onTestStart( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );

        System.out.println( "##teamcity[testStarted name='" + Teamcity.escape( method ) + "' captureStandardOutput='true']" );

        if( DEBUG ) {
            System.out.println();
            System.out.println( "DEBUG::teamcity[testStarted name='" + Teamcity.escape( method ) + "' captureStandardOutput='true']" );
        }
    }

    @Override
    public void onTestSuccess( ITestResult iTestResult ) {
        finish( iTestResult );
    }

    public void finish( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        var time = iTestResult.getEndMillis() - iTestResult.getStartMillis();
        System.out.println( "##teamcity[testFinished name='" + Teamcity.escape( method ) + "' duration='" + time + "']" );

        if( DEBUG ) {
            System.out.println();
            System.out.println( "DEBUG::teamcity[testFinished name='" + Teamcity.escape( method ) + "' duration='" + time + "']" );
        }
    }

    public String getMethodName( ITestResult iTestResult ) {
        String pStr;
        var parameters = iTestResult.getParameters();
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
        var t = iTestResult.getThrowable();
        var message = t != null ? t.getMessage() : "";
        var details = t != null ? Throwables.getStackTraceAsString( t ) : "";
        System.out.println( "##teamcity[testFailed type='comparisonFailure' name='" + Teamcity.escape( method ) + "' message='" + Teamcity.escape( message ) + "' details='" + Teamcity.escape( details ) + "']" );

        if( DEBUG ) {
            System.out.println();
            System.out.println( "DEBUG::teamcity[testFailed type='comparisonFailure' name='" + Teamcity.escape( method ) + "' message='" + Teamcity.escape( message ) + "' details='" + Teamcity.escape( details ) + "']" );
        }

        finish( iTestResult );
    }

    @Override
    public void onTestSkipped( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        System.out.println( "##teamcity[testIgnored name='" + Teamcity.escape( method ) + "' message='skipped']" );

        if( DEBUG ) {
            System.out.println();
            System.out.println( "DEBUG::teamcity[testIgnored name='" + Teamcity.escape( method ) + "' message='skipped']" );
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage( ITestResult iTestResult ) {
        finish( iTestResult );
    }

    @Override
    public void onStart( ITestContext context ) {
    }

    @Override
    public void onFinish( ITestContext context ) {
    }

    @Override
    public void onStart( ISuite suite ) {
//        System.out.println( "##teamcity[testSuiteStarted name='" + Teamcity.escape( suite.getName() ) + "']" );
//
//        if( DEBUG ) {
//            System.out.println();
//            System.out.println( "DEBUG::teamcity[testSuiteStarted name='" + Teamcity.escape( suite.getName() ) + "']" );
//        }
    }

    @Override
    public void onFinish( ISuite suite ) {
//        System.out.println( "##teamcity[testSuiteFinished name='" + Teamcity.escape( suite.getName() ) + "']" );
//
//        if( DEBUG ) {
//            System.out.println();
//            System.out.println( "DEBUG::teamcity[testSuiteFinished name='" + Teamcity.escape( suite.getName() ) + "']" );
//        }
    }

    @Override
    public void onBeforeClass( ITestClass testClass ) {
        System.out.println( "##teamcity[testSuiteStarted name='" + Teamcity.escape( testClass.getName() ) + "']" );

        if( DEBUG ) {
            System.out.println();
            System.out.println( "DEBUG::teamcity[testSuiteStarted name='" + Teamcity.escape( testClass.getName() ) + "']" );
        }
    }

    @Override
    public void onAfterClass( ITestClass testClass ) {
        System.out.println( "##teamcity[testSuiteFinished name='" + Teamcity.escape( testClass.getName() ) + "']" );

        if( DEBUG ) {
            System.out.println();
            System.out.println( "DEBUG::teamcity[testSuiteFinished name='" + Teamcity.escape( testClass.getName() ) + "']" );
        }
    }
}
