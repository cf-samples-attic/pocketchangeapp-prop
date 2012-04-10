organization := "com.pocketchangeapp"

name         := "PocketChange"

version      := "0.3.1-SNAPSHOT"

startYear    := Some(2008)

seq(webSettings :_*)

scalaVersion := "2.9.1"

libraryDependencies ++= Seq("net.liftweb"      %% "lift-webkit" % "2.4",
                            "net.liftweb"      %% "lift-mapper" % "2.4",
                            "jfree"             % "jfreechart"  % "1.0.13",
                            "com.h2database"    % "h2"          % "1.2.147" % "runtime",
                            "org.mortbay.jetty" % "jetty"       % "6.1.26"  % "container")
