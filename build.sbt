/*
 * Copyright 2020-2021 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File

import com.typesafe.tools.mima.core._
import org.openqa.selenium.firefox.FirefoxOptions
import org.scalajs.jsenv.selenium.SeleniumJSEnv

ThisBuild / baseVersion := "3.1"

ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"

ThisBuild / startYear := Some(2020)
ThisBuild / endYear := Some(2021)

ThisBuild / developers := List(
  Developer("djspiewak", "Daniel Spiewak", "@djspiewak", url("https://github.com/djspiewak")),
  Developer("SystemFw", "Fabio Labella", "", url("https://github.com/SystemFw")),
  Developer("RaasAhsan", "Raas Ahsan", "", url("https://github.com/RaasAhsan")),
  Developer("TimWSpence", "Tim Spence", "@TimWSpence", url("https://github.com/TimWSpence")),
  Developer("kubukoz", "Jakub Kozłowski", "@kubukoz", url("https://github.com/kubukoz")),
  Developer("mpilquist", "Michael Pilquist", "@mpilquist", url("https://github.com/mpilquist")),
  Developer("vasilmkd", "Vasil Vasilev", "@vasilvasilev97", url("https://github.com/vasilmkd")),
  Developer("bplommer", "Ben Plommer", "@bplommer", url("https://github.com/bplommer")),
  Developer("wemrysi", "Emrys Ingersoll", "@wemrysi", url("https://github.com/wemrysi")),
  Developer("gvolpe", "Gabriel Volpe", "@volpegabriel87", url("https://github.com/gvolpe"))
)

val PrimaryOS = "ubuntu-latest"
val Windows = "windows-latest"

val ScalaJSJava = "adopt@1.8"
val Scala213 = "2.13.6"

ThisBuild / crossScalaVersions := Seq("3.0.0", "2.12.14", Scala213)

ThisBuild / githubWorkflowTargetBranches := Seq("series/3.x")

val LTSJava = "adopt@1.11"
val LatestJava = "adopt@1.16"
val GraalVM8 = "graalvm-ce-java8@21.1"

ThisBuild / githubWorkflowJavaVersions := Seq(ScalaJSJava, LTSJava, LatestJava, GraalVM8)
ThisBuild / githubWorkflowOSes := Seq(PrimaryOS, Windows)

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v2.1.2"),
    name = Some("Setup NodeJS v14 LTS"),
    params = Map("node-version" -> "14"),
    cond = Some("matrix.ci == 'ciJS'"))

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("${{ matrix.ci }}")),
  WorkflowStep.Sbt(
    List("docs/mdoc"),
    cond = Some(s"matrix.scala == '$Scala213' && matrix.ci == 'ciJVM'")),
  WorkflowStep.Sbt(
    List("exampleJVM/compile"),
    cond = Some(s"matrix.ci == 'ciJVM' && matrix.os == '$PrimaryOS'")),
  WorkflowStep.Sbt(
    List("exampleJS/compile"),
    cond = Some(s"matrix.ci == 'ciJS' && matrix.os == '$PrimaryOS'")),
  WorkflowStep.Run(
    List("example/test-jvm.sh ${{ matrix.scala }}"),
    name = Some("Test Example JVM App Within Sbt"),
    cond = Some(s"matrix.ci == 'ciJVM' && matrix.os == '$PrimaryOS'")
  ),
  WorkflowStep.Run(
    List("example/test-js.sh ${{ matrix.scala }}"),
    name = Some("Test Example JavaScript App Using Node"),
    cond = Some(s"matrix.ci == 'ciJS' && matrix.os == '$PrimaryOS'")
  ),
  WorkflowStep.Run(
    List("cd scalafix", "sbt test"),
    name = Some("Scalafix tests"),
    cond =
      Some(s"matrix.scala == '$Scala213' && matrix.ci == 'ciJVM' && matrix.os == '$PrimaryOS'")
  )
)

val ciVariants = List("ciJVM", "ciJS", "ciFirefox")
ThisBuild / githubWorkflowBuildMatrixAdditions += "ci" -> ciVariants

ThisBuild / githubWorkflowBuildMatrixExclusions ++= {
  val windowsScalaFilters =
    (ThisBuild / githubWorkflowScalaVersions).value.filterNot(Set(Scala213)).map { scala =>
      MatrixExclude(Map("os" -> Windows, "scala" -> scala))
    }

  Seq("ciJS", "ciFirefox").flatMap { ci =>
    val javaFilters =
      (ThisBuild / githubWorkflowJavaVersions).value.filterNot(Set(ScalaJSJava)).map { java =>
        MatrixExclude(Map("ci" -> ci, "java" -> java))
      }

    javaFilters ++ windowsScalaFilters :+ MatrixExclude(Map("os" -> Windows, "ci" -> ci))
  }
}

ThisBuild / githubWorkflowBuildMatrixExclusions ++= Seq(
  MatrixExclude(Map("java" -> LatestJava, "os" -> Windows))
)

lazy val unidoc213 = taskKey[Seq[File]]("Run unidoc but only on Scala 2.13")

lazy val useFirefoxEnv =
  settingKey[Boolean]("Use headless Firefox (via geckodriver) for running tests")
Global / useFirefoxEnv := false

ThisBuild / Test / jsEnv := {
  val old = (Test / jsEnv).value

  if (useFirefoxEnv.value) {
    val options = new FirefoxOptions()
    options.addArguments("-headless")
    new SeleniumJSEnv(options)
  } else {
    old
  }
}

ThisBuild / homepage := Some(url("https://github.com/typelevel/cats-effect"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/typelevel/cats-effect"),
    "git@github.com:typelevel/cats-effect.git"))

ThisBuild / apiURL := Some(url("https://typelevel.org/cats-effect/api/3.x/"))

ThisBuild / autoAPIMappings := true

val CatsVersion = "2.6.1"
val Specs2Version = "4.12.2"
val ScalaCheckVersion = "1.15.4"
val DisciplineVersion = "1.1.6"
val CoopVersion = "1.1.1"

replaceCommandAlias(
  "ci",
  "; project /; headerCheck; scalafmtCheck; clean; test; coreJVM/mimaReportBinaryIssues; root/unidoc213; set Global / useFirefoxEnv := true; testsJS/test; set Global / useFirefoxEnv := false"
)

addCommandAlias(
  "ciJVM",
  "; project rootJVM; headerCheck; scalafmtCheck; clean; test; mimaReportBinaryIssues; root/unidoc213")
addCommandAlias("ciJS", "; project rootJS; headerCheck; scalafmtCheck; clean; test")

// we do the firefox ci *only* on core because we're only really interested in IO here
addCommandAlias(
  "ciFirefox",
  "; set Global / useFirefoxEnv := true; project rootJS; headerCheck; scalafmtCheck; clean; testsJS/test; set Global / useFirefoxEnv := false"
)

addCommandAlias("prePR", "; root/clean; +root/scalafmtAll; +root/headerCreate")

val jsProjects: Seq[ProjectReference] =
  Seq(kernel.js, kernelTestkit.js, laws.js, core.js, testkit.js, tests.js, std.js, example.js)

val undocumentedRefs =
  jsProjects ++ Seq[ProjectReference](benchmarks, example.jvm)

lazy val root = project
  .in(file("."))
  .aggregate(rootJVM, rootJS)
  .enablePlugins(NoPublishPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    ScalaUnidoc / unidoc / unidocProjectFilter := {
      undocumentedRefs.foldLeft(inAnyProject)((acc, a) => acc -- inProjects(a))
    },
    Compile / unidoc213 := Def.taskDyn {
      if (scalaVersion.value.startsWith("2.13"))
        Def.task((Compile / unidoc).value)
      else
        Def.task {
          streams.value.log.warn(s"Skipping unidoc execution in Scala ${scalaVersion.value}")
          Seq.empty[File]
        }
    }.value
  )

lazy val rootJVM = project
  .aggregate(
    kernel.jvm,
    kernelTestkit.jvm,
    laws.jvm,
    core.jvm,
    testkit.jvm,
    tests.jvm,
    std.jvm,
    example.jvm,
    benchmarks)
  .enablePlugins(NoPublishPlugin)

lazy val rootJS = project.aggregate(jsProjects: _*).enablePlugins(NoPublishPlugin)

/**
 * The core abstractions and syntax. This is the most general definition of Cats Effect,
 * without any concrete implementations. This is the "batteries not included" dependency.
 */
lazy val kernel = crossProject(JSPlatform, JVMPlatform)
  .in(file("kernel"))
  .settings(
    name := "cats-effect-kernel",
    libraryDependencies ++= Seq(
      ("org.specs2" %%% "specs2-core" % Specs2Version % Test).cross(CrossVersion.for3Use2_13),
      "org.typelevel" %%% "cats-core" % CatsVersion))
  .jsSettings(
    Compile / doc / sources := {
      if (isDotty.value)
        Seq()
      else
        (Compile / doc / sources).value
    })

/**
 * Reference implementations (including a pure ConcurrentBracket), generic ScalaCheck
 * generators, and useful tools for testing code written against Cats Effect.
 */
lazy val kernelTestkit = crossProject(JSPlatform, JVMPlatform)
  .in(file("kernel-testkit"))
  .dependsOn(kernel)
  .settings(
    name := "cats-effect-kernel-testkit",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-free" % CatsVersion,
      "org.scalacheck" %%% "scalacheck" % ScalaCheckVersion,
      "org.typelevel" %%% "coop" % CoopVersion)
  )

/**
 * The laws which constrain the abstractions. This is split from kernel to avoid
 * jar file and dependency issues. As a consequence of this split, some things
 * which are defined in kernelTestkit are *tested* in the Test scope of this project.
 */
lazy val laws = crossProject(JSPlatform, JVMPlatform)
  .in(file("laws"))
  .dependsOn(kernel, kernelTestkit % Test)
  .settings(
    name := "cats-effect-laws",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-laws" % CatsVersion,
      "org.typelevel" %%% "discipline-specs2" % DisciplineVersion % Test)
  )

/**
 * Concrete, production-grade implementations of the abstractions. Or, more
 * simply-put: IO and Resource. Also contains some general datatypes built
 * on top of IO which are useful in their own right, as well as some utilities
 * (such as IOApp). This is the "batteries included" dependency.
 */
lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .dependsOn(kernel, std)
  .settings(
    name := "cats-effect",
    mimaBinaryIssueFilters ++= Seq(
      // introduced by #1837, removal of package private class
      ProblemFilters.exclude[MissingClassProblem]("cats.effect.AsyncPropagateCancelation"),
      ProblemFilters.exclude[MissingClassProblem]("cats.effect.AsyncPropagateCancelation$"),
      // introduced by #1913, striped fiber callback hashtable, changes to package private code
      ProblemFilters.exclude[MissingClassProblem]("cats.effect.unsafe.FiberErrorHashtable"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("cats.effect.unsafe.IORuntime.fiberErrorCbs"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("cats.effect.unsafe.IORuntime.this"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("cats.effect.unsafe.IORuntime.<init>$default$6"),
      // introduced by #1928, wake up a worker thread before spawning a helper thread when blocking
      // changes to `cats.effect.unsafe` package private code
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("cats.effect.unsafe.WorkStealingThreadPool.notifyParked"),
      // introduced by #2041, Rewrite and improve `ThreadSafeHashtable`
      // changes to `cats.effect.unsafe` package private code
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.unsafe.ThreadSafeHashtable.hashtable"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.unsafe.ThreadSafeHashtable.hashtable_="),
      // introduced by #2051, Tracing
      // changes to package private code
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Blocking.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Blocking.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Blocking.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Delay.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Delay.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Delay.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#FlatMap.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#FlatMap.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#FlatMap.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#HandleErrorWith.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#HandleErrorWith.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#HandleErrorWith.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Map.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Map.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Map.this"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Uncancelable.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Uncancelable.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#Uncancelable.this"),
      // introduced by #2065, Add tracing event to `IO.async`
      // changes to package private code
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#IOCont.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#IOCont.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("cats.effect.IO#IOCont.this")
    )
  )
  .jvmSettings(
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
  )

/**
 * Test support for the core project, providing various helpful instances
 * like ScalaCheck generators for IO and SyncIO.
 */
lazy val testkit = crossProject(JSPlatform, JVMPlatform)
  .in(file("testkit"))
  .dependsOn(core, kernelTestkit)
  .settings(
    name := "cats-effect-testkit",
    libraryDependencies ++= Seq("org.scalacheck" %%% "scalacheck" % ScalaCheckVersion))

/**
 * Unit tests for the core project, utilizing the support provided by testkit.
 */
lazy val tests = crossProject(JSPlatform, JVMPlatform)
  .in(file("tests"))
  .dependsOn(laws % Test, kernelTestkit % Test, testkit % Test)
  .enablePlugins(NoPublishPlugin)
  .settings(
    name := "cats-effect-tests",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "discipline-specs2" % DisciplineVersion % Test,
      "org.typelevel" %%% "cats-kernel-laws" % CatsVersion % Test)
  )
  .jvmSettings(
    Test / fork := true,
    Test / javaOptions += s"-Dsbt.classpath=${(Test / fullClasspath).value.map(_.data.getAbsolutePath).mkString(File.pathSeparator)}")

/**
 * Implementations lof standard functionality (e.g. Semaphore, Console, Queue)
 * purely in terms of the typeclasses, with no dependency on IO. In most cases,
 * the *tests* for these implementations will require IO, and thus those tests
 * will be located within the core project.
 */
lazy val std = crossProject(JSPlatform, JVMPlatform)
  .in(file("std"))
  .dependsOn(kernel)
  .settings(
    name := "cats-effect-std",
    libraryDependencies += {
      if (isDotty.value)
        ("org.specs2" %%% "specs2-scalacheck" % Specs2Version % Test)
          .cross(CrossVersion.for3Use2_13)
          .exclude("org.scalacheck", "scalacheck_2.13")
          .exclude("org.scalacheck", "scalacheck_sjs1_2.13")
      else
        "org.specs2" %%% "specs2-scalacheck" % Specs2Version % Test
    },
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % ScalaCheckVersion % Test,
    libraryDependencies ++= {
      if (!isDotty.value)
        Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided")
      else Seq()
    },
    Compile / unmanagedSourceDirectories ++= {
      if (!isDotty.value)
        Seq(
          (Compile / baseDirectory)
            .value
            .getParentFile() / "shared" / "src" / "main" / "scala-2")
      else Seq()
    }
  )

/**
 * A trivial pair of trivial example apps primarily used to show that IOApp
 * works as a practical runtime on both target platforms.
 */
lazy val example = crossProject(JSPlatform, JVMPlatform)
  .in(file("example"))
  .dependsOn(core)
  .enablePlugins(NoPublishPlugin)
  .settings(name := "cats-effect-example")
  .jsSettings(scalaJSUseMainModuleInitializer := true)

/**
 * JMH benchmarks for IO and other things.
 */
lazy val benchmarks = project
  .in(file("benchmarks"))
  .dependsOn(core.jvm)
  .settings(name := "cats-effect-benchmarks")
  .enablePlugins(NoPublishPlugin, JmhPlugin)

lazy val docs = project
  .in(file("site-docs"))
  .dependsOn(core.jvm)
  .enablePlugins(MdocPlugin)
