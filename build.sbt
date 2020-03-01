import play.core.PlayVersion.akkaVersion

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "my-play-scala-websocket-example",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.12.10",
    libraryDependencies ++= Seq(
      ehcache,
      specs2 % Test,
      guice,
      ws,
      "org.webjars" % "flot" % "0.8.3-1",
      "org.webjars" % "bootstrap" % "3.3.7",
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
      "org.awaitility" % "awaitility" % "4.0.1" % Test,
      "com.h2database" % "h2" % "1.4.192",
      "com.typesafe.play" %% "play-slick" % "4.0.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "4.0.0",
      "com.yahoofinance-api" % "YahooFinanceAPI" % "3.15.0"
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    )
  )
