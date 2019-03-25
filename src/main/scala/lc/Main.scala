package lc

import com.sksamuel.scrimage._
import java.sql._
import java.io._
import lc.HTTPServer._
import lc.FontFunCaptcha._
import lc.GifCaptcha._
import lc.ShadowTextCaptcha._
import javax.imageio._
import java.awt.image._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import java.util.Base64
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}
import java.util.concurrent._
import scala.Array

class Captcha {
  val con: Connection = DriverManager.getConnection("jdbc:h2:./captcha", "sa", "")
  val stmt: Statement = con.createStatement()
  stmt.execute("CREATE TABLE IF NOT EXISTS challenge(token varchar, id varchar, secret varchar, provider varchar, image blob)")
  val insertPstmt: PreparedStatement = con.prepareStatement("INSERT INTO challenge(token, id, secret, provider, image) VALUES (?, ?, ?, ?, ?)")
  val selectPstmt: PreparedStatement = con.prepareStatement("SELECT secret, provider FROM challenge WHERE token = ?")
  val imagePstmt: PreparedStatement = con.prepareStatement("SELECT image FROM challenge WHERE token = ?")

  val filters = Map("FilterChallenge" -> new FilterChallenge,
                    "FontFunCaptcha" -> new FontFunCaptcha,
                    "GifCaptcha" -> new GifCaptcha,
                    "ShadowTextCaptcha" -> new ShadowTextCaptcha
                    )

  def getCaptcha(id: Id): Array[Byte] = {
  	imagePstmt.setString(1, id.id)
  	val rs: ResultSet = imagePstmt.executeQuery()
  	rs.next()
  	val blob = rs.getBlob("image")
  	var image :Array[Byte] = null
  	if(blob != null)
  		image =  blob.getBytes(1, blob.length().toInt)
  	image
  }
  
  def getChallenge(param: Parameters): Id = {
  	//TODO: eval params to choose a provider
  	val providerMap = "FontFunCaptcha"
  	val provider = filters(providerMap)
    val challenge = provider.returnChallenge()
    val blob = new ByteArrayInputStream(challenge.content)
    val token = scala.util.Random.nextInt(10000).toString
    val id = Id(token)
    print("Successfull")
    insertPstmt.setString(1, token)
    insertPstmt.setString(2, provider.getId)
    insertPstmt.setString(3, challenge.secret)
    insertPstmt.setString(4, providerMap)
    insertPstmt.setBlob(5, blob)
    insertPstmt.executeUpdate()
    id
  }

  val task = new Runnable {
  	val providerMap = "FilterChallenge"
  	val provider = filters(providerMap)
  	def run(): Unit = {
	    val challenge = provider.returnChallenge()
	    val blob = new ByteArrayInputStream(challenge.content)
	    val token = scala.util.Random.nextInt(10000).toString
	    val id = Id(token)
	    insertPstmt.setString(1, token)
	    insertPstmt.setString(2, provider.getId)
	    insertPstmt.setString(3, challenge.secret)
	    insertPstmt.setString(4, providerMap)
	    insertPstmt.setBlob(5, blob)
	    insertPstmt.executeUpdate()
  	}
  }

  def beginThread(delay: Int) : Unit = {
  	val ex = new ScheduledThreadPoolExecutor(1)
  	val thread = ex.scheduleAtFixedRate(task, 1, delay, TimeUnit.SECONDS)
  }

  def getAnswer(answer: Answer): Boolean = {
    selectPstmt.setString(1, answer.id)
    val rs: ResultSet = selectPstmt.executeQuery()
    rs.next()
    val secret = rs.getString("secret")
    val provider = rs.getString("provider")
    filters(provider).checkAnswer(secret, answer.answer)
  }

  def display(): Unit = {
    val rs: ResultSet = stmt.executeQuery("SELECT * FROM challenge")
    println("token\t\tid\t\tsecret\t\timage")
    while(rs.next()) {
      val token = rs.getString("token")
      val id = rs.getString("id")
      val secret = rs.getString("secret")
      val image = rs.getString("image")
      println(s"${token}\t\t${id}\t\t${secret}\t\t")
    }
  }
  
  def closeConnection(): Unit = {
	  con.close()
  } 
}

case class Size(height: Int, width: Int)
case class Parameters(level: String, media: String, input_type: String, size: Option[Size])
case class Id(id: String)
case class Answer(answer: String, id: String)

class Server(port: Int){
	val captcha = new Captcha()
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
    	val body = req.getJson()
    	val json = parse(body)
    	val id = json.extract[Id]
    	val image = captcha.getCaptcha(id)
    	resp.getHeaders().add("Content-Type","image/png")
    	resp.send(200, image)
    	0
    },"POST")

    host.addContext("/v1/answer",(req, resp) => {
    	val body = req.getJson()
    	val json = parse(body)
    	val answer = json.extract[Answer]
    	val result = captcha.getAnswer(answer)
    	resp.getHeaders().add("Content-Type","application/json")
    	val responseContent = if(result) """{"result":"True"}""" else """{"result":"False"}"""
    	resp.send(200,responseContent)
    	0
    },"POST")

    def start(): Unit = {
    	server.start()
    }

}

object LCFramework{
  def main(args: scala.Array[String]) {
  	val captcha = new Captcha()
    val server = new Server(8888)
    server.start()
    //captcha.beginThread(2)
    
  } 
}

