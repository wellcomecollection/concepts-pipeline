package weco.concepts.common.source

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.util.ByteString
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
      .via(Scroll.apply)
      .runWith(TestSink.probe[String])
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
}
