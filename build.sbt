lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.example",
      scalaVersion := "2.13.7",
      version := "0.1.0-SNAPSHOT",
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision,
      scalafixScalaBinaryVersion := "2.13"
    )
  ),
  name := "LibreCaptcha",
  libraryDependencies += "com.sksamuel.scrimage" % "scrimage-core" % "4.0.24",
  libraryDependencies += "com.sksamuel.scrimage" % "scrimage-filters" % "4.0.24",
  libraryDependencies += "org.json4s" % "json4s-jackson_2.13" % "4.0.3"
)

Compile / unmanagedResourceDirectories += { baseDirectory.value / "lib" }
scalacOptions ++= List(
  "-Yrangepos",
  "-Ywarn-unused",
  "-deprecation",
  "-Xsource:3"
)
javacOptions += "-g:none"
compileOrder := CompileOrder.JavaThenScala
javafmtOnCompile := false
assembly / mainClass := Some("lc.LCFramework")
Compile / run / mainClass := Some("lc.LCFramework")
assembly / assemblyJarName := "LibreCaptcha.jar"

assembly / assemblyMergeStrategy := {
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

run / fork := true
