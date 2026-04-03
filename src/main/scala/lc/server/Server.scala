package lc.server

import zio.blocks.schema._
import zio.blocks.schema.json._
import lc.core.CaptchaManager
import lc.core.ErrorMessageEnum
import lc.core.{Answer, ByteConvert, Error, Id, Parameters}
import org.limium.picoserve
import org.limium.picoserve.Server.{ByteResponse, ServerBuilder, StringResponse}
import scala.io.Source
import java.net.InetSocketAddress
import java.util
import java.nio.ByteBuffer
import scala.jdk.CollectionConverters._

class Server(
    address: String,
    port: Int,
    captchaManager: CaptchaManager,
    playgroundEnabled: Boolean,
    corsHeader: String,
    authRequired: Boolean = false,
    authKey: Option[String] = None
) {
  var headerMap: util.Map[String, util.List[String]] = null
  if (corsHeader.nonEmpty) {
    headerMap = Map("Access-Control-Allow-Origin" -> List(corsHeader).asJava).asJava
  }

  private def checkAuth(request: picoserve.Server#Request): Boolean = {
    if (!authRequired) return true
    val headers = request.getHeaders()
    if (headers != null && headers.containsKey("Auth")) {
      val authHeaderValues = headers.get("Auth")
      if (authHeaderValues != null && authHeaderValues.size() > 0) {
        val authHeader = authHeaderValues.get(0)
        val expectedKey = authKey.getOrElse("")
        return authHeader == expectedKey
      }
    }
    false
  }

  val serverBuilder: ServerBuilder = picoserve.Server
    .builder()
    .address(new InetSocketAddress(address, port))
    .backlog(32)
    .POST(
      "/v2/captcha",
      (request) => {
        if (!checkAuth(request)) {
          new StringResponse(401, "Unauthorized", headerMap)
        } else {
          val bodyStr = request.getBodyString().trim.replaceAll("\u0000", "")
          val paramEither = Parameters.codec.decode(ByteBuffer.wrap(bodyStr.getBytes("UTF-8")))
          paramEither match {
            case Right(param) =>
              val id = captchaManager.getChallenge(param)
              getResponse(id, headerMap)
            case Left(err) =>
              getResponse(Left(Error("Invalid parameters: " + err.toString)), headerMap)
          }
        }
      }
    )
    .GET(
      "/v2/media",
      (request) => {
        if (!checkAuth(request)) {
          new StringResponse(401, "Unauthorized", headerMap)
        } else {
          val params = request.getQueryParams()
          val result = if (params.containsKey("id")) {
            val paramId = params.get("id").get(0)
            val id = Id(paramId)
            captchaManager.getCaptcha(id)
          } else {
            Left(Error(ErrorMessageEnum.INVALID_PARAM.toString + "=> id"))
          }
          getResponse(result, headerMap)
        }
      }
    )
    .POST(
      "/v2/answer",
      (request) => {
        if (!checkAuth(request)) {
          new StringResponse(401, "Unauthorized", headerMap)
        } else {
          val bodyStr = request.getBodyString().trim.replaceAll("\u0000", "")
          val answerEither = Answer.codec.decode(ByteBuffer.wrap(bodyStr.getBytes("UTF-8")))
          answerEither match {
            case Right(answer) =>
              val result = captchaManager.checkAnswer(answer)
              getResponse(result, headerMap)
            case Left(err) =>
              getResponse(Left(Error("Invalid answer format: " + err.toString)), headerMap)
          }
        }
      }
    )
  if (playgroundEnabled) {
    val htmlHeaderMap = Map("Content-Type" -> List("text/html").asJava).asJava
    serverBuilder.GET(
      "/demo/index.html",
      (_) => {
        val resStream = getClass().getResourceAsStream("/index.html")
        val str = Source.fromInputStream(resStream).mkString
        new StringResponse(200, str, htmlHeaderMap)
      }
    )
    serverBuilder.GET(
      "/",
      (_) => {
        val str = """
        <html>
          <h2>Welcome to LibreCaptcha server</h2>
          <h3><a href="/demo/index.html">Link to Demo</a></h3>
          <h3>API is served at <b>/v2/</b></h3>
        </html>
        """;
        new StringResponse(200, str, htmlHeaderMap)
      }
    )
    println("Playground enabled on /demo/index.html")
  }

  val server: picoserve.Server = serverBuilder.build()

  private def getResponse(
      response: Either[Error, ByteConvert],
      responseHeaders: util.Map[String, util.List[String]]
  ): ByteResponse = {
    response match {
      case Right(value) => {
        new ByteResponse(200, value.toBytes(), responseHeaders)
      }
      case Left(value) => {
        new ByteResponse(500, value.toBytes(), responseHeaders)
      }
    }
  }

  def start(): Unit = {
    println("Starting server on " + address + ":" + port)
    server.start()
  }

  def stop(): Unit = {
    println("Stopping server...")
    server.stop(0)
  }
}
