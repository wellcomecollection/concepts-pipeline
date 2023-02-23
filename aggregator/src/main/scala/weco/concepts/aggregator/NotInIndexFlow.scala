package weco.concepts.aggregator

import akka.NotUsed
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse,
  StatusCodes
}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import ujson.Value
import weco.concepts.common.elasticsearch.ElasticHttpClient
import weco.concepts.common.json.JsonOps._
import weco.concepts.common.model.CatalogueConcept

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/** At bulk scale, even noop upserts in Elasticsearch can be a little lethargic,
  * and scripted updates are prohibitively slow. This flow filters catalogue
  * concepts on whether they already exist in the index.
  */
class NotInIndexFlow(
  elasticHttpClient: ElasticHttpClient,
  indexName: String,
  maxTerms: Int = 10000 // default max for a terms query is 10k.
)(implicit mat: Materializer, ec: ExecutionContext)
    extends Logging {

  def flow: Flow[CatalogueConcept, CatalogueConcept, NotUsed] =
    Flow[CatalogueConcept]
      .grouped(maxTerms)
      .via(findMissingCanonicalIds)
      .mapConcat(identity)

  private def queryBody(concepts: Seq[CatalogueConcept]): String = ujson.write(
    ujson.Obj(
      "size" -> maxTerms,
      "fields" -> Seq("canonicalId"),
      "query" -> ujson.Obj(
        "bool" -> ujson.Obj(
          "filter" -> Seq(
            ujson.Obj(
              "terms" -> ujson.Obj(
                "canonicalId" -> concepts.map(_.canonicalId)
              )
            )
          )
        )
      )
    )
  )

  private def findMissingCanonicalIds
    : Flow[Seq[CatalogueConcept], Seq[CatalogueConcept], NotUsed] = {
    Flow[Seq[CatalogueConcept]]
      .map { concepts =>
        HttpRequest(
          method = HttpMethods.POST,
          uri = s"/$indexName/_search?filter_path=hits.hits.fields.canonicalId",
          entity =
            HttpEntity(ContentTypes.`application/json`, queryBody(concepts))
        ) -> concepts
      }
      .via(elasticHttpClient.flow[Seq[CatalogueConcept]])
      .map {
        case (Success(response), concepts) if response.status.isSuccess() =>
          (response, concepts)
        case (Success(errorResponse), _) =>
          error("Error response returned when checking for existing ids")
          throw new RuntimeException(s"Response: $errorResponse")
        case (Failure(exception), _) =>
          error("Unexpected error when checking for existing ids")
          throw exception
      }
      .mapAsync(1) {
        case (
              HttpResponse(StatusCodes.OK, _, entity, _),
              concepts
            ) =>
          Unmarshal(entity)
            .to[String]
            .map { responseBody: String =>
              filterExistingConcepts(concepts, responseBody)

            }
        case (_, concepts) =>
          Future(concepts)
      }
  }

  private def filterExistingConcepts(
    concepts: Seq[CatalogueConcept],
    responseBody: String
  ): Seq[CatalogueConcept] = {
    ujson
      .read(responseBody)
      .opt[Seq[Value]]("hits", "hits")
      .flatMap { hits =>
        Option(
          hits
            .flatMap(_.opt[Seq[Value]]("fields", "canonicalId"))
            .flatMap { canonicalIds: Seq[Value] =>
              canonicalIds.map(_.str)
            }
        )
      } match {
      // It is possible that there are no matches at all.  In this case, there will be no hits.hits.fields.canonicalId
      // In this case, all of the concepts in the batch are "new".
      case None =>
        info(s"from ${concepts.length} ids, none were already in the index")
        concepts
      // Otherwise, filter out any concepts from the initial batch whose canonicalId is already in the database.
      case Some(seq) =>
        val foundIds: Set[String] = seq.toSet
        val missingConcepts =
          concepts.filter(concept => foundIds.contains(concept.canonicalId))
        info(
          s"from ${concepts.length} ids, found ${concepts.length - missingConcepts.length} are already in the index"
        )
        missingConcepts
    }
  }
}
