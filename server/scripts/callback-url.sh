#!/bin/sh

# This script prints the callback URL for the provider given as argument, e.g. github
if command -v gp > /dev/null
then
    echo "`gp url 8080 | sed s/https:/http:/`/login/oauth2/code/$1"
else
    echo "http://localhost:8080/login/oauth2/code/$1"
fi
