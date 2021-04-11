package lc.server

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import lc.core.Captcha
import lc.core.ErrorMessageEnum
import lc.core.{Parameters, Id, Answer, Response}
import org.json4s.JsonAST.JValue
import com.sun.net.httpserver.{HttpServer, HttpExchange}
import java.net.InetSocketAddress

class Server(port: Int) {

  implicit val formats: DefaultFormats.type = DefaultFormats
  val server: HttpServer = HttpServer.create(new InetSocketAddress(port), 32)

  private def getRequestJson(ex: HttpExchange): JValue = {
    val requestBody = ex.getRequestBody
    val bytes = requestBody.readAllBytes
    val string = bytes.map(_.toChar).mkString
    parse(string)
  }

  private def getPathParameter(ex: HttpExchange): String = {
    try {
      val uri = ex.getRequestURI.toString
      val param = uri.split("\\?")(1)
      param.split("=")(1)
    } catch {
      case exception: ArrayIndexOutOfBoundsException => {
        println(exception.getStackTrace)
        throw new Exception(ErrorMessageEnum.INVALID_PARAM.toString)
      }
    }
  }

  private def sendResponse(statusCode: Int, response: Array[Byte], ex: HttpExchange): Unit = {
    ex.sendResponseHeaders(statusCode, response.length)
    val os = ex.getResponseBody
    os.write(response)
    os.close
  }

  private def getException(exception: Exception): Response = {
    println(exception.printStackTrace)
    val message = ("message" -> exception.getMessage)
    val messageByte = write(message).getBytes
    Response(500, messageByte)
  }

  private def getBadRequestError(): Response = {
    val message = ("message" -> ErrorMessageEnum.BAD_METHOD.toString)
    Response(405, write(message).getBytes)
  }

  private def makeApiWorker(path: String, f: (String, HttpExchange) => Response): Unit = {
    server.createContext(
      path,
      ex => {
        val requestMethod = ex.getRequestMethod
        val response =
          try {
            f(requestMethod, ex)
          } catch {
            case exception: Exception => {
              getException(exception)
            }
          }
        sendResponse(statusCode = response.statusCode, response = response.message, ex = ex)
      }
    )
  }

  def start(): Unit = {
    println("Starting server on port:" + port)
    server.start()
  }

  makeApiWorker(
    "/v1/captcha",
    (method: String, ex: HttpExchange) => {
      if (method == "POST") {
        val json = getRequestJson(ex)
        val param = json.extract[Parameters]
        val id = Captcha.getChallenge(param)
        Response(200, write(id).getBytes)
      } else {
        getBadRequestError()
      }
    }
  )

  makeApiWorker(
    "/v1/media",
    (method: String, ex: HttpExchange) => {
      if (method == "GET") {
        val param = getPathParameter(ex)
        val id = Id(param)
        val image = Captcha.getCaptcha(id)
        Response(200, image)
      } else {
        getBadRequestError()
      }
    }
  )

  makeApiWorker(
    "/v1/answer",
    (method: String, ex: HttpExchange) => {
      if (method == "POST") {
        val json = getRequestJson(ex)
        val answer = json.extract[Answer]
        val result = Captcha.checkAnswer(answer)
        Response(200, write(result).getBytes)
      } else {
        getBadRequestError()
      }
    }
  )

}
