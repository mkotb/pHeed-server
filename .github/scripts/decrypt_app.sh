#!/bin/sh

# --batch to prevent interactive command --yes to assume "yes" for questions
gpg --quiet --batch --yes --decrypt --passphrase="$APP_PASS" \
--output src/main/appengine/app.yaml src/main/appengine/app.yaml.gpg