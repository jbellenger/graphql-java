#!/bin/sh

set -e

RESULTS_FILE=$1

if [ ! -e "$RESULTS_FILE" -o ! -f "$RESULTS_FILE" ]; then
  echo "Could not load results file. Usage: $0 <path-to-jmh-results.json>"
  exit 1
fi

OUTPUT=$(dirname "$RESULTS_FILE")/row.json

JSON=$(cat $RESULTS_FILE | jq -c '.[0]')
GIT_SHA=$(git rev-parse HEAD)
GIT_BRANCH=$(git branch --show-current)
STAMP=$(date -u +"%Y-%m-%d %H:%M:%S UTC")


DOC=$(cat <<EOT
{
  "gitsha": "${GIT_SHA}",
  "branch": "${GIT_BRANCH}",
  "jmh_json": ${JSON},
  "stamp": "${STAMP}"
}
EOT
)

echo $DOC | jq -c > $OUTPUT
echo $OUTPUT
