package lc.database

import lc.database.DBConn
import java.sql.Statement
import java.sql.PreparedStatement

class Statements(dbConn: DBConn, maxAttempts: Int) {

  private val stmt = dbConn.getStatement()

  stmt.execute(
    "CREATE TABLE IF NOT EXISTS challenge" +
      "(token int auto_increment, " +
      "id varchar, " +
      "secret varchar, " +
      "provider varchar, " +
      "contentType varchar, " +
      "contentLevel varchar, " +
      "contentInput varchar, " +
      "size varchar, " +
      "image blob, " +
      "attempted int default 0, " +
      "PRIMARY KEY(token));" +
      """
      CREATE INDEX IF NOT EXISTS attempted ON challenge(attempted);
      """
  )
  stmt.execute(
    "CREATE TABLE IF NOT EXISTS mapId" +
      "(uuid varchar, " +
      "token int, " +
      "lastServed timestamp, " +
      "PRIMARY KEY(uuid), " +
      "FOREIGN KEY(token) " +
      "REFERENCES challenge(token) " +
      "ON DELETE CASCADE)"
  )

  val insertPstmt: PreparedStatement = dbConn.con.prepareStatement(
    "INSERT INTO " +
      "challenge(id, secret, provider, contentType, contentLevel, contentInput, size, image) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
    Statement.RETURN_GENERATED_KEYS
  )

  val mapPstmt: PreparedStatement =
    dbConn.con.prepareStatement(
      "INSERT INTO " +
        "mapId(uuid, token, lastServed) " +
        "VALUES (?, ?, CURRENT_TIMESTAMP)"
    )

  val selectPstmt: PreparedStatement = dbConn.con.prepareStatement(
    "SELECT c.secret, c.provider " +
      "FROM challenge c, mapId m " +
      "WHERE m.token=c.token AND " +
      "DATEDIFF(MINUTE, CURRENT_TIMESTAMP, DATEADD(MINUTE, ?, m.lastServed)) > 0 AND " +
      "m.uuid = ?"
  )

  val imagePstmt: PreparedStatement = dbConn.con.prepareStatement(
    "SELECT image " +
      "FROM challenge c, mapId m " +
      "WHERE c.token=m.token AND " +
      "m.uuid = ?"
  )

  val updateAttemptedPstmt: PreparedStatement = dbConn.con.prepareStatement(
    "UPDATE challenge " +
      "SET attempted = attempted+1 " +
      "WHERE token = ?;"
  )

  val countForParameterPstmt: PreparedStatement = dbConn.con.prepareStatement(
    s"""
      SELECT count(*) as count
        FROM challenge
        WHERE attempted < $maxAttempts AND
        contentLevel = ? AND
        contentType = ? AND
        contentInput = ? AND
        size = ?
        """
  )

  val tokenPstmt: PreparedStatement = dbConn.con.prepareStatement(
    s"""
      SELECT token, attempted
        FROM challenge
        WHERE attempted < $maxAttempts AND
        contentLevel = ? AND
        contentType = ? AND
        contentInput = ? AND
        size = ?
        LIMIT 1
        OFFSET FLOOR(RAND()*?)
         """
  )

  val deleteAnswerPstmt: PreparedStatement = dbConn.con.prepareStatement(
    "DELETE FROM mapId WHERE uuid = ?"
  )

  val challengeGCPstmt: PreparedStatement = dbConn.con.prepareStatement(
    s"""DELETE FROM challenge
      WHERE attempted >= $maxAttempts AND
      token NOT IN (SELECT token FROM mapId)"""
  )

  val mapIdGCPstmt: PreparedStatement = dbConn.con.prepareStatement(
    "DELETE FROM mapId WHERE DATEDIFF(MINUTE, CURRENT_TIMESTAMP, DATEADD(MINUTE, ?, lastServed)) < 0"
  )

  val getCountChallengeTable: PreparedStatement = dbConn.con.prepareStatement(
    "SELECT COUNT(*) AS total FROM challenge"
  )

  val getChallengeTable: PreparedStatement = dbConn.con.prepareStatement(
    "SELECT * FROM challenge"
  )

  val getMapIdTable: PreparedStatement = dbConn.con.prepareStatement(
    "SELECT * FROM mapId"
  )

  val shutdown: PreparedStatement = dbConn.con.prepareStatement(
    "SHUTDOWN"
  )

  val shutdownCompact: PreparedStatement = dbConn.con.prepareStatement(
    "SHUTDOWN COMPACT"
  )

}

object Statements {
  /* Note: h2 documentation recommends using a separate DB connection per thread
     But in practice, as of version 1.4.200, multiple connections occassionally shows error on the console of the form
     ```
     org.h2.jdbc.JdbcSQLNonTransientException: General error: "java.lang.NullPointerException"; SQL statement:
     SELECT image FROM challenge c, mapId m WHERE c.token=m.token AND m.uuid = ? [50000-200]
     ```
   */
  private val dbConn: DBConn = new DBConn()
  var maxAttempts: Int = 10
  val tlStmts: ThreadLocal[Statements] = ThreadLocal.withInitial(() => new Statements(dbConn, maxAttempts))
}
