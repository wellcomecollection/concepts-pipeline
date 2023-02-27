package weco.concepts.common
import scala.io.Source

trait ResourceLoader {
  def loadJsonResource(name: String): String
}

object ResourceLoader extends ResourceLoader {

  /*
   * Load the named resource to a string.
   *
   * This method deliberately replaces any newlines in the source with a single space.
   * In doing so, it allows the source JSON file to contain line breaks for clarity
   * while still resulting in valid JSON when loaded.
   *
   * This is particularly useful for Elasticsearch Scripts, which are somewhat
   * unreadable when squished on to a single line.
   */
  def loadJsonResource(name: String): String =
    Source.fromResource(s"$name.json").getLines().mkString("")

}
