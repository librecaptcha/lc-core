package lc

import com.sksamuel.scrimage._
import java.io.ByteArrayInputStream
import java.util.concurrent._
import java.util.UUID
import java.sql.{Blob, ResultSet}
import java.util.concurrent.atomic.AtomicInteger
import java.io._
import java.sql.Statement

case class Size(height: Int, width: Int)
case class Parameters(level: String, media: String, input_type: String, size: Option[Size])
case class Id(id: String)
case class Answer(answer: String, id: String)

case class ProviderSecret(provider: String, secret: String)

object CaptchaProviders {
  val providers = Map(
    "FilterChallenge" -> new FilterChallenge,
    // "FontFunCaptcha" -> new FontFunCaptcha,
    "GifCaptcha" -> new GifCaptcha,
    "ShadowTextCaptcha" -> new ShadowTextCaptcha,
    "RainDropsCaptcha" -> new RainDropsCP,
    // "LabelCaptcha" -> new LabelCaptcha
    )

  def generateChallengeSamples() = {
    providers.map {case (key, provider) =>
      (key, provider.returnChallenge())
    }
  }
}

class Statements(dbConn: DBConn) {
  val insertPstmt = dbConn.con.prepareStatement("INSERT INTO challenge(id, secret, provider, contentType, image) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS )
  val mapPstmt = dbConn.con.prepareStatement("INSERT INTO mapId(uuid, token, lastServed) VALUES (?, ?, CURRENT_TIMESTAMP)")
  val selectPstmt = dbConn.con.prepareStatement("SELECT c.secret, c.provider FROM challenge c, mapId m WHERE m.token=c.token AND DATEDIFF(MINUTE, CURRENT_TIMESTAMP, DATEADD(MINUTE, 1, m.lastServed)) > 0 AND m.uuid = ?")
  val imagePstmt = dbConn.con.prepareStatement("SELECT image FROM challenge c, mapId m WHERE c.token=m.token AND m.uuid = ?")
  val updateAttemptedPstmt = dbConn.con.prepareStatement("UPDATE challenge SET attempted = attempted+1 WHERE token = (SELECT m.token FROM mapId m, challenge c WHERE m.token=c.token AND m.uuid = ?)")
  val tokenPstmt = dbConn.con.prepareStatement("SELECT token FROM challenge WHERE attempted < 10 ORDER BY RAND() LIMIT 1")
  val deleteAnswerPstmt = dbConn.con.prepareStatement("DELETE FROM mapId WHERE uuid = ?")
  val challengeGCPstmt = dbConn.con.prepareStatement("DELETE FROM challenge WHERE attempted >= 10 AND token NOT IN (SELECT token FROM mapId)")
  val mapIdGCPstmt = dbConn.con.prepareStatement("DELETE FROM mapId WHERE DATEDIFF(MINUTE, CURRENT_TIMESTAMP, DATEADD(MINUTE, 1, lastServed)) < 0")
}

object Statements {
  var dbConn: DBConn = _
  val tlStmts = ThreadLocal.withInitial(() => new Statements(dbConn))
}

class Captcha(throttle: Int, dbConn: DBConn) {
  import CaptchaProviders._

  private val stmt = dbConn.getStatement()
  stmt.execute("CREATE TABLE IF NOT EXISTS challenge(token int auto_increment, id varchar, secret varchar, provider varchar, contentType varchar, image blob, attempted int default 0, PRIMARY KEY(token))")
  stmt.execute("CREATE TABLE IF NOT EXISTS mapId(uuid varchar, token int, lastServed timestamp, PRIMARY KEY(uuid), FOREIGN KEY(token) REFERENCES challenge(token) ON DELETE CASCADE)")

  private val seed = System.currentTimeMillis.toString.substring(2,6).toInt
  private val random = new scala.util.Random(seed)

  def getNextRandomInt(max: Int) = random.synchronized {
    random.nextInt(max)
  }
  
  def getProvider(): String = {
    val keys = providers.keys
    val providerIndex = keys.toVector(getNextRandomInt(keys.size))
    providerIndex
  }

  def getCaptcha(id: Id): Array[Byte] = {
    var image :Array[Byte] = null
    var blob: Blob = null
    try {
      val imagePstmt = Statements.tlStmts.get.imagePstmt
    	imagePstmt.setString(1, id.id)
    	val rs: ResultSet = imagePstmt.executeQuery()
    	if(rs.next()){
        blob = rs.getBlob("image")
        if(blob != null){
    		  image =  blob.getBytes(1, blob.length().toInt)
        }
      }
    image
    } catch { case e: Exception =>
      println(e)
      image
    }
  }

  private val uniqueIntCount = new AtomicInteger()

  def generateChallenge(param: Parameters): Int = {
  	//TODO: eval params to choose a provider
  	val providerMap = getProvider()
  	val provider = providers(providerMap)
    val challenge = provider.returnChallenge()
    val blob = new ByteArrayInputStream(challenge.content)
    val insertPstmt = Statements.tlStmts.get.insertPstmt
    insertPstmt.setString(1, provider.getId)
    insertPstmt.setString(2, challenge.secret)
    insertPstmt.setString(3, providerMap)
    insertPstmt.setString(4, challenge.contentType)
    insertPstmt.setBlob(5, blob)
    insertPstmt.executeUpdate()
    val rs: ResultSet = insertPstmt.getGeneratedKeys()
    val token = if(rs.next()){
      rs.getInt("token")
    }
    println("Added new challenge: "+ token.toString)
    token.asInstanceOf[Int]
  }

  val task = new Runnable {
  	def run(): Unit = {
      try {

        val mapIdGCPstmt = Statements.tlStmts.get.mapIdGCPstmt
        mapIdGCPstmt.executeUpdate()

        val challengeGCPstmt = Statements.tlStmts.get.challengeGCPstmt
        challengeGCPstmt.executeUpdate()

        val imageNum = stmt.executeQuery("SELECT COUNT(*) AS total FROM challenge")
        var throttleIn = (throttle*1.1).toInt
        if(imageNum.next())
          throttleIn = (throttleIn-imageNum.getInt("total"))
        while(0 < throttleIn){
          generateChallenge(Parameters("","","",Option(Size(0,0))))
          throttleIn -= 1
        }
      } catch { case e: Exception => println(e) }
  	}
  }

  def beginThread(delay: Int) : Unit = {
    val ex = new ScheduledThreadPoolExecutor(1)
    val thread = ex.scheduleWithFixedDelay(task, 1, delay, TimeUnit.SECONDS)
  }

  def getChallenge(param: Parameters): Id = {
    try {
      val tokenPstmt = Statements.tlStmts.get.tokenPstmt
      val rs = tokenPstmt.executeQuery()
      val tokenOpt = if(rs.next()) {
        Some(rs.getInt("token"))
      } else {
        None
      }
      val updateAttemptedPstmt = Statements.tlStmts.get.updateAttemptedPstmt
      val uuid = getUUID(tokenOpt.getOrElse(generateChallenge(param)))
      updateAttemptedPstmt.setString(1, uuid)
      updateAttemptedPstmt.executeUpdate()
      Id(uuid)
    } catch {case e: Exception => 
      println(e)
      Id(getUUID(-1))
    }
  }

  def getUUID(id: Int): String = {
    val uuid = UUID.randomUUID().toString
      val mapPstmt = Statements.tlStmts.get.mapPstmt
      mapPstmt.setString(1,uuid)
      mapPstmt.setInt(2,id)
      mapPstmt.executeUpdate()
    uuid
  }

  def checkAnswer(answer: Answer): String = {
      val selectPstmt = Statements.tlStmts.get.selectPstmt
      selectPstmt.setString(1, answer.id)
      val rs: ResultSet = selectPstmt.executeQuery()
      val psOpt = if (rs.first()) {
        val secret = rs.getString("secret")
        val provider = rs.getString("provider")
        val check = providers(provider).checkAnswer(secret, answer.answer)
        val result = if(check) "TRUE" else "FALSE"
        result
      } else {
        "EXPIRED"
      }
      val deleteAnswerPstmt = Statements.tlStmts.get.deleteAnswerPstmt
      deleteAnswerPstmt.setString(1, answer.id)
      deleteAnswerPstmt.executeUpdate()
      psOpt
  }

  def display(): Unit = {
    val rs: ResultSet = stmt.executeQuery("SELECT * FROM challenge")
    println("token\t\tid\t\tsecret\t\tattempted")
    while(rs.next()) {
      val token = rs.getInt("token")
      val id = rs.getString("id")
      val secret = rs.getString("secret")
      val attempted = rs.getString("attempted")
      println(s"${token}\t\t${id}\t\t${secret}\t\t${attempted}\n\n")
    }

    val rss: ResultSet = stmt.executeQuery("SELECT * FROM mapId")
    println("uuid\t\ttoken\t\tlastServed")
    while(rss.next()){
      val uuid = rss.getString("uuid")
      val token = rss.getInt("token")
      val lastServed = rss.getTimestamp("lastServed")
      println(s"${uuid}\t\t${token}\t\t${lastServed}\n\n")
    }
  }
}

object LCFramework{
  def main(args: scala.Array[String]) {
    val dbConn = new DBConn()
    Statements.dbConn = dbConn
  	val captcha = new Captcha(2, dbConn)
    val server = new Server(8888, captcha, dbConn)
    captcha.beginThread(2)
    server.start()
  }
}

object MakeSamples {
  def main(args: scala.Array[String]) {
    val samples = CaptchaProviders.generateChallengeSamples()
    samples.foreach {case (key, sample) =>
      val extensionMap = Map("image/png" -> "png", "image/gif" -> "gif")
      println(key + ": " + sample)

      val outStream = new java.io.FileOutputStream("samples/"+key+"."+extensionMap(sample.contentType))
      outStream.write(sample.content)
      outStream.close
    }
  }
}
