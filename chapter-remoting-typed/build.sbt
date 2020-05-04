name := "goticks-typed"

version := "1.0"

organization := "com.goticks"

libraryDependencies ++= {
  val akkaVersion     = "2.6.5"
  val akkaHttpVersion = "10.1.11"
  Seq(
    "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"               % akkaVersion,
    "com.typesafe.akka" %% "akka-remote"              % akkaVersion,
    "com.typesafe.akka" %% "akka-multi-node-testkit"  % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % "test",
    "org.scalatest"     %% "scalatest"                % "3.1.1" % "test",
    "com.typesafe.akka" %% "akka-http-core"           % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
    "ch.qos.logback"    % "logback-classic"           % "1.2.3"
  )
}

// Assembly settings
mainClass in Global := Some("com.goticks.SingleNodeMain")

assemblyJarName in assembly := "goticks-server.jar"
