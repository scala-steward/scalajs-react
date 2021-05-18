import sbt._
import sbt.Keys._
import com.jsuereth.sbtpgp.PgpKeys._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import xerial.sbt.Sonatype.autoImport._
//import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin
//import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._

object Lib {
  import Dependencies.Dep

  type PE = Project => Project

  val ghProject = "scalajs-react"

  def byScalaVersion[A](f: PartialFunction[(Long, Long), Seq[A]]): Def.Initialize[Seq[A]] =
    Def.setting(CrossVersion.partialVersion(scalaVersion.value).flatMap(f.lift).getOrElse(Nil))

  def publicationSettings: PE =
    _.configure(sourceMapsToGithub(ghProject))
      .settings(
        publishTo := sonatypePublishToBundle.value,
        pomExtra :=
          <scm>
            <connection>scm:git:github.com/japgolly/{ghProject}</connection>
            <developerConnection>scm:git:git@github.com:japgolly/{ghProject}.git</developerConnection>
            <url>github.com:japgolly/{ghProject}.git</url>
          </scm>
          <developers>
            <developer>
              <id>japgolly</id>
              <name>David Barri</name>
            </developer>
          </developers>)

  def sourceMapsToGithub(ghProject: String): PE =
    p => p.settings(
      scalacOptions ++= (if (isSnapshot.value) Seq.empty else Seq({
        val a = p.base.toURI.toString.replaceFirst("[^/]+/?$", "")
        val g = s"https://raw.githubusercontent.com/japgolly/$ghProject"
        s"-P:scalajs:mapSourceURI:$a->$g/v${version.value}/"
      }))
    )

  def preventPublication: PE =
    _.settings(publish / skip := true)

  def utestSettings: PE =
    _.configure(InBrowserTesting.js)
      .settings(
        jsEnv                := new JSDOMNodeJSEnv,
        Test / scalacOptions += "-language:reflectiveCalls",
        libraryDependencies  += Dep.MTest.value % Test,
        testFrameworks       += new TestFramework("utest.runner.Framework"))

  def extModuleName(shortName: String): PE =
    _.settings(name := s"ext-$shortName")

  def definesMacros: Project => Project =
    _.settings(
      scalacOptions += "-language:experimental.macros",
      libraryDependencies ++= Seq(
        Dep.ScalaReflect.value,
        Dep.ScalaCompiler.value % Provided))

  def hasNoTests: Project => Project =
    _.settings(
      Test / fastOptJS     := Attributed((Test / fastOptJS / artifactPath).value)(AttributeMap.empty),
      Test / fullOptJS     := Attributed((Test / fullOptJS / artifactPath).value)(AttributeMap.empty),
      Test / sbt.Keys.test := { (Test / compile).value; () },
      Test / testOnly      := { (Test / compile).value; () },
      Test / testQuick     := { (Test / compile).value; () })

  /*
  lazy val yarnOnPath: Boolean =
    try {
      Process("yarn --version").!!
      true
    } catch {
      case t: Throwable => false
    }

  def useScalaJsBundler: PE =
    _.enablePlugins(ScalaJSBundlerPlugin)
      .settings(
        // useYarn := yarnOnPath,
        version in webpack := "2.6.1")
  */

  val disable = settingKey[Boolean]("Disable project?")

  def conditionallyDisable: Project => Project = {

    def clearWhenDisabled[A](key: SettingKey[Seq[A]]) =
      Def.setting[Seq[A]] {
        val disabled = disable.value
        val as = key.value
        if (disabled) Nil else as
      }

    _.settings(
      libraryDependencies := clearWhenDisabled(libraryDependencies).value,
      Compile / unmanagedSourceDirectories := clearWhenDisabled(Compile / unmanagedSourceDirectories).value,
      Test / unmanagedSourceDirectories := clearWhenDisabled(Test / unmanagedSourceDirectories).value,
      publish / skip := ((publish / skip).value || disable.value),
    )
  }
}