/*
 * Copyright (c) Madberry Oy
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.testng;

import com.google.common.base.Throwables;
import lombok.val;
import oap.testng.casesuite.CaseContext;
import oap.util.Stream;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.stream.Collectors;

public class TestListener implements ITestListener, IInvokedMethodListener {
    @Override
    public void onTestStart( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );

        System.out.println( "##teamcity[testStarted name='" + Teamcity.escape( method ) + "' captureStandardOutput='true']" );
    }

    @Override
    public void onTestSuccess( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        val time = iTestResult.getEndMillis() - iTestResult.getStartMillis();
        System.out.println( "##teamcity[testFinished name='" + Teamcity.escape( method ) + "' duration='" + time + "']" );
    }

    public String getMethodName( ITestResult iTestResult ) {
        String pStr = "";
        String className = null;
        val parameters = iTestResult.getParameters();
        if( parameters.length > 0 ) {
            pStr = Stream.of( parameters )
                .filter( p -> p == null || !CaseContext.class.equals( p.getClass() ) )
                .map( Object::toString )
                .collect( Collectors.joining( ",", "[", "]" ) );
            if( parameters[0] != null && CaseContext.class.equals( parameters[0].getClass() ) ) {
                className = parameters[0].toString();
            }
        }

        return className != null
            ? className + pStr
            : iTestResult.getMethod().getRealClass().getSimpleName() + "." + iTestResult.getMethod().getMethodName() + pStr;
    }

    @Override
    public void onTestFailure( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        val t = iTestResult.getThrowable();
        val message = t != null ? t.getMessage() : "";
        val details = t != null ? Throwables.getStackTraceAsString( t ) : "";
        System.out.println( "##teamcity[testFailed type='comparisonFailure' name='" + Teamcity.escape( method ) + "' message='" + Teamcity.escape( message ) + "' details='" + Teamcity.escape( details ) + "']" );
        val time = iTestResult.getEndMillis() - iTestResult.getStartMillis();
        System.out.println( "##teamcity[testFinished name='" + Teamcity.escape( method ) + "' duration='" + time + "']" );
    }

    @Override
    public void onTestSkipped( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        System.out.println( "##teamcity[testIgnored name='" + Teamcity.escape( method ) + "' message='skipped']" );
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        val time = iTestResult.getEndMillis() - iTestResult.getStartMillis();
        System.out.println( "##teamcity[testFinished name='" + Teamcity.escape( method ) + "' duration='" + time + "']" );
    }

    @Override
    public void onStart( ITestContext iTestContext ) {

    }

    @Override
    public void onFinish( ITestContext iTestContext ) {

    }

    @Override
    public void beforeInvocation( IInvokedMethod method, ITestResult testResult ) {
    }

    @Override
    public void afterInvocation( IInvokedMethod invokeMethod, ITestResult testResult ) {
        if( !invokeMethod.isConfigurationMethod() ) return;

        val methodName = testResult.getMethod().getRealClass().getSimpleName() + "." + testResult.getMethod().getMethodName();
        val t = testResult.getThrowable();
        if( t != null ) {
            val message = t.getMessage();
            val details = Throwables.getStackTraceAsString( t );

            System.out.println( "##teamcity[testMetadata name='" + Teamcity.escape( methodName ) + "' name='message' value='" + Teamcity.escape( message ) + "']" );
            System.out.println( "##teamcity[testMetadata name='" + Teamcity.escape( methodName ) + "' name='details' value='" + Teamcity.escape( details ) + "']" );
        }
    }
}
