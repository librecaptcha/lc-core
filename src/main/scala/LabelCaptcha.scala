import java.io.File
import javax.imageio.ImageIO
import scala.collection.mutable.Map
import java.nio.file.{Files,Path,StandardCopyOption}
import java.awt.image.BufferedImage
import java.awt.{Graphics2D,Color}

class LabelCaptcha extends CaptchaProvider {
  var knownFiles = new File("known").list.toList
  var unknownFiles = new File("unknown").list.toList
  val tokenImagePair = Map[String, ImagePair]()
  val unknownAnswers = Map[String, Map[String, Int]]()
  val total = Map[String, Int]()
  for(file <- unknownFiles) {
      unknownAnswers += file -> Map[String, Int]()
      total += file -> 0
  }
  def getChallenge(): Challenge = synchronized {
    val r = scala.util.Random.nextInt(knownFiles.length)
    val s = scala.util.Random.nextInt(unknownFiles.length)
    val knownImageFile = knownFiles(r)
    val unknownImageFile = unknownFiles(s)
    val ip = new ImagePair(knownImageFile, unknownImageFile)
    val token = scala.util.Random.nextInt(10000).toString
    tokenImagePair += token -> ip
    var knownImage = ImageIO.read(new File("known/"+knownImageFile))
    var unknownImage = ImageIO.read(new File("unknown/"+unknownImageFile))
    val width = knownImage.getWidth()+unknownImage.getWidth()
    val height = List(knownImage.getHeight(), unknownImage.getHeight()).max
    val imageType = knownImage.getType()
    val finalImage = new BufferedImage(width, height, imageType)
    val g = finalImage.createGraphics()
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, finalImage.getWidth(), finalImage.getHeight())
    g.drawImage(knownImage, null, 0, 0)
    g.drawImage(unknownImage, null, knownImage.getWidth(), 0)
    g.dispose()
    val challenge = new Challenge(token, finalImage)
    challenge
  }
  def checkAnswer(token: String, input: String): Boolean = synchronized {
    val expectedAnswer = tokenImagePair(token).known.split('.')(0)
    val userAnswer = input.split(' ')
    if(userAnswer(0)==expectedAnswer) {
      val unknownFile = tokenImagePair(token).unknown
      if((unknownAnswers(unknownFile)).contains(userAnswer(1))) {
        unknownAnswers(unknownFile)(userAnswer(1)) += 1
        total(unknownFile) += 1
      } else {
        unknownAnswers(unknownFile)+=(userAnswer(1)) -> 1
        total(unknownFile) += 1
      }
      if(total(unknownFile)>=3) {
        if((unknownAnswers(unknownFile)(userAnswer(1))/total(unknownFile))>=0.9) {
          unknownAnswers -= unknownFile
          Files.move(new File("unknown/"+unknownFile).toPath, new File("known/"+userAnswer(1)+".png").toPath, StandardCopyOption.REPLACE_EXISTING)
          knownFiles = new File("known").list.toList
          unknownFiles = new File("unknown").list.toList
        }
      }
      true
    } else {
      false
    }
  }
}

class ImagePair(val known: String, val unknown: String)
