#! /bin/sh
# define env GDRIVE_REFRESH_TOKEN, GDRIVE_DIR

COMMIT=$(echo $TRAVIS_COMMIT | sed 's!^\(.\{10\}\).*!\1!')

#./gdrive delete -r app-release.apk reports || true
ARCHIVE=app/build
./gdrive upload --refresh-token $GDRIVE_REFRESH_TOKEN --parent $GDRIVE_DIR "$ARCHIVE"
