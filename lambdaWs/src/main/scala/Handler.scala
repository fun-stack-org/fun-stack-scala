package funstack.lambda.ws

import net.exoego.facade.aws_lambda._

import funstack.core.StringSerdes
import scala.scalajs.js
import mycelium.core.message._
import sloth._
import chameleon.{Serializer, Deserializer}
import scala.scalajs.js.JSConverters._
import cats.effect.IO
import cats.data.Kleisli
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

object Handler {

  type AnyPickle = Boolean // workaround for generic type ServerMessage, where one or two type params can be anything. Nothing does not work, because it hangs with e.g. boopickle.

  case class WsAuth(sub: String, username: String)
  case class WsRequest(event: APIGatewayWSEvent, context: Context, auth: Option[WsAuth])

  type FunctionType = js.Function2[APIGatewayWSEvent, Context, js.Promise[APIGatewayProxyStructuredResultV2]]

  type FutureFunc[Out]    = WsRequest => Future[Out]
  type FutureKleisli[Out] = Kleisli[Future, WsRequest, Out]
  type IOFunc[Out]        = WsRequest => IO[Out]
  type IOKleisli[Out]     = Kleisli[IO, WsRequest, Out]

  def handle[T](
      router: Router[T, IO],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, AnyPickle], StringSerdes],
  ): FunctionType = handleF[T, AnyPickle, IO](router, _.map(Right.apply).unsafeToFuture())

  def handleFuture[T](
      router: Router[T, Future],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, AnyPickle], StringSerdes],
  ): FunctionType = handleF[T, AnyPickle, Future](router, _.map(Right.apply))

  def handleF[T, Failure, F[_]](
      router: Router[T, F],
      execute: F[T] => Future[Either[Failure, T]],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, Failure], StringSerdes],
  ): FunctionType = handleFWithContext[T, Failure, F](router, (f, _) => execute(f))

  def handle[T](
      router: WsRequest => Router[T, IO],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, AnyPickle], StringSerdes],
  ): FunctionType = handleF[T, AnyPickle, IO](router, _.map(Right.apply).unsafeToFuture())

  def handleFuture[T](
      router: WsRequest => Router[T, Future],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, AnyPickle], StringSerdes],
  ): FunctionType = handleF[T, AnyPickle, Future](router, _.map(Right.apply))

  def handleF[T, Failure, F[_]](
      router: WsRequest => Router[T, F],
      execute: F[T] => Future[Either[Failure, T]],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, Failure], StringSerdes],
  ): FunctionType = handleFCustom[T, Failure, F](router, (f, _) => execute(f))

  def handleFunc[T](
      router: Router[T, IOFunc],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, AnyPickle], StringSerdes],
  ): FunctionType = handleFWithContext[T, AnyPickle, IOFunc](router, (f, ctx) => f(ctx).map(Right.apply).unsafeToFuture())

  def handleKleisli[T](
      router: Router[T, IOKleisli],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, AnyPickle], StringSerdes],
  ): FunctionType = handleFWithContext[T, AnyPickle, IOKleisli](router, (f, ctx) => f(ctx).map(Right.apply).unsafeToFuture())

  def handleFutureKleisli[T](
      router: Router[T, FutureKleisli],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, AnyPickle], StringSerdes],
  ): FunctionType = handleFWithContext[T, AnyPickle, FutureKleisli](router, (f, ctx) => f(ctx).map(Right.apply))

  def handleFutureFunc[T](
      router: Router[T, FutureFunc],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, AnyPickle], StringSerdes],
  ): FunctionType = handleFWithContext[T, AnyPickle, FutureFunc](router, (f, ctx) => f(ctx).map(Right.apply))

  def handleFWithContext[T, Failure, F[_]](
      router: Router[T, F],
      execute: (F[T], WsRequest) => Future[Either[Failure, T]],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, Failure], StringSerdes],
  ): FunctionType = handleFCustom[T, Failure, F](_ => router, execute)

  def handleFCustom[T, Failure, F[_]](
      routerf: WsRequest => Router[T, F],
      execute: (F[T], WsRequest) => Future[Either[Failure, T]],
  )(implicit
      deserializer: Deserializer[ClientMessage[T], StringSerdes],
      serializer: Serializer[ServerMessage[T, AnyPickle, Failure], StringSerdes],
  ): FunctionType = { (event, context) =>
    // println(js.JSON.stringify(event))
    // println(js.JSON.stringify(context))

    val auth = event.requestContext.authorizer.toOption.flatMap { claims =>
      for {
        sub <- claims.get("sub")
        username <- claims.get("username")
      } yield WsAuth(sub = sub, username = username)
    }
    val request = WsRequest(event, context, auth)
    val router = routerf(request)

    val result: js.Promise[ServerMessage[T, AnyPickle, Failure]] = Deserializer[ClientMessage[T], StringSerdes].deserialize(StringSerdes(event.body)) match {
      case Left(error) => js.Promise.reject(new Exception(s"Deserializer: $error"))
      case Right(Ping) => js.Promise.resolve[Pong.type](Pong)
      case Right(CallRequest(seqNumber, path, payload)) =>
        router(Request(path, payload)).toEither match {
          case Right(result) => execute(result, request).toJSPromise.`then`[ServerMessage[T, AnyPickle, Failure]](CallResponse(seqNumber, _))
          case Left(error)   => js.Promise.reject(new Exception(error.toString))
        }
    }

    result
      .`then`[StringSerdes](Serializer[ServerMessage[T, AnyPickle, Failure], StringSerdes].serialize)
      .`then`[APIGatewayProxyStructuredResultV2](
        payload => APIGatewayProxyStructuredResultV2(body = payload.value, statusCode = 200),
        (((e: Any) => APIGatewayProxyStructuredResultV2(body = e.toString, statusCode = 500)): js.Function1[
          Any,
          APIGatewayProxyStructuredResultV2,
        ]): js.UndefOr[js.Function1[Any, APIGatewayProxyStructuredResultV2]],
      )
      .`then`[APIGatewayProxyStructuredResultV2]({ (result: APIGatewayProxyStructuredResultV2) =>
        println(js.JSON.stringify(result))
        result: APIGatewayProxyStructuredResultV2
      }: js.Function1[APIGatewayProxyStructuredResultV2, APIGatewayProxyStructuredResultV2])
  }
}
