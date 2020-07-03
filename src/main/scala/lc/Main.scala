package lc

import com.sksamuel.scrimage._
import java.io.ByteArrayInputStream
import java.util.concurrent._
import java.util.UUID
import java.sql.{Blob, ResultSet}

case class Size(height: Int, width: Int)
case class Parameters(level: String, media: String, input_type: String, size: Option[Size])
case class Id(id: String)
case class Answer(answer: String, id: String)

class Captcha(throttle: Int) extends DBConn {

  val stmt = getConn()
  stmt.execute("CREATE TABLE IF NOT EXISTS challenge(token varchar, id varchar, secret varchar, provider varchar, contentType varchar, image blob, solved boolean default False, PRIMARY KEY(token))")
  stmt.execute("CREATE TABLE IF NOT EXISTS mapId(uuid varchar, token varchar, PRIMARY KEY(uuid), FOREIGN KEY(token) REFERENCES challenge(token))")
  stmt.execute("CREATE TABLE IF NOT EXISTS users(email varchar, hash int)")

  val providers = Map("FilterChallenge" -> new FilterChallenge,
                    "FontFunCaptcha" -> new FontFunCaptcha,
                    "GifCaptcha" -> new GifCaptcha,
                    "ShadowTextCaptcha" -> new ShadowTextCaptcha,
                    "RainDropsCaptcha" -> new RainDropsCP,
                    "LabelCaptcha" -> new LabelCaptcha)

  def getProvider(): String = {
    val random = new scala.util.Random
    val keys = providers.keys
    val providerIndex = keys.toVector(random.nextInt(keys.size))
    providerIndex
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

  def generateChallengeSamples() = {
    providers.map {case (key, provider) =>
      (key, provider.returnChallenge())
    }
  }

  def generateChallenge(param: Parameters): String = {
  	//TODO: eval params to choose a provider
  	val providerMap = getProvider()
  	val provider = providers(providerMap)
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
    val id = if(rs.next()){
      rs.getString("token")
    } else {
      generateChallenge(param)
    }
    val uuid = getUUID(id)
    Id(uuid)
  }

  def getUUID(id: String): String = {
    val uuid = UUID.randomUUID().toString
    mapPstmt.setString(1,uuid)
    mapPstmt.setString(2,id)
    mapPstmt.executeUpdate()
    uuid
  }

  def checkAnswer(answer: Answer): Boolean = {
    selectPstmt.setString(1, answer.id)
    val rs: ResultSet = selectPstmt.executeQuery()
    rs.next()
    val secret = rs.getString("secret")
    val provider = rs.getString("provider")
    providers(provider).checkAnswer(secret, answer.answer)
  }

  def getHash(email: String): Int = {
    val secret = ""
    val str = email+secret
    val hash = str.hashCode()
    userPstmt.setString(1, email)
    userPstmt.setInt(2, hash)
    userPstmt.executeUpdate()
    hash
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
}

object LCFramework{
  def main(args: scala.Array[String]) {
  	val captcha = new Captcha(2)
    val server = new Server(8888)
    captcha.beginThread(2)
    server.start()
  }
}

object MakeSamples {
  def main(args: scala.Array[String]) {
    val captcha = new Captcha(2)
    val samples = captcha.generateChallengeSamples()
    samples.foreach {case (key, sample) =>
      val extensionMap = Map("image/png" -> "png", "image/gif" -> "gif")
      println(key + ": " + sample)

      val outStream = new java.io.FileOutputStream("samples/"+key+"."+extensionMap(sample.contentType))
      outStream.write(sample.content)
      outStream.close
    }
  }
}
