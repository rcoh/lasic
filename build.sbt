organization := "com.github.rcoh"
version := "0.2.0"
scalaVersion := "2.11.8"
libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.scalactic" %% "scalactic" % "3.0.0",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.json4s" %% "json4s-native" % "3.4.0"
)

// Turn me on to debug the macro expansion
// scalacOptions += "-Ymacro-debug-lite"

// Publishing info
publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/rcoh/lasic</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>https://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:rcoh/lastic.git</url>
    <connection>scm:git:git@github.com:rcoh/lasic.git</connection>
  </scm>
  <developers>
    <developer>
      <id>rcoh</id>
      <name>Russell Cohen</name>
      <url>rcoh.me</url>
    </developer>
  </developers>)


