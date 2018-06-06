import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.3",
      version      := "0.1.0-SNAPSHOT")),
    name := "LibreCaptcha",
    libraryDependencies += scalaTest % Test,

    libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",

    libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-io-extra" % "2.1.8",

    libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-filters" % "2.1.8"
   
)

fork in run := true
