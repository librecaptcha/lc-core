package lc

import java.io.File
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{read, write}

import lc.HTTPServer._

case class Secret(token: Int)

class RateLimiter extends DBConn {
  val stmt = getConn()
  val userLastActive = collection.mutable.Map[Int, Long]()
  val userAllowance = collection.mutable.Map[Int, Double]()
  val rate = 2.0
  val per = 45.0
  val allowance = rate

  def validateUser(user: Int) : Boolean = {
    synchronized {
      val allow = if(userLastActive.contains(user)){
        true
      } else {
        validatePstmt.setInt(1, user)
        val rs = validatePstmt.executeQuery()
        val validated = if(rs.next()){
          val hash = rs.getInt("hash")
          userLastActive(hash) = System.currentTimeMillis()
          userAllowance(hash) = allowance
          true
        } else {
          false
        }
        validated
      }
      allow
    }
  }

  def checkLimit(user: Int): Boolean = {
    synchronized {
      val current = System.currentTimeMillis()
      val time_passed = (current - userLastActive(user)) / 1000000000
      userLastActive(user) = current
      userAllowance(user) += time_passed * (rate/per)
      if(userAllowance(user) > rate){ userAllowance(user) = rate }
      val allow = if(userAllowance(user) < 1.0){
        false
      } else {
        userAllowance(user) -= 1.0
        true
      }
      allow
    }
  }

}

class Server(port: Int){
	val captcha = new Captcha(0)
  val rateLimiter = new RateLimiter()
	val server = new HTTPServer(port)
	val host = server.getVirtualHost(null)

	implicit val formats = DefaultFormats

	host.addContext("/v1/captcha",(req, resp) => {
      val accessToken = Option(req.getHeaders().get("access-token")).map(_.toInt)
      val access = accessToken.map(t => rateLimiter.validateUser(t) && rateLimiter.checkLimit(t)).getOrElse(false)
      val id = if(access){
        val body = req.getJson()
      	val json = parse(body)
      	val param = json.extract[Parameters]
        captcha.getChallenge(param)
      } else {
        "Not a valid user or rate limit reached!"
      }
    	resp.getHeaders().add("Content-Type","application/json")
    	resp.send(200, write(id))
    	0
    },"POST")

    host.addContext("/v1/media",(req, resp) => {
    	var id = Id(null)
    	if ("GET" == req.getMethod()){
    		val params = req.getParams()
    		id = Id(params.get("id"))
    	} else {
    		val body = req.getJson()
    		val json = parse(body)
    		id = json.extract[Id]
    	}
    	val image = captcha.getCaptcha(id)
    	resp.getHeaders().add("Content-Type","image/png")
    	resp.send(200, image)
    	0
    },"POST", "GET")

    host.addContext("/v1/answer",(req, resp) => {
    	val body = req.getJson()
    	val json = parse(body)
    	val answer = json.extract[Answer]
    	val result = captcha.checkAnswer(answer)
    	resp.getHeaders().add("Content-Type","application/json")
    	val responseContent = if(result) """{"result":"True"}""" else """{"result":"False"}"""
    	resp.send(200,responseContent)
    	0
    },"POST")

    host.addContext("/v1/register", new FileContextHandler(new File("client/")))

    host.addContext("/v1/token", (req,resp) => {
      val params = req.getParams()
      val hash = captcha.getHash(params.get("email"))
      val token = Secret(hash)
      resp.getHeaders().add("Content-Type", "application/json")
      resp.send(200, write(token))
      0
    })

    def start(): Unit = {
    	server.start()
    }

}
