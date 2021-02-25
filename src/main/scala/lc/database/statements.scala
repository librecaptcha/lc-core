package lc.database

import lc.database.DBConn
import java.sql.Statement
import java.sql.PreparedStatement

class Statements(dbConn: DBConn) {

  private val stmt = dbConn.getStatement()

  stmt.execute(
    "CREATE TABLE IF NOT EXISTS challenge" +
      "(token int auto_increment, " +
      "id varchar, " +
      "secret varchar, " +
      "provider varchar, " +
      "contentType varchar, " +
      "image blob, " +
      "attempted int default 0, " +
      "PRIMARY KEY(token))"
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
      "challenge(id, secret, provider, contentType, image) " +
      "VALUES (?, ?, ?, ?, ?)",
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
      "DATEDIFF(MINUTE, CURRENT_TIMESTAMP, DATEADD(MINUTE, 1, m.lastServed)) > 0 AND " +
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
      "WHERE token = (SELECT m.token " +
      "FROM mapId m, challenge c " +
      "WHERE m.token=c.token AND " +
      "m.uuid = ?)"
  )

  val tokenPstmt: PreparedStatement = dbConn.con.prepareStatement(
    "SELECT token " +
      "FROM challenge " +
      "WHERE attempted < 10 " +
      "ORDER BY RAND() LIMIT 1"
  )

  val deleteAnswerPstmt: PreparedStatement = dbConn.con.prepareStatement(
    "DELETE FROM mapId WHERE uuid = ?"
  )

  val challengeGCPstmt: PreparedStatement = dbConn.con.prepareStatement(
    "DELETE FROM challenge " +
      "WHERE attempted >= 10 AND " +
      "token NOT IN (SELECT token FROM mapId)"
  )

  val mapIdGCPstmt: PreparedStatement = dbConn.con.prepareStatement(
    "DELETE FROM mapId WHERE DATEDIFF(MINUTE, CURRENT_TIMESTAMP, DATEADD(MINUTE, 1, lastServed)) < 0"
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

}

object Statements {
  private val dbConn: DBConn = new DBConn()
  val tlStmts: ThreadLocal[Statements] = ThreadLocal.withInitial(() => new Statements(dbConn))
}
