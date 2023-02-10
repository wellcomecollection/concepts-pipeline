"""
Then for each list of ids, get all the records from teh concepts store for them.
This will give the authoritative name for each of them

Filter out the one (if any) that matches the original name

Then ask reporting for all varfields with that id in 0
If there are two 0s and no t, store it in the dodgy list.

If there is just the one 0, store it in the probably not dodgy list.
"""
import requests
import re
from elasticsearch import Elasticsearch
import os
import botocore
import botocore.session
from aws_secretsmanager_caching import SecretCache, SecretCacheConfig
from pprint import pprint
import csv
from itertools import chain
import json

def main():
    es_catalogue_concepts = Elasticsearch([
        "concepts-2022-08-31.es.eu-west-1.aws.found.io"
    ],scheme="https", port=9243,
        http_auth=("recorder", get_catalogue_concepts_secret())
    )

    es_concepts_store = Elasticsearch([
        "concepts-2022-08-31.es.eu-west-1.aws.found.io"
    ],scheme="https", port=9243,
        http_auth=("api", get_concepts_store_secret())
    )

    es_reporting = Elasticsearch([
        "d3f9c38fe7134d44b3ec7752d86b5e98.eu-west-1.aws.found.io"
    ],scheme="https", port=9243,
        http_auth=(get_reporting_user(), get_reporting_password())
    )

    # es_catalogue_concepts.search(index="catalogue-concepts")
    # es_concepts_store.search(index="concepts-store")
    #es_reporting.search(index="sierra_varfields")

    # First, aggregate the reused labels with their ids.
    reused_labels = get_reused_labels(es_catalogue_concepts)
    # Then for each list of ids, get all the records from the concepts store for them.
    # This will give the authoritative name for each of them
    reused_labels_with_named_ids = get_authoritative_names(es_concepts_store, reused_labels)
   # pprint(reused_labels_with_named_ids)
    # Filter out the one (if any) that matches the original name
    # and ask reporting for all varfields with those id in 0
    found_in_sierra = in_varfields(es_reporting, reused_labels_with_named_ids)
   # pprint(found_in_sierra)

    with open('dodgy.csv', 'w', newline='') as csvfile:
        fieldnames = found_in_sierra[0][0].keys()
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows((row for row in chain.from_iterable(found_in_sierra) if row["subfeld_0_count"] > 1))


def get_secret(as_profile, secret_name):
    client = botocore.session.Session(profile=as_profile).create_client('secretsmanager')
    cache_config = SecretCacheConfig()
    cache = SecretCache(config=cache_config, client=client)
    return cache.get_secret_string(secret_name)

def get_catalogue_concepts_secret():
    return get_secret("catalogue-developer", "elasticsearch/concepts-2022-08-31/recorder/password")

def get_concepts_store_secret():
    return get_secret("catalogue-developer", "elasticsearch/concepts-2022-08-31/api/password")

def get_reporting_user():
    return get_secret("platform-developer", "reporting/read_only/es_username")

def get_reporting_password():
    return get_secret("platform-developer", "reporting/read_only/es_password")

def do_query(ix, q):
    requests.get()

def get_authoritative_names(es, labels_to_ids):
    return {k: with_correct_names(es, v) for k, v in labels_to_ids.items()}

def with_correct_names(es, id_list):
    return {hit["fields"]["query.identifiers.value"][0]: hit["fields"]["query.label"][0] for hit in es.search(index="concepts-store", body=query_records_with_ids(id_list))["hits"]["hits"]}

def with_correct_name(hit):
    return {
        "id": hit["fields"]["query.identifiers.value"][0],
        "label": hit["fields"]["query.label"][0]
    }

def query_records_with_ids(ids):
    return {
        "_source": False,
        "fields": ["query.identifiers.value", "query.label"],
        "query": {
            "bool": {
                "must":[
                    {
                        "term": {
                            "query.identifiers.identifierType":  "lc-names"
                        }},
                    {"terms":
                        {
                            "query.identifiers.value": ids
                        }
                    }]
            }}
    }


def in_varfields(es, labels_to_ids):
    return [get_b_numbers(es, wrong_ids_for_label(k, v)) for k, v in labels_to_ids.items()]


def get_b_numbers(es, concept_ids):
    if not concept_ids:
        return []
    hits = es.search(index="sierra_varfields", body=query_varfields(concept_ids))["hits"]["hits"]
    try:
        return [{
            "bnumber": hit["fields"]["parent.idWithCheckDigit"][0],
            "marc_tag": hit["fields"]["varField.marcTag"][0],
            "field_content": " ".join(hit["fields"]["varField.subfields.content"]),
            "identifier": hit["matched_queries"][0].partition(':')[0],
            "LoC name": hit["matched_queries"][0].partition(':')[2],
            "subfeld_0_count": hit["fields"]["varField.subfields.tag"].count("0"),
            "subfeld_t_count": hit["fields"]["varField.subfields.tag"].count("t"),
        } for hit in hits]
    except:
        pprint(hits)
        pprint(concept_ids)
        print(json.dumps(query_varfields(concept_ids), indent=2))
        raise


def query_varfields(identifiers):
    query = {
        "size":30,
        "_source": False,
        "fields": [
            "parent.idWithCheckDigit", "varField.subfields.tag",
            "varField.subfields.content",   "varField.marcTag"

        ],
        "query": {
            "bool": {
                "should": [
                    {"wildcard": {
                        "varField.subfields.content.keyword": {
                            "value": id_to_wildcard(identifier[0]),
                            "_name": ": ".join(identifier)
                        }
                    }} for identifier in identifiers
                ]
            }
        }
    }
    # print(json.dumps(query, indent=2))
    # exit()
    return query


def wrong_ids_for_label(label, pairs):
    wrong_ids = [(k,v) for k,v in pairs.items() if v != label]
#    wrong_ids = [pair for pair in pairs if pair["label"] != label]
    if not wrong_ids:
        print("there were no wrong ids")
        pprint(pairs)
    return wrong_ids

def id_to_wildcard(identifier):
    """
    given a LoC id, return a wildcard string that would match any instances of it
    that we might expect to find in Sierra data

    A common format for LoC ids in Sierra is to separate the alphabetic prefix
    from the numeric id with one or more spaces.  e.g. " n  97800474".

    Leading spaces are ignored by the indexer anyway, so a matching wildcard query
    simply separates the prefix and id with a star
    >>> id_to_wildcard("n12345")
    'n*12345'
    >>> id_to_wildcard("no54321")
    'no*54321'
    """
    return "*".join(re.split("(\d+)", identifier, 1)[:-1])


def get_reused_labels(es):
    used_labels_aggregation = es.search(index="catalogue-concepts", body=AGGREGATE_REUSED_LABELS)
    return dict((collect_ids_from_bucket(bucket) for bucket in used_labels_aggregation["aggregations"]["common-labels"]["buckets"]))


def collect_ids_from_bucket(parent_bucket):
    return parent_bucket["key"], [bucket["key"] for bucket in parent_bucket["ids"]["buckets"]]


AGGREGATE_REUSED_LABELS = {
    "size": 0,
    "query": {
        "bool": {"filter":[{
            "term": {
                "authority": "lc-names"
            }},
            {"prefix": {
                "identifier": "n"
            }}]}
    },
    "aggs": {
        "common-labels": {
            "terms": {
                "size": 20000,
                "field": "label",
                "min_doc_count": 2
            },
            "aggs": {
                "ids":{
                    "terms":{
                        "size": 30,
                        "field": "identifier"
                    }
                }
            }

        }
    }
}

if __name__ == "__main__":
    main()