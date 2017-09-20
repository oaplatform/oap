/*
 * Copyright (c) Madberry Oy
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.testng;

import com.google.common.base.Throwables;
import lombok.val;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Created by igor.petrenko on 20.09.2017.
 */
public class TestListener implements ITestListener {
    @Override
    public void onTestStart( ITestResult iTestResult ) {
        System.out.println( "##teamcity[testStarted name='" + Teamcity.escape( iTestResult.getTestName() ) + "' captureStandardOutput='true']" );
    }

    @Override
    public void onTestSuccess( ITestResult iTestResult ) {
        val time = iTestResult.getEndMillis() - iTestResult.getStartMillis();
        System.out.println( "##teamcity[testFinished name='" + Teamcity.escape( iTestResult.getTestName() ) + "' duration='" + time + "']" );
    }

    @Override
    public void onTestFailure( ITestResult iTestResult ) {
        val t = iTestResult.getThrowable();
        val message = t != null ? t.getMessage() : "";
        val details = t != null ? Throwables.getStackTraceAsString( t ) : "";
        System.out.println( "##teamcity[testFailed type='comparisonFailure' name='" + Teamcity.escape( iTestResult.getTestName() ) + "' message='" + Teamcity.escape( message ) + "' details='" + Teamcity.escape( details ) + "']" );
        val time = iTestResult.getEndMillis() - iTestResult.getStartMillis();
        System.out.println( "##teamcity[testFinished name='" + Teamcity.escape( iTestResult.getTestName() ) + "' duration='" + time + "']" );
    }

    @Override
    public void onTestSkipped( ITestResult iTestResult ) {
        System.out.println( "##teamcity[testIgnored name='" + Teamcity.escape( iTestResult.getTestName() ) + "' message='skipped']" );
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage( ITestResult iTestResult ) {
        val time = iTestResult.getEndMillis() - iTestResult.getStartMillis();
        System.out.println( "##teamcity[testFinished name='" + Teamcity.escape( iTestResult.getTestName() ) + "' duration='" + time + "']" );
    }

    @Override
    public void onStart( ITestContext iTestContext ) {

    }

    @Override
    public void onFinish( ITestContext iTestContext ) {

    }
}
