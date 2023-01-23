#####################################################################
# Given a work id, run that work id through the local aggregator
# that is already running in docker compose
#####################################################################

WORK_ID=${1:?"Missing Work id"}

read -r -d '' SQS_MESSAGE << EOM
{
  "Records": [
    {
      "messageId": "19dd0b57-b21e-4ac1-bd88-01bbb068cb78",
      "receiptHandle": "MessageReceiptHandle",
      "body": "{\"Type\" : \"Notification\",\n  \"MessageId\" : \"53a96f4c-90bd-546a-9598-3484fad751d7\",\n  \"TopicArn\" : \"arn:aws:sns:eu-west-1:my-topic\",\n  \"Subject\" : \"Sent from the ingestor-works\",\n  \"Message\" : \"$WORK_ID\",\n  \"Timestamp\" : \"2022-10-18T08:47:38.754Z\",\n  \"SignatureVersion\" : \"1\",\n  \"Signature\" : \"blahblahblah\",\n  \"SigningCertURL\" : \"https://example.com\",\n  \"UnsubscribeURL\" : \"https://example.com\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1523232000000",
        "SenderId": "123456789012",
        "ApproximateFirstReceiveTimestamp": "1523232000001"
      },
      "messageAttributes": {},
      "md5OfBody": "{{{md5_of_body}}}",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:MyQueue",
      "awsRegion": "us-east-1"
    }
  ]
}
EOM
echo $SQS_MESSAGE | curl -XPOST "http://localhost:9000/2015-03-31/functions/function/invocations" -d @-
