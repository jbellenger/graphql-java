#!/bin/sh

set -e

SUITE=$1
RESULTS_FILE=$2

printUsageAndExit() {
  echo "Usage: $0 <suite-name> <path-to-jmh-results>"
  exit 1
}

if [ -z "${COMPONENT}" ]; then
  printUsageAndExit
fi

if [ ! -e "$RESULTS_FILE" -o ! -f "$RESULTS_FILE" ]; then
  printUsageAndExit
fi

OUTPUT=$(mktemp -t jmh-result-rows)

ID=$(uuidgen)
JSON=$(cat $RESULTS_FILE | jq -c '.[0]')
GIT_SHA=$(git rev-parse HEAD)
GIT_BRANCH=$(git branch --show-current)
STAMP=$(date -u +"%Y-%m-%d %H:%M:%S UTC")

# BigQuery expects json data to be objects
# This is a challenge when working with jmh result data, which formats results
# as an array of individual benchmark results.
# We can work around this by inserting a row for each benchmark result in that
# array.

JQ_RUN_TEMPLATE=$(cat <<EOT
  {
    id: "${ID}", 
    suite: "${SUITE}",
    git_sha: "${GIT_SHA}", 
    git_branch: "${GIT_BRANCH}", 
    stamp: "${STAMP}", 
    jmh_json: .
  }
EOT
)

# For each row in the jmh result array, format it with some metadata from its
# test run and emit it as a line in a file.
cat $RESULTS_FILE | jq -c ".[] | ${JQ_RUN_TEMPLATE}" > $OUTPUT

echo $OUTPUT
