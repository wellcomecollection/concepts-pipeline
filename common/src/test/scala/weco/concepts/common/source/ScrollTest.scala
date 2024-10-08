package weco.concepts.common.source

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.apache.pekko.util.ByteString
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class ScrollTest extends AnyFunSpec with Matchers {
  it("decompresses a gzipped stream and splits it into lines") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    val lines = Seq(
      "Freude, schöner Götterfunken",
      "Tochter aus Elysium,",
      "Wir betreten feuertrunken,",
      "Himmlische, dein Heiligtum!",
      "Deine Zauber binden wieder",
      "Was die Mode streng geteilt;",
      "Alle Menschen werden Brüder",
      "Wo dein sanfter Flügel weilt."
    )

    Source
      .single(gzip(lines.mkString("\n")))
      .via(Scroll.fromCompressed(32))
      .runWith(TestSink[String]())
      .request(lines.length)
      .expectNextN(lines)
      .expectComplete()
  }

  // Copied from https://gist.github.com/owainlewis/1e7d1e68a6818ee4d50e
  def gzip(str: String): ByteString = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)
    gzipOutputStream.write(str.getBytes)
    gzipOutputStream.close()
    val gzipped = byteArrayOutputStream.toByteArray
    byteArrayOutputStream.close()
    ByteString(gzipped)
  }

  it("fails if any line is too long") {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    val lines = Seq(
      "Der Vogelfänger bin ich ja!",
      "Steht's lustg, heissa hopsassa!", // longer than 30
      "Ich Vogelfänger bin bekannt"
    )

    Source
      .single(gzip(lines.mkString("\n")))
      .via(Scroll.fromCompressed(30))
      .runWith(TestSink[String]())
      .request(lines.length)
      .expectError()
  }
}
