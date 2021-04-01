lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.example",
      scalaVersion := "2.13.5",
      version := "0.1.0-SNAPSHOT",
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision,
      scalafixScalaBinaryVersion := "2.13"
    )
  ),
  name := "LibreCaptcha",
  libraryDependencies += "com.sksamuel.scrimage" % "scrimage-core" % "4.0.5",
  libraryDependencies += "com.sksamuel.scrimage" % "scrimage-filters" % "4.0.5",
  libraryDependencies += "org.json4s" % "json4s-jackson_2.13" % "3.6.9"
)

unmanagedResourceDirectories in Compile += { baseDirectory.value / "lib" }
scalacOptions ++= List(
  "-Yrangepos",
  "-Ywarn-unused"
)
javacOptions += "-g:none"
compileOrder := CompileOrder.JavaThenScala
mainClass in assembly := Some("lc.LCFramework")
mainClass in (Compile, run) := Some("lc.LCFramework")
assemblyJarName in assembly := "LibreCaptcha.jar"

fork in run := true
