#! /bin/sh
# define env GDRIVE_REFRESH_TOKEN, GDRIVE_DIR

COMMIT=$(echo $TRAVIS_COMMIT | sed 's!^\(.\{10\}\).*!\1!')

./gdrive upload --recursive --refresh-token $GDRIVE_REFRESH_TOKEN --parent $GDRIVE_DIR app/build
