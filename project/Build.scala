import sbt._

object Build extends sbt.Build {
    lazy val `oap-core` = project.in(file(".")).aggregate(`oap-application`)
    lazy val `oap-application` = project
}
