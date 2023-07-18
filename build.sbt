ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.11"
Compile / run := (back / Compile / run).evaluated

lazy val back = project
  .enablePlugins(JavaAppPackaging)
  .dependsOn(middle)
  .settings(
    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.5",
    libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.22",
    libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.22",
    libraryDependencies += "org.http4s" %% "http4s-scalatags" % "0.25.2",
    libraryDependencies += "org.http4s" %% "http4s-ember-server" % "0.23.22",
    libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.12.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.8.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.5.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
//    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.7",
    libraryDependencies += "org.scalanlp" %% "breeze" % "2.1.0",
    Compile / resourceDirectory := (front / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value,
    Compile / unmanagedResources := (Compile / unmanagedResources).dependsOn(front / Compile / fullLinkJS).value,
  )

lazy val front = project
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(middle)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "com.raquo" %%% "laminar" % "15.0.1",
    libraryDependencies += "io.circe" %%% "circe-generic" % "0.14.5",
    libraryDependencies += "io.circe" %%% "circe-parser" % "0.14.5",
    Compile / unmanagedSourceDirectories ++= (middle / Compile / unmanagedSourceDirectories).value,
  )

lazy val middle = project
