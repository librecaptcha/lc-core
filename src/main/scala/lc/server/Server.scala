package lc.server

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import lc.core.Captcha
import lc.core.ErrorMessageEnum
import lc.core.{Parameters, Id, Answer, Response}
import eu.lucaventuri.fibry.Stereotypes
import org.json4s.JsonAST.JValue
import com.sun.net.httpserver.HttpExchange
import java.util.Base64


class Server(port: Int) {

  implicit val formats: DefaultFormats.type = DefaultFormats

  val captcha = new Captcha()

  private def getRequestJson(ex: HttpExchange): JValue = {
    val requestBody = ex.getRequestBody
    val bytes = requestBody.readAllBytes
    val string = bytes.map(_.toChar).mkString
    parse(string)
  }

  private def getPathParameter(ex: HttpExchange): String = {
    try{
      val uri = ex.getRequestURI.toString
      val param = uri.split("\\?")(1)
      param.split("=")(1)
    } catch {
      case exception: ArrayIndexOutOfBoundsException => {
        println(exception.getStackTrace)
        throw new Exception(ErrorMessageEnum.INVALID_PARAM.toString)
      }
      case exception: Exception => {
        println(exception.getStackTrace)
        throw new Exception(exception.getMessage)
      }
    }
  }

  private def sendResponse(statusCode: Int, message: String, ex: HttpExchange, media: Boolean): Unit = {
    val response = if(media && statusCode == 200){
      Base64.getDecoder().decode(message)
    } else {
      message.getBytes
    }
    ex.sendResponseHeaders(statusCode, response.length)
    val os = ex.getResponseBody
    os.write(response)
    os.close
  }

  private def getException(exception: Exception): Response = {
    println(exception.printStackTrace)
    val message = ("message" -> exception.getMessage)
    Response(500, write(message))
  }

  private def getBadRequestError(): Response = {
    val message = ("message" -> ErrorMessageEnum.BAD_METHOD.toString)
    Response(405, write(message))
  } 
    
  Stereotypes.auto().embeddedHttpServer(port,
 
    new Stereotypes.HttpWorker("/v1/captcha", ex => {
      val requestMethod = ex.getRequestMethod
      val response = if (requestMethod == "POST"){
        try{
          val json = getRequestJson(ex)
          val param = json.extract[Parameters]
          val id = captcha.getChallenge(param)
          Response(200, write(id))
        } catch {
          case exception: Exception =>{
            getException(exception)
          }
        }
      } else {
        getBadRequestError()
      }
      sendResponse(statusCode = response.statusCode, message = response.message, ex = ex, media = false)
    }),

    new Stereotypes.HttpWorker("/v1/media", ex => {
      val requestMethod = ex.getRequestMethod
      val response = if (requestMethod == "GET"){
        try{
          val param = getPathParameter(ex)
          val id = Id(param)
          val image = captcha.getCaptcha(id)
          val imageString = Base64.getEncoder().encodeToString(image)
          Response(200, imageString)
        } catch {
          case exception: Exception => {
            getException(exception)
          }
        }
      } else {
        getBadRequestError()
      }
      sendResponse(statusCode = response.statusCode, message = response.message, ex = ex, media = true)
    }),

    new Stereotypes.HttpWorker("/v1/answer", ex => {
      val requestMethod = ex.getRequestMethod
      val response = if (requestMethod == "POST"){
        try{
          val json = getRequestJson(ex)
          val answer = json.extract[Answer]
          val result = captcha.checkAnswer(answer)
          Response(200, write(result))
        } catch {
          case exception: Exception => {
            getException(exception)
          }
        }
      } else {
        getBadRequestError()
      }
      sendResponse(statusCode = response.statusCode, message = response.message, ex = ex, media = false)
    })
  )
}
