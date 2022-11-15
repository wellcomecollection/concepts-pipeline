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
      .distinctBy(_.canonicalId)
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
            (obj.obj.values, CatalogueConcepts.conceptWithSource(obj))
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
    * authorities and a CatalogueConcept contains just one, but we believe that
    * we should never see a case where that list contains more than one element.
    */
  def conceptWithSource(
    conceptJson: Obj
  ): Option[CatalogueConcept] =
    conceptJson.optSeq("identifiers").flatMap {
      case Seq(sourceIdentifier) =>
        val concept = for {
          authority <- sourceIdentifier.opt[String]("identifierType", "id")
          identifierType <- IdentifierType.fromId(authority)
          identifierValue <- sourceIdentifier.opt[String]("value")
          label <- conceptJson.opt[String]("label")
          canonicalId <- conceptJson.opt[String]("id")
          ontologyType <- conceptJson.opt[String]("type")
        } yield CatalogueConcept(
          identifier = Identifier(
            value = identifierValue,
            identifierType = identifierType
          ),
          label = label,
          canonicalId = canonicalId,
          ontologyType = ontologyType
        )
        if (concept.isEmpty) {
          warn(s"Encountered a malformed concept: ${ujson.write(conceptJson)}")
        }
        concept
      case Nil =>
        warn(s"Encountered a concept with no source identifiers! ${ujson.write(conceptJson)}")
        None
      case multipleIdentifiers =>
        warn(
          s"Encountered multiple source identifiers for a single canonicalId: ${multipleIdentifiers
              .map(ujson.write(_))
              .mkString(",\n")}"
        )
        None
    }
}
