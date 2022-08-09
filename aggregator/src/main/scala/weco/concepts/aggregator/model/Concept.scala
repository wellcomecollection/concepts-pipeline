package weco.concepts.aggregator.model
//TODO: Harmonise with ingestor models

case class Concept(
  canonicalIdentifier: String,
  identifier: Identifier,
  label: String,
  alternativeLabels: Seq[String]
)
