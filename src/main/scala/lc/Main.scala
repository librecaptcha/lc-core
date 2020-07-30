package lc

import com.sksamuel.scrimage._
import java.io.ByteArrayInputStream
import java.util.concurrent._
import java.util.UUID
import java.sql.{Blob, ResultSet}
import java.util.concurrent.atomic.AtomicInteger

case class Size(height: Int, width: Int)
case class Parameters(level: String, media: String, input_type: String, size: Option[Size])
case class Id(id: String)
case class Answer(answer: String, id: String)

case class ProviderSecret(provider: String, secret: String)

object CaptchaProviders {
  val providers = Map(
    "FilterChallenge" -> new FilterChallenge,
    "FontFunCaptcha" -> new FontFunCaptcha,
    "GifCaptcha" -> new GifCaptcha,
    "ShadowTextCaptcha" -> new ShadowTextCaptcha,
    "RainDropsCaptcha" -> new RainDropsCP,
    "LabelCaptcha" -> new LabelCaptcha
    )

  def generateChallengeSamples() = {
    providers.map {case (key, provider) =>
      (key, provider.returnChallenge())
    }
  }
}

class Statements(dbConn: DBConn) {
  val insertPstmt = dbConn.con.prepareStatement("INSERT INTO challenge(token, id, secret, provider, contentType, image) VALUES (?, ?, ?, ?, ?, ?)")
  val mapPstmt = dbConn.con.prepareStatement("INSERT INTO mapId(uuid, token) VALUES (?, ?)")
  val selectPstmt = dbConn.con.prepareStatement("SELECT secret, provider FROM challenge WHERE token = (SELECT m.token FROM mapId m, challenge c WHERE m.token=c.token AND m.uuid = ?)")
  val imagePstmt = dbConn.con.prepareStatement("SELECT image FROM challenge c, mapId m WHERE c.token=m.token AND m.uuid = ?")
  val updatePstmt = dbConn.con.prepareStatement("UPDATE challenge SET solved = True WHERE token = (SELECT m.token FROM mapId m, challenge c WHERE m.token=c.token AND m.uuid = ?)")
  val userPstmt = dbConn.con.prepareStatement("INSERT INTO users(email, hash) VALUES (?,?)")

}

object Statements {
  var dbConn: DBConn = _
  var tlStmts = ThreadLocal.withInitial(() => new Statements(dbConn))
}

class Captcha(throttle: Int, dbConn: DBConn) {
  import CaptchaProviders._

  private val stmt = dbConn.getStatement()
  stmt.execute("CREATE TABLE IF NOT EXISTS challenge(token varchar, id varchar, secret varchar, provider varchar, contentType varchar, image blob, solved boolean default False, PRIMARY KEY(token))")
  stmt.execute("CREATE TABLE IF NOT EXISTS mapId(uuid varchar, token varchar, PRIMARY KEY(uuid), FOREIGN KEY(token) REFERENCES challenge(token))")
  stmt.execute("CREATE TABLE IF NOT EXISTS users(email varchar, hash int)")

  
  def getProvider(): String = {
    val random = new scala.util.Random
    val keys = providers.keys
    val providerIndex = keys.toVector(random.nextInt(keys.size))
    providerIndex
  }

  def getCaptcha(id: Id): Array[Byte] = {
    var image :Array[Byte] = null
    var blob: Blob = null
    // val imageOpt = imagePstmt.synchronized {
      val imagePstmt = Statements.tlStmts.get.imagePstmt
    	imagePstmt.setString(1, id.id)
    	val rs: ResultSet = imagePstmt.executeQuery()
    	if(rs.next()){
        blob = rs.getBlob("image")
        // updatePstmt.synchronized{
          val updatePstmt = Statements.tlStmts.get.updatePstmt
          updatePstmt.setString(1,id.id)
          updatePstmt.executeUpdate()
        //}
      //}
    	if(blob != null)
    		image =  blob.getBytes(1, blob.length().toInt)
    	image
    }
    image
  }

  private val uniqueIntCount = new AtomicInteger()

  def generateChallenge(param: Parameters): String = {
  	//TODO: eval params to choose a provider
  	val providerMap = getProvider()
  	val provider = providers(providerMap)
    val challenge = provider.returnChallenge()
    val blob = new ByteArrayInputStream(challenge.content)
    // val token = scala.util.Random.nextInt(100000).toString
    val token = uniqueIntCount.incrementAndGet().toString
    // insertPstmt.synchronized {
      val insertPstmt = Statements.tlStmts.get.insertPstmt
      insertPstmt.setString(1, token)
      insertPstmt.setString(2, provider.getId)
      insertPstmt.setString(3, challenge.secret)
      insertPstmt.setString(4, providerMap)
      insertPstmt.setString(5, challenge.contentType)
      insertPstmt.setBlob(6, blob)
      insertPstmt.executeUpdate()
    //}
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
    //val idOpt = stmt.synchronized {
      val rs = stmt.executeQuery("SELECT token FROM challenge WHERE solved=FALSE ORDER BY RAND() LIMIT 1")
      val idOpt = if(rs.next()) {
        Some(rs.getString("token"))
      } else {
        None
      }
    //}
    val id = idOpt.getOrElse(generateChallenge(param))
    val uuid = getUUID(id)
    Id(uuid)
  }

  def getUUID(id: String): String = {
    val uuid = UUID.randomUUID().toString
    //mapPstmt.synchronized {
      val mapPstmt = Statements.tlStmts.get.mapPstmt
      mapPstmt.setString(1,uuid)
      mapPstmt.setString(2,id)
      mapPstmt.executeUpdate()
    //}
    uuid
  }

  def checkAnswer(answer: Answer): Boolean = {
    //val psOpt:Option[ProviderSecret] = selectPstmt.synchronized {
      val selectPstmt = Statements.tlStmts.get.selectPstmt
      selectPstmt.setString(1, answer.id)
      val rs: ResultSet = selectPstmt.executeQuery()
      val psOpt = if (rs.first()) {
        val secret = rs.getString("secret")
        val provider = rs.getString("provider")
        Some(ProviderSecret(provider, secret))
      } else {
        None
      }
    //}
    psOpt.map(ps => providers(ps.provider).checkAnswer(ps.secret, answer.answer)).getOrElse(false)
  }

  def getHash(email: String): Int = {
    val secret = ""
    val str = email+secret
    val hash = str.hashCode()
    val userPstmt = Statements.tlStmts.get.userPstmt
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
