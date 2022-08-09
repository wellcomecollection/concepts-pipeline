// TODO: These will probably end up living in a common project/library in this repo
package weco.concepts.common.model

case class Concept(
  identifier: Identifier,
  label: String,
  alternativeLabels: Seq[String]
)
