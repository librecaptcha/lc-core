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
  val selectPstmt = dbConn.con.prepareStatement("SELECT secret, provider FROM challenge WHERE token = (SELECT m.token FROM mapId m, challenge c WHERE m.token=c.token AND m.uuid = ? AND DATEDIFF(MINUTE, DATEADD(MINUTE,2,m.lastServed), CURRENT_TIMESTAMP) <= 0)")
  val imagePstmt = dbConn.con.prepareStatement("SELECT image FROM challenge c, mapId m WHERE c.token=m.token AND m.uuid = ?")
  val updateSolvedPstmt = dbConn.con.prepareStatement("UPDATE challenge SET solved = solved+1 WHERE token = (SELECT m.token FROM mapId m, challenge c WHERE m.token=c.token AND m.uuid = ?)")
  val tokenPstmt = dbConn.con.prepareStatement("SELECT token FROM challenge WHERE solved < 10 ORDER BY RAND() LIMIT 1")
}

object Statements {
  var dbConn: DBConn = _
  val tlStmts = ThreadLocal.withInitial(() => new Statements(dbConn))
}

class Captcha(throttle: Int, dbConn: DBConn) {
  import CaptchaProviders._

  private val stmt = dbConn.getStatement()
  stmt.execute("CREATE TABLE IF NOT EXISTS challenge(token int auto_increment, id varchar, secret varchar, provider varchar, contentType varchar, image blob, solved int default 0, PRIMARY KEY(token))")
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
    	if(blob != null)
    		image =  blob.getBytes(1, blob.length().toInt)
    	image
    }
    image
  } catch{ case e: Exception =>
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
      val imageNum = stmt.executeQuery("SELECT COUNT(*) AS total FROM challenge")
      var throttleIn = (throttle*1.1).toInt
      if(imageNum.next())
        throttleIn = (throttleIn-imageNum.getInt("total"))
      while(0 < throttleIn){
        generateChallenge(Parameters("","","",Option(Size(0,0))))
        throttleIn -= 1
      }
      
      val gcStmt = stmt.executeUpdate("DELETE FROM challenge WHERE solved > 10 AND token = (SELECT m.token FROM mapId m, challenge c WHERE c.token = m.token AND m.lastServed = (SELECT MAX(m.lastServed) FROM mapId m, challenge c WHERE c.token=m.token AND DATEDIFF(MINUTE, DATEADD(MINUTE,5,m.lastServed), CURRENT_TIMESTAMP) <= 0))")

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
      Id(getUUID(tokenOpt.getOrElse(generateChallenge(param))))
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
        val result = if(check) {
          val updateSolvedPstmt = Statements.tlStmts.get.updateSolvedPstmt
          updateSolvedPstmt.setString(1,answer.id)
          updateSolvedPstmt.executeUpdate()
          "TRUE"
        } else {
          "FALSE"
        }
        result
      } else {
        "EXPIRED"
      }
      psOpt
  }

  def display(): Unit = {
    val rs: ResultSet = stmt.executeQuery("SELECT * FROM challenge")
    println("token\t\tid\t\tsecret\t\tsolved")
    while(rs.next()) {
      val token = rs.getInt("token")
      val id = rs.getString("id")
      val secret = rs.getString("secret")
      val solved = rs.getString("solved")
      println(s"${token}\t\t${id}\t\t${secret}\t\t${solved}\n\n")
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
