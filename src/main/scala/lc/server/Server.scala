package lc.server

import org.json4s.jackson.JsonMethods.parse
import lc.core.Captcha
import lc.core.ErrorMessageEnum
import lc.core.{Parameters, Id, Answer, Response, Error, ByteConvert}
import org.json4s.JsonAST.JValue
import com.sun.net.httpserver.{HttpServer, HttpExchange}
import java.net.InetSocketAddress
import lc.core.Config.formats

class Server(port: Int) {

  val server: HttpServer = HttpServer.create(new InetSocketAddress(port), 32)
  server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool())

  private def getRequestJson(ex: HttpExchange): JValue = {
    val requestBody = ex.getRequestBody
    val bytes = requestBody.readAllBytes
    val string = new String(bytes)
    parse(string)
  }

  private val eqPattern = java.util.regex.Pattern.compile("=")
  private def getPathParameter(ex: HttpExchange): Either[String, String] = {
    try {
      val query = ex.getRequestURI.getQuery
      val param = eqPattern.split(query)
      if (param(0) == "id") {
        Right(param(1))
      } else {
        Left(ErrorMessageEnum.INVALID_PARAM.toString + "=> id")
      }
    } catch {
      case exception: ArrayIndexOutOfBoundsException => {
        println(exception)
        Left(ErrorMessageEnum.INVALID_PARAM.toString + "=> id")
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
    println(exception)
    val message = Error(exception.getMessage)
    Response(500, message.toBytes())
  }

  private def getBadRequestError(): Response = {
    val message = Error(ErrorMessageEnum.BAD_METHOD.toString)
    Response(405, message.toBytes())
  }

  private def getResponse(response: Either[Error, ByteConvert]): Response = {
    response match {
      case Right(value) => {
        Response(200, value.toBytes())
      }
      case Left(value) => {
        Response(500, value.toBytes())
      }
    }
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
        getResponse(id)
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
        val result = param match {
          case Right(value) => {
            val id = Id(value)
            Captcha.getCaptcha(id)
          }
          case Left(value) => Left(Error(value))
        }
        getResponse(result)
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
        getResponse(result)
      } else {
        getBadRequestError()
      }
    }
  )

}
