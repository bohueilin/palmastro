#!/bin/sh
# Runs the PalmAstroKit test suite.
#
# With full Xcode installed a plain `swift test` works. On Command Line
# Tools-only machines, Swift Testing (Testing.framework) lives outside the
# default search paths, so this script passes the framework and rpath flags
# explicitly. Extra arguments are forwarded to `swift test`
# (e.g. ./test.sh --filter AstroEngineTests).
set -eu
cd "$(dirname "$0")"

CLT_FRAMEWORKS="/Library/Developer/CommandLineTools/Library/Developer/Frameworks"
CLT_TESTING_LIB="/Library/Developer/CommandLineTools/Library/Developer/usr/lib"

if [ -d "$CLT_FRAMEWORKS/Testing.framework" ] && ! xcode-select -p 2>/dev/null | grep -qv CommandLineTools; then
    exec swift test \
        -Xswiftc -F -Xswiftc "$CLT_FRAMEWORKS" \
        -Xlinker -rpath -Xlinker "$CLT_FRAMEWORKS" \
        -Xlinker -rpath -Xlinker "$CLT_TESTING_LIB" \
        "$@"
fi

exec swift test "$@"
