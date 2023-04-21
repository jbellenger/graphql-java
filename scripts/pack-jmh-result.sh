#!/bin/sh

set -e

RESULTS_FILE=$1

if [ ! -e "$RESULTS_FILE" -o ! -f "$RESULTS_FILE" ]; then
  echo "Could not load results file. Usage: $0 <path-to-jmh-results.json>"
  exit 1
fi

OUTPUT=$(dirname "$RESULTS_FILE")/row.json

JSON=$(cat $RESULTS_FILE | jq -c '.[0]')
GITSHA=$(git rev-parse HEAD)
STAMP=$(date -u +"%Y-%m-%d %H:%M:%S UTC")


DOC=$(cat <<EOT
{
  "gitsha": "${GITSHA}",
  "jmh_json": ${JSON},
  "stamp": "${STAMP}"
}
EOT
)

echo $DOC | jq -c > $OUTPUT
echo $OUTPUT
