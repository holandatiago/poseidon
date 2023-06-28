val app = crossProject(JSPlatform, JVMPlatform)
  .settings(
      version := "0.1",
      scalaVersion := "2.13.11",
      libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.12.0",
      libraryDependencies += "com.lihaoyi" %%% "autowire" % "0.3.3",
      libraryDependencies += "com.lihaoyi" %%% "upickle" % "3.1.0")
  .jsSettings(
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.6.0")
  .jvmSettings(
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.8.0",//"2.5.20",
      libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.5.0",//"10.0.15",
      libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
      libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0",//"2.5.20",
      libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.6",
      libraryDependencies += "com.opengamma.strata" % "strata-measure" % "2.12.21",
      libraryDependencies += "org.knowm.xchart" % "xchart" % "3.8.3",
      libraryDependencies += "org.scalanlp" %% "breeze" % "2.1.0",
      libraryDependencies += "org.scalanlp" %% "breeze-viz" % "2.1.0")

val copyMainDirectoryJS = taskKey[Unit]("Copy main JS directory to JVM")
copyMainDirectoryJS := IO.copyDirectory(
    (app.js / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value,
    (app.jvm / Compile / classDirectory).value)
