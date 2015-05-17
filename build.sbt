
lazy val commonSettings = Seq(
  organization := "com.strongfellow",
  version := "0.0.1",
  scalaVersion := "2.10.5"
)

lazy val root = (project in file(".")).
  aggregate(util, core)

lazy val util = (project in file("util")).
  settings(commonSettings: _*).
  settings(
    name := "util"
  )

lazy val core = (project in file("core")).
  dependsOn("util").
  settings(commonSettings: _*).
  settings(
    name := "core"
  )
