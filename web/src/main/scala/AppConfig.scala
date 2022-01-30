package funstack.web

import scala.scalajs.js

@js.native
trait WebsiteAppConfig extends js.Object {
  def url: String = js.native
}

@js.native
trait AuthAppConfig extends js.Object {
  def url: String         = js.native
  def clientId: String    = js.native
}

@js.native
trait WsAppConfig extends js.Object {
  def url: String                   = js.native
  def allowUnauthenticated: Boolean = js.native
}

@js.native
trait HttpAppConfig extends js.Object {
  def url: String = js.native
}

@js.native
trait AppConfigTyped[T] extends js.Object {
  def stage: String                      = js.native
  def website: WebsiteAppConfig          = js.native
  def auth: js.UndefOr[AuthAppConfig]    = js.native
  def http: js.UndefOr[HttpAppConfig]    = js.native
  def ws: js.UndefOr[WsAppConfig]        = js.native
  def environment: T                     = js.native
}
object AppConfig {
  import scala.scalajs.js.Dynamic.{global => g}

  def load() = g.AppConfig.asInstanceOf[AppConfig]
  def loadTyped[T]() = g.AppConfig.asInstanceOf[AppConfigTyped[T]]
}
