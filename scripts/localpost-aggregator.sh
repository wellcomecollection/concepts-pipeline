WORK_ID=$1
read -r -d '' SQS_MESSAGE << EOM
{
  "Records": [
    {
      "messageId": "19dd0b57-b21e-4ac1-bd88-01bbb068cb78",
      "receiptHandle": "MessageReceiptHandle",
      "body": "{\"Type\" : \"Notification\",\n  \"MessageId\" : \"53a96f4c-90bd-546a-9598-3484fad751d7\",\n  \"TopicArn\" : \"arn:aws:sns:eu-west-1:760097843905:catalogue-2022-10-14_ingestor_works_output\",\n  \"Subject\" : \"Sent from the ingestor-works\",\n  \"Message\" : \"$WORK_ID\",\n  \"Timestamp\" : \"2022-10-18T08:47:38.754Z\",\n  \"SignatureVersion\" : \"1\",\n  \"Signature\" : \"rPP0a4TRoVKNCKDALPI94wu8XV5mbNO8JGZWr6agBwz2iSy7hXZUqvRExSgrsgPPBQqCFmH8kBIt0YmUSM3x5xtCNN92A5mgTutc77ZH8B33O1ADLK9lw9OqhN75EEXOSJ+DF2hZe0a8y8U0GhIBF2s0NF5Q7PrExUsBA99qIEDNAigiJq5ZBlwotb0jIn/GZBjwf8oiMj4SsjRwqMxsrivnyFt9bafAm3uV3t4cROb++UvdQFf+s0DwrReQaHpnemluuG+pVEwI9LCI3RSpm1TJWjg70xSv4pbidAjj6HYKRL4uZw8832kS0lV3W3B7pxC1mESYy7V9iUJRfW59xw==\",\n  \"SigningCertURL\" : \"https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-56e67fcb41f6fec09b0196692625d385.pem\",\n  \"UnsubscribeURL\" : \"https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:760097843905:catalogue-2022-10-14_ingestor_works_output:c91b41b3-a11c-4e01-9d59-d70726a76196\"}",
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