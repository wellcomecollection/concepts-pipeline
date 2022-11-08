package weco.concepts.aggregator
import grizzled.slf4j.Logging
import ujson.Obj
import ujson.Value.Value
import weco.concepts.common.model._
import weco.concepts.common.json.JsonOps._

import scala.annotation.tailrec

object ConceptExtractor extends Logging {
  val conceptTypes =
    Seq("Concept", "Person", "Organisation", "Meeting", "Period", "Subject")
  def apply(jsonString: String): Seq[CatalogueConcept] = {
    val jsonObj = ujson.read(jsonString)
    val concepts = allConcepts(List(jsonObj), Nil).toList
      .distinctBy(_.identifier)
    debug(s"extracted ${concepts.length} concepts from ${jsonObj.obj("id")}")
    concepts
  }

  /** Extract concepts from wherever they may be in a JSON document.
    */
  @tailrec
  private def allConcepts(
    jsons: Seq[Value],
    acc: Seq[CatalogueConcept]
  ): Seq[CatalogueConcept] = {
    jsons match {
      case Nil => acc
      case _ =>
        val (nextJsons, concepts) = jsons.map {
          case obj: ujson.Obj if isConcept(obj) =>
            (obj.obj.values, CatalogueConcepts(obj))
          case arr: ujson.Arr => (arr.arr, Nil)
          case obj: ujson.Obj => (obj.obj.values, Nil)
          case _              => (Nil, Nil)
        }.unzip
        allConcepts(nextJsons.flatten, acc ++ concepts.flatten)
    }
  }

  /** Determines whether a given block of JSON represents a Concept as returned
    * from the Catalogue API. A Concept is a block of JSON with a type property
    * containing one of the "Concept" types (Person, Concept, etc.), and a list
    * of identifiers.
    *
    * There are other properties that are vital to the extraction of a Concept
    * from such a JSON block, but these are the minimal conditions that
    * differentiate a concept from a non-concept. The absence or malformation of
    * those other properties represents a concept that is itself malformed, and
    * should be notified.
    */
  private def isConcept(json: Value): Boolean = {
    json
      .opt[String]("type")
      .exists(conceptType =>
        conceptTypes.contains(conceptType) && json.obj.contains("identifiers")
      )
  }
}

object CatalogueConcepts extends Logging {

  /** Transform a block of JSON representing a Concept from the Catalogue API
    * into one or more CatalogueConcepts
    *
    * A Catalogue API Concept contains a list of identifiers from one or more
    * authorities, whereas a CatalogueConcept contains just one. So a catalogue
    * API concept may produce more than one CatalogueConcept.
    */
  def apply(conceptJson: ujson.Obj): Seq[CatalogueConcept] = {
    // straight to get, it should have been verified before now
    // that identifiers exists.
    conceptJson.optSeq("identifiers").get.flatMap {
      conceptWithSource(conceptJson, _)
    }
  }

  private def conceptWithSource(
    conceptJson: Obj,
    sourceIdentifier: Value
  ): Option[CatalogueConcept] = {
    try {
      val authority = sourceIdentifier.opt[Value]("identifierType").get
      Some(
        CatalogueConcept(
          identifier = Identifier(
            value = sourceIdentifier.opt[String]("value").get,
            identifierType = identifierTypeFromAuthority(authority)
          ),
          label = conceptJson.opt[String]("label").get,
          canonicalId = conceptJson.opt[String]("id").get,
          ontologyType = conceptJson.opt[String]("type").get
        )
      )
    } catch {
      case exception: Exception =>
        warn(
          s"Malformed Concept encountered: $exception ${conceptJson.render(indent = 2)}"
        )
        None
    }
  }

  private def identifierTypeFromAuthority(
    sourceType: ujson.Value
  ): IdentifierType = {
    val sourceTypeId = sourceType.opt[String]("id").get
    IdentifierType
      .fromId(sourceTypeId)
      .getOrElse(
        throw BadIdentifierTypeException(sourceTypeId)
      )
  }
}
