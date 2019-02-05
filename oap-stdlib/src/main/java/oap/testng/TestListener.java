/*
 * Copyright (c) Madberry Oy
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.testng;

import com.google.common.base.Throwables;
import lombok.val;
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

    private String getParameters( ITestResult iTestResult ) {
        val parameters = iTestResult.getParameters();
        if( parameters.length == 0 ) return "";

        return Stream.of( parameters ).map( Object::toString ).collect( Collectors.joining( ",", "[", "]" ) );
    }

    @Override
    public void onTestSuccess( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        val time = iTestResult.getEndMillis() - iTestResult.getStartMillis();
        System.out.println( "##teamcity[testFinished name='" + Teamcity.escape( method ) + "' duration='" + time + "']" );
    }

    public String getMethodName( ITestResult iTestResult ) {
        return iTestResult.getMethod().getRealClass().getSimpleName() + "." + iTestResult.getMethod().getMethodName() + getParameters( iTestResult );
    }

    @Override
    public void onTestFailure( ITestResult iTestResult ) {
        String method = getMethodName( iTestResult );
        val t = iTestResult.getThrowable();
        val message = t != null ? t.getMessage() : "";
        val details = t != null ? Throwables.getStackTraceAsString( t ) : "";
        System.out.println( "##teamcity[testFailed type='comparisonFailure' name='" + Teamcity.escape( method + getParameters( iTestResult ) ) + "' message='" + Teamcity.escape( message ) + "' details='" + Teamcity.escape( details ) + "']" );
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
}
