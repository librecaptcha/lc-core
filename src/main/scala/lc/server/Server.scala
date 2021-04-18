package lc.server

import org.json4s.jackson.JsonMethods.parse
import lc.core.Captcha
import lc.core.ErrorMessageEnum
import lc.core.{Parameters, Id, Answer, Error, ByteConvert}
import lc.core.Config.formats
import org.limium.picoserve
import org.limium.picoserve.Server.ByteResponse

class Server(port: Int) {
  val server: picoserve.Server = picoserve.Server.builder()
    .port(8888)
    .backlog(32)
    .POST("/v1/captcha", (request) => {
      val json = parse(request.getBodyString())
      val param = json.extract[Parameters]
      val id = Captcha.getChallenge(param)
      getResponse(id)
    })
    .GET("/v1/media", (request) => {
      val params = request.getQueryParams()
      val result = if (params.containsKey("id")) {
        val paramId = params.get("id").get(0)
        val id = Id(paramId)
        Captcha.getCaptcha(id)
      } else {
        Left(Error(ErrorMessageEnum.INVALID_PARAM.toString + "=> id"))
      }
      getResponse(result)
    })
    .POST("/v1/answer", (request) => {
      val json = parse(request.getBodyString())
      val answer = json.extract[Answer]
      val result = Captcha.checkAnswer(answer)
      getResponse(result)
    })
    .build()

  private def getResponse(response: Either[Error, ByteConvert]): ByteResponse = {
    response match {
      case Right(value) => {
        new ByteResponse(200, value.toBytes())
      }
      case Left(value) => {
        new ByteResponse(500, value.toBytes())
      }
    }
  }

  def start(): Unit = {
    println("Starting server on port:" + port)
    server.start()
  }
}
