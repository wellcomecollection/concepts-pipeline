package weco.concepts.common
import scala.io.Source

trait ResourceLoader {
  def loadJsonResource(name: String): String
}

object ResourceLoader extends ResourceLoader {

  def loadJsonResource(name: String): String =
    Source.fromResource(s"$name.json").getLines().mkString(" ")

}
