package weco.concepts.aggregator
import grizzled.slf4j.Logging
import ujson.Obj
import ujson.Value.Value
import weco.concepts.common.model._
import weco.concepts.common.json.JsonOps._

object ConceptExtractor {
  val conceptTypes = Seq("Concept", "Person", "Organisation", "Meeting", "Period")
  def apply(jsonString: String): Seq[UsedConcept] =
    allConcepts(ujson.read(jsonString)).distinctBy(_.identifier)

  private def allConcepts(json: Value): Seq[UsedConcept] =
    json match {
      case arr: ujson.Arr => arr.arr.flatMap(allConcepts).toList
      case obj: ujson.Obj if isConcept(obj) => UsedConcepts(obj)
      case obj: ujson.Obj => obj.obj.values.flatMap(allConcepts).toList
      case _              => Nil
    }

  /**
   * Determines whether a given block of JSON represents a Concept
   * as returned from the Catalogue API.
   * A Concept is a block of JSON with a type property containing
   * one of the "Concept" types (Person, Concept, etc.),
   * and a list of identifiers.
   *
   * There are other properties that are vital to the extraction of
   * a Concept from such a JSON block, but these are the minimal
   * conditions that differentiate a concept from a non-concept.
   * The absence or malformation of those other properties represents
   * a concept that is itself malformed, and should be notified.
   */
  private def isConcept(json: Value): Boolean = {
    json.opt[String]("type") match {
      case None              => false
      case Some(conceptType) =>
        conceptTypes.contains(conceptType) && json.obj.contains("identifiers")
    }
  }
}

object UsedConcepts extends Logging{
  def apply(conceptJson: ujson.Obj): Seq[UsedConcept] = {
    // straight to get, it should have been verified before now
    // that identifiers exists.
    conceptJson.optSeq("identifiers").get.flatMap {
      conceptWithSource(conceptJson, _)
    }
  }

  private def conceptWithSource(conceptJson: Obj, sourceIdentifier: Value): Option[UsedConcept] = {
    try {
      val authority = sourceIdentifier.opt[Value]("identifierType").get
      Some(UsedConcept(
        identifier = Identifier(
          value = sourceIdentifier.opt[String]("value").get,
          identifierType = identifierTypeFromAuthority(authority)
        ),
        label = conceptJson.opt[String]("label").get,
        canonicalId = conceptJson.opt[String]("id").get,
      ))
    } catch {
      case exception: Exception =>
        warn(s"Malformed Concept encountered: $exception ${conceptJson.render(indent=2)}")
        None
    }
  }

  private def identifierTypeFromAuthority(
    sourceType: ujson.Value
  ): IdentifierType = {
    val sourceTyepId = sourceType.opt[String]("id").get
    IdentifierType.typeMap.getOrElse(
      sourceTyepId,
      throw BadIdentifierTypeException(sourceTyepId)
    )
  }
}
