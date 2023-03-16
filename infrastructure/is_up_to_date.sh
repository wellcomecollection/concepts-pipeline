ROOT=$(git rev-parse --show-toplevel)

if [ "$ROOT/aggregator/src/main/resources/append-fields.json" -nt "$ROOT/infrastructure/stack/append_fields.painless" ]
then
    echo "append-fields.json is newer than append_fields.painless"
    echo "copy the painless script from the json file into the painless file"
    exit 1
fi
