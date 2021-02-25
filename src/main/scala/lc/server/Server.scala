package lc.server

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import lc.core.Captcha
import lc.core.{Parameters, Id, Answer}
import lc.server.HTTPServer

class Server(port: Int, captcha: Captcha) {
  val server = new HTTPServer(port)
  val host: HTTPServer.VirtualHost = server.getVirtualHost(null)

  implicit val formats: DefaultFormats.type = DefaultFormats

  host.addContext(
    "/v1/captcha",
    (req, resp) => {
      val body = req.getJson()
      val json = parse(body)
      val param = json.extract[Parameters]
      val id = captcha.getChallenge(param)
      resp.getHeaders().add("Content-Type", "application/json")
      resp.send(200, write(id))
      0
    },
    "POST"
  )

  host.addContext(
    "/v1/media",
    (req, resp) => {
      val params = req.getParams()
      val id = Id(params.get("id"))
      val image = captcha.getCaptcha(id)
      resp.getHeaders().add("Content-Type", "image/png")
      resp.send(200, image)
      0
    },
    "GET"
  )

  host.addContext(
    "/v1/answer",
    (req, resp) => {
      val body = req.getJson()
      val json = parse(body)
      val answer = json.extract[Answer]
      val result = captcha.checkAnswer(answer)
      resp.getHeaders().add("Content-Type", "application/json")
      resp.send(200, write(result))
      0
    },
    "POST"
  )

  def start(): Unit = {
    println("Starting server on port:" + port)
    server.start()
  }

}
