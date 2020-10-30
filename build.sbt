ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

val circeVersion = "0.14.0-M1"

lazy val root = (project in file("."))
  .settings(
    name := "word-count",
    libraryDependencies ++=Seq(
      "dev.zio" %% "zio" % "1.0.3",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.polynote" %% "uzhttp" % "0.2.6"
    )
  )