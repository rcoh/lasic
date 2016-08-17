organization := "me.rcoh"
version := "0.1.0"
scalaVersion := "2.11.8"
libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.scalactic" %% "scalactic" % "3.0.0",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.json4s" %% "json4s-native" % "3.4.0"
)


// Turn me on to debug the macro expansion
// scalacOptions += "-Ymacro-debug-lite"
