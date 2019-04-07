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

class Captcha(throttle: Int) {
  val con: Connection = DriverManager.getConnection("jdbc:h2:./captcha", "sa", "")
  val stmt: Statement = con.createStatement()
  stmt.execute("CREATE TABLE IF NOT EXISTS challenge(token varchar, id varchar, secret varchar, provider varchar, contentType varchar, image blob, solved boolean default False)")
  val insertPstmt: PreparedStatement = con.prepareStatement("INSERT INTO challenge(token, id, secret, provider, contentType, image) VALUES (?, ?, ?, ?, ?, ?)")
  val selectPstmt: PreparedStatement = con.prepareStatement("SELECT secret, provider FROM challenge WHERE token = ?")
  val imagePstmt: PreparedStatement = con.prepareStatement("SELECT image FROM challenge WHERE token = ?")
  val updatePstmt: PreparedStatement = con.prepareStatement("UPDATE challenge SET solved = True WHERE token = ?")

  val filters = Map("FilterChallenge" -> new FilterChallenge,
                    "FontFunCaptcha" -> new FontFunCaptcha,
                    "GifCaptcha" -> new GifCaptcha,
                    "ShadowTextCaptcha" -> new ShadowTextCaptcha,
                    "LabelCaptcha" -> new LabelCaptcha)

  def getProvider(): String = {
    val random = new scala.util.Random
    val keys = filters.keys
    val providerMap = keys.toVector(random.nextInt(keys.size))
    providerMap
  }

  def getCaptcha(id: Id): Array[Byte] = {
    var image :Array[Byte] = null
    var blob: Blob = null
  	imagePstmt.setString(1, id.id)
  	val rs: ResultSet = imagePstmt.executeQuery()
  	if(rs.next()){
      blob = rs.getBlob("image")
      updatePstmt.setString(1,id.id)
      updatePstmt.executeUpdate()
    }
  	if(blob != null)
  		image =  blob.getBytes(1, blob.length().toInt)
  	image
  }
  
  def generateChallenge(param: Parameters): String = {
  	//TODO: eval params to choose a provider
  	val providerMap = getProvider()
  	val provider = filters(providerMap)
    val challenge = provider.returnChallenge()
    val blob = new ByteArrayInputStream(challenge.content)
    val token = scala.util.Random.nextInt(10000).toString
    insertPstmt.setString(1, token)
    insertPstmt.setString(2, provider.getId)
    insertPstmt.setString(3, challenge.secret)
    insertPstmt.setString(4, providerMap)
    insertPstmt.setString(5, challenge.contentType)
    insertPstmt.setBlob(6, blob)
    insertPstmt.executeUpdate()
    token
  }

  val task = new Runnable {
  	def run(): Unit = {
      val imageNum = stmt.executeQuery("SELECT COUNT(*) AS total FROM challenge")
      var throttleIn = (throttle*1.1).toInt
      if(imageNum.next())
        throttleIn = (throttleIn-imageNum.getInt("total"))
      while(0 < throttleIn){
        getChallenge(Parameters("","","",Option(Size(0,0))))
        throttleIn -= 1
      }
  	}
  }

  def beginThread(delay: Int) : Unit = {
    val ex = new ScheduledThreadPoolExecutor(1)
    val thread = ex.scheduleWithFixedDelay(task, 1, delay, TimeUnit.SECONDS)
  }

  def getChallenge(param: Parameters): Id = {
    val rs = stmt.executeQuery("SELECT token FROM challenge WHERE solved=FALSE LIMIT 1")
    var id: String = null
    if(rs.next()){
      id = rs.getString("token")
    } else {
      id = generateChallenge(param)
    }
    Id(id)
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
    println("token\t\tid\t\tsecret\t\tsolved")
    while(rs.next()) {
      val token = rs.getString("token")
      val id = rs.getString("id")
      val secret = rs.getString("secret")
      val solved = rs.getString("solved")
      println(s"${token}\t\t${id}\t\t${secret}\t\t${solved}")
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
	val captcha = new Captcha(0)
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
  	val captcha = new Captcha(50)
    val server = new Server(8888)
    captcha.beginThread(2)
    server.start()
  } 
}

