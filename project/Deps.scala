import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object Deps {
  import Def.{setting => s}

  // testing
  val scalatest = s("org.scalatest" %%% "scalatest" % "3.2.19")

  // core libraries
  val cats = new {
    val effect = s("org.typelevel" %%% "cats-effect" % "3.5.7")
  }

  // frp
  val colibri = new {
    val version = "0.8.0"
    val core    = s("com.github.cornerman" %%% "colibri" % version)
    val jsdom   = s("com.github.cornerman" %%% "colibri-jsdom" % version)
  }

  // rpc
  val sloth     = s("com.github.cornerman" %%% "sloth" % "0.8.0")
  val chameleon = s("com.github.cornerman" %%% "chameleon" % "0.4.1")

  // websocket connecitivity
  val mycelium = new {
    val version  = "0.4.0"
    val core     = s("com.github.cornerman" %%% "mycelium-core" % version)
    val clientJs = s("com.github.cornerman" %%% "mycelium-client-js" % version)
  }

  // js utils
  val jsTime = s("io.github.cquiroz" %%% "scala-java-time" % "2.5.0")

  // sttp
  val apiSpec = new {
    val version = "0.7.2"
    val circe   = s("com.softwaremill.sttp.apispec" %%% "openapi-circe" % version)
  }

  val tapir = new {
    val version    = "1.9.2"
    val core       = s("com.softwaremill.sttp.tapir" %%% "tapir-core" % version)
    val lambda     = s("com.softwaremill.sttp.tapir" %%% "tapir-aws-lambda" % version)
    val circe      = s("com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % version)
    val openApi    = s("com.softwaremill.sttp.tapir" %%% "tapir-openapi-docs" % version)
    val jsClient   = s("com.softwaremill.sttp.tapir" %%% "tapir-sttp-client" % version)
    val catsClient = s("com.softwaremill.sttp.client3" %%% "cats" % "3.10.2")
  }

  // aws-sdk-js
  val awsSdkJS    = new {
    val version                 = s"0.33.0-v${NpmDeps.awsSdkVersion}"
    val lambda                  = s("net.exoego" %%% "aws-sdk-scalajs-facade-lambda" % version)
    val dynamodb                = s("net.exoego" %%% "aws-sdk-scalajs-facade-dynamodb" % version)
    val apigatewaymanagementapi = s("net.exoego" %%% "aws-sdk-scalajs-facade-apigatewaymanagementapi" % version)
    val cognitoidentityprovider = s("net.exoego" %%% "aws-sdk-scalajs-facade-cognitoidentityprovider" % version)
    val sns                     = s("net.exoego" %%% "aws-sdk-scalajs-facade-sns" % version)
  }
  val awsLambdaJS = s("net.exoego" %%% "aws-lambda-scalajs-facade" % "0.12.1")
}

object NpmDeps {
  val awsSdkVersion = "2.892.0"
  val awsSdk        = "aws-sdk"    -> awsSdkVersion
  val jwtDecode     = "jwt-decode" -> "3.1.2"
  val nodeFetch     = "node-fetch" -> "2.6.7"
  val ws            = "ws"         -> "8.10.0"
  val open          = "open"       -> "8.4.0"
}
