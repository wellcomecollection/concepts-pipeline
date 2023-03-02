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

/** This flow filters catalogue concepts on whether a Concept's canonicalId
  * already exists in the index, allowing the bulk update to only push unseen
  * ids to Elasticsearch.
  *
  * At bulk scale, scripted updates are prohibitively slow, because there is no
  * way for Elasticsearch to short-circuit the update to a NOOP. This flow
  * provides that NOOP shortcut.
  *
  * This has no significant effect on the speed of indexing the API snapshot
  * into a pristine database with a simple doc-based bulk upsert, but brings the
  * time taken to run the scripted version down to match the simple method.
  *
  * As an example, running the bulk import locally using docker compose took 1
  * hour without this and 6 minutes with.
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
      // We are only interested in canonicalId, so that it can be match against
      // canonicalIds in the input group of Concepts. Just return that from
      // stored fields.
      // As well as performance, this has the added advantage that canonicalId will always be
      // a list, regardless of how many canonicalIds a record has (as opposed to _source.canonicalId
      // which will either be a string or a list of strings)
      "_source" -> false,
      "fields" -> Seq("canonicalId"),
      "query" -> ujson.Obj(
        "bool" -> ujson.Obj(
          // Use filter context.  As well as avoiding scoring, this allows ES to use its magic bitset caching
          // Most concepts are expected to appear more than once in the Works corpus
          // so if we are lucky with cache evictions, this can make a bulk update a bit quicker.
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
          concepts.filter(concept => !foundIds.contains(concept.canonicalId.head))
        info(
          s"from ${concepts.length} ids, found ${concepts.length - missingConcepts.length} already in the index"
        )
        missingConcepts
    }
  }
}
