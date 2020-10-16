#!/bin/sh

# This script must be run from the 'server' directory
# Start an HTTP server that shows the test report on port 8081
curl lama.sh | sh -s -- -p 8081 -d build/reports/tests/
