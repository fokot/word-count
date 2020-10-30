ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"



lazy val root = (project in file("."))
  .settings(
    name := "word-count",
    libraryDependencies ++=Seq(
      "dev.zio" %% "zio" % "1.0.3",
      "dev.zio" %% "zio-streams" % "1.0.3",
    )
  )