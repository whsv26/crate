val Http4sVersion = "0.21.24"
val CirceVersion = "0.13.0"
val MunitVersion = "0.7.20"
val LogbackVersion = "1.2.3"
val MunitCatsEffectVersion = "0.13.0"
val enumeratumCirceVersion = "1.6.1"
val pureConfigVersion = "0.16.0"
val doobieVersion = "0.12.1"
val macWireVersion = "2.3.7"

lazy val root = (project in file("."))
  .settings(
    organization := "org.whsv26",
    name := "crate",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.6",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
      "org.typelevel"   %% "munit-cats-effect-2" % MunitCatsEffectVersion % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "org.scalameta"   %% "svm-subs"            % "20.2.0"
    ),

    libraryDependencies ++= Seq(
      "com.beachape" %% "enumeratum-circe" % enumeratumCirceVersion,
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
    ),

    libraryDependencies ++= Seq(
      // Start with this one
      "org.tpolecat" %% "doobie-core"      % doobieVersion,

      // And add any of these as needed
      "org.tpolecat" %% "doobie-h2"        % doobieVersion,          // H2 driver 1.4.200 + type mappings.
      "org.tpolecat" %% "doobie-hikari"    % doobieVersion,          // HikariCP transactor.
      "org.tpolecat" %% "doobie-postgres"  % doobieVersion,          // Postgres driver 42.2.19 + type mappings.
      "org.tpolecat" %% "doobie-quill"     % doobieVersion,          // Support for Quill 3.6.1
      "org.tpolecat" %% "doobie-specs2"    % doobieVersion % "test", // Specs2 support for typechecking statements.
      "org.tpolecat" %% "doobie-scalatest" % doobieVersion % "test"  // ScalaTest support for typechecking statements.
    ),

//    libraryDependencies ++= Seq(
//      "com.softwaremill.macwire" %% "macros" % macWireVersion % "provided",
//      "com.softwaremill.macwire" %% "macrosakka" % macWireVersion % "provided",
//      "com.softwaremill.macwire" %% "util" % macWireVersion,
//      "com.softwaremill.macwire" %% "proxy" % macWireVersion,
//    ),

    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.0" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )
