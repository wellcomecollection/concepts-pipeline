import json
import sys

print(json.loads(sys.stdin.read())["id"])
