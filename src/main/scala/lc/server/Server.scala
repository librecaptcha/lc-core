package lc.server

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import lc.core.Captcha
import lc.core.ErrorMessageEnum
import lc.core.{Parameters, Id, Answer, Response, Error, ChallengeResult, Image}
import org.json4s.JsonAST.JValue
import com.sun.net.httpserver.{HttpServer, HttpExchange}
import java.net.InetSocketAddress

class Server(port: Int) {

  implicit val formats: DefaultFormats.type = DefaultFormats
  val server: HttpServer = HttpServer.create(new InetSocketAddress(port), 32)
  server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool())

  private def getRequestJson(ex: HttpExchange): JValue = {
    val requestBody = ex.getRequestBody
    val bytes = requestBody.readAllBytes
    val string = new String(bytes)
    parse(string)
  }

  private val eqPattern = java.util.regex.Pattern.compile("=")
  private def getPathParameter(ex: HttpExchange): Either[String, Error] = {
    try {
      val query = ex.getRequestURI.getQuery
      val param = eqPattern.split(query)
      if(param(0) == "id"){
        Left(param(1))
      } else {
        Right(Error(ErrorMessageEnum.INVALID_PARAM.toString + "=> id"))
      }
    } catch {
      case exception: ArrayIndexOutOfBoundsException => {
        println(exception.getStackTrace)
        Right(Error(ErrorMessageEnum.INVALID_PARAM.toString + "=> id"))
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

  private def getResponse(response: ChallengeResult): Response = {
    response match {
      case Image(image) => Response(200, image)
      case Error(_)     => Response(500, write(response).getBytes)
      case _            => Response(200, write(response).getBytes)
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
          case Left(value) => {
            val id = Id(value)
            Captcha.getCaptcha(id)
          }
          case Right(value) => value
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
