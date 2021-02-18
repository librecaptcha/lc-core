package lc

import java.io.File
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{read, write}

import lc.HTTPServer._

case class Secret(token: Int)

class Server(port: Int, captcha: Captcha, dbConn: DBConn){
	val server = new HTTPServer(port)
	val host = server.getVirtualHost(null)

	implicit val formats = DefaultFormats

	host.addContext("/v1/captcha",(req, resp) => {
      val body = req.getJson()
    	val json = parse(body)
    	val param = json.extract[Parameters]
      val id = captcha.getChallenge(param)
      resp.getHeaders().add("Content-Type","application/json")
      resp.send(200, write(id))
    	0
    },"POST")

    host.addContext("/v1/media",(req, resp) => {
    	val params = req.getParams()
    	val id = Id(params.get("id"))
    	val image = captcha.getCaptcha(id)
    	resp.getHeaders().add("Content-Type","image/png")
    	resp.send(200, image)
    	0
    },"GET")

    host.addContext("/v1/answer",(req, resp) => {
    	val body = req.getJson()
    	val json = parse(body)
    	val answer = json.extract[Answer]
    	val result = captcha.checkAnswer(answer)
    	resp.getHeaders().add("Content-Type","application/json")
    	val responseContent = s"""{"result":"$result"}"""
    	resp.send(200,responseContent)
    	0
    },"POST")


    def start(): Unit = {
      println("Starting server on port:" + port)
    	server.start()
    }

}
