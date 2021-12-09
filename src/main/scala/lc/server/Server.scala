package lc.server

import org.json4s.jackson.JsonMethods.parse
import lc.core.Captcha
import lc.core.ErrorMessageEnum
import lc.core.{Answer, ByteConvert, Error, Id, Parameters}
import lc.core.Config.formats
import org.limium.picoserve
import org.limium.picoserve.Server.{ByteResponse, ServerBuilder, StringResponse}
import scala.io.Source
import java.net.InetSocketAddress
import java.util
import scala.jdk.CollectionConverters._

class Server(address: String, port: Int, captcha: Captcha, playgroundEnabled: Boolean, corsHeader: String) {
  var headerMap: util.Map[String, util.List[String]] = _
  if( corsHeader.nonEmpty ) {
    headerMap = Map("Access-Control-Allow-Origin" -> List(corsHeader).asJava).asJava
  }
  val serverBuilder: ServerBuilder = picoserve.Server
    .builder()
    .address(new InetSocketAddress(address, port))
    .backlog(32)
    .POST(
      "/v1/captcha",
      (request) => {
        val json = parse(request.getBodyString())
        val param = json.extract[Parameters]
        val id = captcha.getChallenge(param)
        getResponse(id, headerMap)
      }
    )
    .GET(
      "/v1/media",
      (request) => {
        val params = request.getQueryParams()
        val result = if (params.containsKey("id")) {
          val paramId = params.get("id").get(0)
          val id = Id(paramId)
          captcha.getCaptcha(id)
        } else {
          Left(Error(ErrorMessageEnum.INVALID_PARAM.toString + "=> id"))
        }
        getResponse(result, headerMap)
      }
    )
    .POST(
      "/v1/answer",
      (request) => {
        val json = parse(request.getBodyString())
        val answer = json.extract[Answer]
        val result = captcha.checkAnswer(answer)
        getResponse(result, headerMap)
      }
    )
  if( playgroundEnabled ) {
    serverBuilder.GET(
      "/demo/index.html",
      (_) => {
        val resStream = getClass().getResourceAsStream("/index.html")
        val str = Source.fromInputStream(resStream).mkString
        new StringResponse(200, str)
      }
    )
  }

  val server: picoserve.Server = serverBuilder.build()

  private def getResponse(response: Either[Error, ByteConvert], responseHeaders: util.Map[String, util.List[String]]): ByteResponse = {
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
}
