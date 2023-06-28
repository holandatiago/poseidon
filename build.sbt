version := "0.1"
scalaVersion := "2.13.11"

lazy val poseidon = (project in file("."))
  .settings(
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.8.0",
      libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.5.0",
      libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
      libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0",
      libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.6",
      libraryDependencies += "com.opengamma.strata" % "strata-measure" % "2.12.21",
      libraryDependencies += "org.knowm.xchart" % "xchart" % "3.8.3",
      libraryDependencies += "org.scalanlp" %% "breeze" % "2.1.0",
      libraryDependencies += "org.scalanlp" %% "breeze-viz" % "2.1.0",
  )
