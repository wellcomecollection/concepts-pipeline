package weco.concepts.ingestor.stages

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.ingestor.model._

class TransformerTest extends AnyFunSpec with Matchers {
  describe("LoC transformer") {
    it("transforms a concept with an ID, a prefLabel, and altLabels") {
      Transformer.subjectsTransformer.transform(
        TestData.completeConcept
      ) should contain(
        Concept(
          identifier = Identifier(
            value = "sh95000541",
            identifierType = IdentifierType.LCSubjects
          ),
          label = "World Wide Web",
          alternativeLabels = Seq(
            "W3 (World Wide Web)",
            "WWW (World Wide Web)",
            "Web (World Wide Web)",
            "World Wide Web (Information retrieval system)"
          )
        )
      )
    }

    it("transforms a concept with an ID and a prefLabel, but no altLabels") {
      Transformer.subjectsTransformer.transform(
        TestData.noAltLabels
      ) should contain(
        Concept(
          identifier = Identifier(
            value = "sh2003010454",
            identifierType = IdentifierType.LCSubjects
          ),
          label = "Wellcome Building (London, England)",
          alternativeLabels = Nil
        )
      )
    }

    it("returns None for concepts missing a prefLabel") {
      Transformer.subjectsTransformer.transform(
        TestData.deprecatedConcept
      ) shouldBe None
    }

    it("Handles flat labels for names") {
      Transformer.namesTransformer.transform(
        TestData.flatLabelConcept
      ) should contain(
        Concept(
          identifier = Identifier(
            value = "n83217500",
            identifierType = IdentifierType.LCNames
          ),
          label = "Wellcome, Henry S. (Henry Solomon), Sir, 1853-1936",
          alternativeLabels = Seq(
            "Wellcome, H. S. (Henry Solomon), Sir, 1853-1936",
            "Wellcome, Henry Solomon, Sir, 1853-1936",
            "Wellcome, Henry, Sir, 1853-1936"
          )
        )
      )
    }
  }
}

object TestData {
  val completeConcept =
    """{"@context": "http://v3/authorities/subjects/context.json", "@graph": [{"@id": "http://id.loc.gov/authorities/subjects/sh88002671", "@type": "skos:Concept", "skos:prefLabel": {"@language": "en", "@value": "Hypertext systems"}}, {"@id": "http://id.loc.gov/authorities/subjects/sh92002381", "@type": "skos:Concept", "skos:prefLabel": {"@language": "en", "@value": "Multimedia systems"}}, {"@id": "http://id.loc.gov/authorities/subjects/sh95000541", "@type": "skos:Concept", "skos:altLabel": [{"@language": "en", "@value": "W3 (World Wide Web)"}, {"@language": "en", "@value": "WWW (World Wide Web)"}, {"@language": "en", "@value": "Web (World Wide Web)"}, {"@language": "en", "@value": "World Wide Web (Information retrieval system)"}], "skos:broader": [{"@id": "http://id.loc.gov/authorities/subjects/sh88002671"}, {"@id": "http://id.loc.gov/authorities/subjects/sh92002381"}], "skos:changeNote": [{"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b1"}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b2"}], "skos:editorial": "headings beginning with the words [Web] or [Web-based]", "skos:inScheme": {"@id": "http://id.loc.gov/authorities/subjects"}, "skos:narrower": [{"@id": "http://id.loc.gov/authorities/subjects/sh2002000569"}, {"@id": "http://id.loc.gov/authorities/subjects/sh2003001415"}, {"@id": "http://id.loc.gov/authorities/subjects/sh2007008317"}, {"@id": "http://id.loc.gov/authorities/subjects/sh2007008319"}, {"@id": "http://id.loc.gov/authorities/subjects/sh2008009697"}, {"@id": "http://id.loc.gov/authorities/subjects/sh97003254"}], "skos:prefLabel": {"@language": "en", "@value": "World Wide Web"}, "skos:related": {"@id": "http://id.loc.gov/authorities/subjects/sh92002816"}, "skosxl:altLabel": [{"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b3"}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b4"}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b5"}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b6"}]}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b1", "@type": "cs:ChangeSet", "cs:changeReason": "revised", "cs:createdDate": {"@type": "xsd:dateTime", "@value": "2021-11-23T12:02:16"}, "cs:creatorName": {"@id": "http://id.loc.gov/vocabulary/organizations/dlc"}, "cs:subjectOfChange": {"@id": "http://id.loc.gov/authorities/subjects/sh95000541"}}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b2", "@type": "cs:ChangeSet", "cs:changeReason": "new", "cs:createdDate": {"@type": "xsd:dateTime", "@value": "2000-04-28T00:00:00"}, "cs:creatorName": {"@id": "http://id.loc.gov/vocabulary/organizations/dlc"}, "cs:subjectOfChange": {"@id": "http://id.loc.gov/authorities/subjects/sh95000541"}}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b3", "@type": "skosxl:Label", "skosxl:literalForm": {"@language": "en", "@value": "W3 (World Wide Web)"}}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b4", "@type": "skosxl:Label", "skosxl:literalForm": {"@language": "en", "@value": "Web (World Wide Web)"}}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b5", "@type": "skosxl:Label", "skosxl:literalForm": {"@language": "en", "@value": "World Wide Web (Information retrieval system)"}}, {"@id": "_:n5b4760e689dd4c3fad46356fb72f9ca2b6", "@type": "skosxl:Label", "skosxl:literalForm": {"@language": "en", "@value": "WWW (World Wide Web)"}}, {"@id": "http://id.loc.gov/authorities/subjects/sh92002816", "@type": "skos:Concept", "skos:prefLabel": {"@language": "en", "@value": "Internet"}}], "@id": "/authorities/subjects/sh95000541"}"""
  val noAltLabels =
    """{"@context": "http://v3/authorities/subjects/context.json", "@graph": [{"@id": "http://id.loc.gov/authorities/subjects/sh2004005949", "@type": "skos:Concept", "skos:prefLabel": {"@language": "en", "@value": "Library buildings--England"}}, {"@id": "http://id.loc.gov/authorities/subjects/sh2001005531", "@type": "skos:Concept", "skos:prefLabel": {"@language": "en", "@value": "Museum buildings--England"}}, {"@id": "http://id.loc.gov/authorities/subjects/sh92006359", "@type": "skos:Concept", "skos:prefLabel": {"@language": "en", "@value": "Office buildings--England"}}, {"@id": "http://id.loc.gov/authorities/subjects/sh2003010454", "@type": "skos:Concept", "skos:broader": [{"@id": "http://id.loc.gov/authorities/subjects/sh2001005531"}, {"@id": "http://id.loc.gov/authorities/subjects/sh2004005949"}, {"@id": "http://id.loc.gov/authorities/subjects/sh92006359"}], "skos:changeNote": [{"@id": "_:nbf5ed533a09847fd8fb49fd7da4a470db1"}, {"@id": "_:nbf5ed533a09847fd8fb49fd7da4a470db2"}], "skos:inScheme": {"@id": "http://id.loc.gov/authorities/subjects"}, "skos:prefLabel": {"@language": "en", "@value": "Wellcome Building (London, England)"}}, {"@id": "_:nbf5ed533a09847fd8fb49fd7da4a470db1", "@type": "cs:ChangeSet", "cs:changeReason": "new", "cs:createdDate": {"@type": "xsd:dateTime", "@value": "2004-01-07T00:00:00"}, "cs:creatorName": {"@id": "http://id.loc.gov/vocabulary/organizations/uklw"}, "cs:subjectOfChange": {"@id": "http://id.loc.gov/authorities/subjects/sh2003010454"}}, {"@id": "_:nbf5ed533a09847fd8fb49fd7da4a470db2", "@type": "cs:ChangeSet", "cs:changeReason": "revised", "cs:createdDate": {"@type": "xsd:dateTime", "@value": "2004-02-13T14:12:52"}, "cs:creatorName": {"@id": "http://id.loc.gov/vocabulary/organizations/dlc"}, "cs:subjectOfChange": {"@id": "http://id.loc.gov/authorities/subjects/sh2003010454"}}], "@id": "/authorities/subjects/sh2003010454"}"""
  val deprecatedConcept =
    """{"@context": "http://v3/authorities/subjects/context.json", "@graph": [{"@id": "http://id.loc.gov/authorities/subjects/sh2009008863", "@type": "skosxl:Label", "rdfs:seeAlso": {"@id": "http://id.loc.gov/authorities/names/nb2019004203"}, "skos:changeNote": [{"@id": "_:n1a4576f9ba9344a3b254de66f5ec3c39b1"}, {"@id": "_:n1a4576f9ba9344a3b254de66f5ec3c39b2"}, {"@language": "en", "@value": "This authority record has been deleted because the subject heading is covered by an identical name heading (DLC)nb2019004203"}], "skosxl:literalForm": {"@language": "en", "@value": "Langdon, Robert (Fictitious character)"}}, {"@id": "_:n1a4576f9ba9344a3b254de66f5ec3c39b1", "@type": "cs:ChangeSet", "cs:changeReason": "new", "cs:createdDate": {"@type": "xsd:dateTime", "@value": "2009-11-12T00:00:00"}, "cs:creatorName": {"@id": "http://id.loc.gov/vocabulary/organizations/upb"}, "cs:subjectOfChange": {"@id": "http://id.loc.gov/authorities/subjects/sh2009008863"}}, {"@id": "_:n1a4576f9ba9344a3b254de66f5ec3c39b2", "@type": "cs:ChangeSet", "cs:changeReason": "deprecated", "cs:createdDate": {"@type": "xsd:dateTime", "@value": "2019-06-17T08:47:03"}, "cs:creatorName": {"@id": "http://id.loc.gov/vocabulary/organizations/uk"}, "cs:subjectOfChange": {"@id": "http://id.loc.gov/authorities/subjects/sh2009008863"}}], "@id": "/authorities/subjects/sh2009008863"}"""
  val flatLabelConcept =
    """{"@context": "http://v3/authorities/names/context.json", "@graph": [{"@id": "http://id.loc.gov/authorities/names/n83217500", "@type": "skos:Concept", "skos:altLabel": ["Wellcome, H. S. (Henry Solomon), Sir, 1853-1936", "Wellcome, Henry Solomon, Sir, 1853-1936", "Wellcome, Henry, Sir, 1853-1936"], "skos:changeNote": [{"@id": "_:n079ab77cc4fe49b6996027184c8e6630b1"}, {"@id": "_:n079ab77cc4fe49b6996027184c8e6630b2"}], "skos:exactMatch": {"@id": "http://viaf.org/viaf/sourceID/LC%7Cn++83217500#skos:Concept"}, "skos:inScheme": {"@id": "http://id.loc.gov/authorities/names"}, "skos:prefLabel": "Wellcome, Henry S. (Henry Solomon), Sir, 1853-1936", "skosxl:altLabel": [{"@id": "_:n079ab77cc4fe49b6996027184c8e6630b3"}, {"@id": "_:n079ab77cc4fe49b6996027184c8e6630b4"}, {"@id": "_:n079ab77cc4fe49b6996027184c8e6630b5"}]}, {"@id": "_:n079ab77cc4fe49b6996027184c8e6630b1", "@type": "cs:ChangeSet", "cs:changeReason": "new", "cs:createdDate": {"@type": "xsd:dateTime", "@value": "1983-09-13T00:00:00"}, "cs:creatorName": {"@id": "http://id.loc.gov/vocabulary/organizations/dlc"}, "cs:subjectOfChange": {"@id": "http://id.loc.gov/authorities/names/n83217500"}}, {"@id": "_:n079ab77cc4fe49b6996027184c8e6630b2", "@type": "cs:ChangeSet", "cs:changeReason": "revised", "cs:createdDate": {"@type": "xsd:dateTime", "@value": "2021-05-07T07:01:04"}, "cs:creatorName": {"@id": "http://id.loc.gov/vocabulary/organizations/mosu"}, "cs:subjectOfChange": {"@id": "http://id.loc.gov/authorities/names/n83217500"}}, {"@id": "_:n079ab77cc4fe49b6996027184c8e6630b3", "@type": "skosxl:Label", "skosxl:literalForm": "Wellcome, Henry Solomon, Sir, 1853-1936"}, {"@id": "_:n079ab77cc4fe49b6996027184c8e6630b4", "@type": "skosxl:Label", "skosxl:literalForm": "Wellcome, Henry, Sir, 1853-1936"}, {"@id": "_:n079ab77cc4fe49b6996027184c8e6630b5", "@type": "skosxl:Label", "skosxl:literalForm": "Wellcome, H. S. (Henry Solomon), Sir, 1853-1936"}], "@id": "/authorities/names/n83217500"}"""
}
