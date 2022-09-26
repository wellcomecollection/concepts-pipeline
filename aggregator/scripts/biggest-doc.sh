HERE=`dirname "$0"`

sh $HERE/longest-line.sh | python $HERE/get_doc_id.py
