package lc.captchas

import java.io.File
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import scala.collection.mutable.Map
import java.nio.file.{Files,Path,StandardCopyOption}
import java.awt.image.BufferedImage
import java.awt.{Graphics2D,Color}
import lc.captchas.interfaces.ChallengeProvider
import lc.captchas.interfaces.Challenge

class LabelCaptcha extends ChallengeProvider {
  private var knownFiles = new File("known").list.toList
  private var unknownFiles = new File("unknown").list.toList
  private var unknownAnswers = Map[String, Map[String, Int]]()
  private var total = Map[String, Int]()

  for(file <- unknownFiles) {
      unknownAnswers += file -> Map[String, Int]()
      total += file -> 0
  }

  def getId = "LabelCaptcha"

  def returnChallenge(): Challenge = synchronized {
    val r = scala.util.Random.nextInt(knownFiles.length)
    val s = scala.util.Random.nextInt(unknownFiles.length)
    val knownImageFile = knownFiles(r)
    val unknownImageFile = unknownFiles(s)
    val ip = new ImagePair(knownImageFile, unknownImageFile)

    var knownImage = ImageIO.read(new File("known/"+knownImageFile))
    var unknownImage = ImageIO.read(new File("unknown/"+unknownImageFile))
    val mergedImage = merge(knownImage, unknownImage)

    val token = encrypt(knownImageFile + "," + unknownImageFile)
    val baos = new ByteArrayOutputStream()
    ImageIO.write(mergedImage,"png",baos)

    new Challenge(baos.toByteArray(), "image/png", token)
  }

  private def merge(knownImage: BufferedImage, unknownImage: BufferedImage) = {
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
    finalImage
  }

  def checkAnswer(token: String, input: String): Boolean = synchronized {
    val parts = decrypt(token).split(",")
    val knownImage = parts(0)
    val unknownImage = parts(1)
    val expectedAnswer = knownImage.split('.')(0)
    val userAnswer = input.split(' ')
    if(userAnswer(0)==expectedAnswer) {
      val unknownFile = unknownImage
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

  // TODO: Encryption is not implemented for the POC, since the API re-maps the tokens anyway.
  //       But we need to encrypt after POC, to avoid leaking file-names.
  //       There are good ideas here: https://stackoverflow.com/questions/1205135/how-to-encrypt-string-in-java
  private def encrypt(s: String) = s
  private def decrypt(s: String) = s
}

class ImagePair(val known: String, val unknown: String)
