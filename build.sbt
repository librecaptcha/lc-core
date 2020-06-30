lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.13.2",
      version      := "0.1.0-SNAPSHOT")),
    name := "LibreCaptcha",

    libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-core" % "4.0.5",

    libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-io-extra" % "4.0.5",

    libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-filters" % "4.0.5",
    
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.9"
   
)

unmanagedResourceDirectories in Compile += {baseDirectory.value / "lib"}
javacOptions += "-g:none"
compileOrder := CompileOrder.JavaThenScala

fork in run := true
