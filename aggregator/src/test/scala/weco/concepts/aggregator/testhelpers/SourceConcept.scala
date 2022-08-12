package weco.concepts.aggregator.testhelpers

object SourceConcept extends ValueGenerators {
  def apply(
             authority: String = anAuthority,
             identifier: String = anExternalId,
             label: String = aLabel,
             canonicalId: String = aCanonicalId,
             ontologyType: String = aType
           ): SourceConcept = new SourceConcept(
    authority,
    identifier,
    label,
    canonicalId,
    ontologyType
  )

}

/*
 * Utility object to generate concepts in the form in which they are
 * presented in the Works API.  This is the form in which they arrive
 * into the ConceptExtractor
 */
case class SourceConcept(
                          authority: String,
                          identifier: String,
                          label: String,
                          canonicalId: String,
                          ontologyType: String
                        ) {
  override def toString: String =
    s"""
       |{
       |  "id": "$canonicalId",
       |  "identifiers": [
       |    {
       |      "identifierType": {
       |        "id": "$authority",
       |        "label": "This field is ignored",
       |        "type": "IdentifierType"
       |      },
       |      "value": "$identifier",
       |      "type": "Identifier"
       |    }
       |  ],
       |  "label": "$label",
       |  "type": "$ontologyType"
       |}
       |""".stripMargin
}