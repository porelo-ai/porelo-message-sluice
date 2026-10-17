#!/usr/bin/env bash
# Installs the filter where Porelo looks for it, for this user.
#
# Shell rather than Java: it copies one native executable, and nothing about that needs a program.
# Build first, on a GraalVM:  JAVA_HOME=<graalvm> ./mvnw -B -s settings.xml -Pnative package
#
#   ./install-local.sh              install, or replace an earlier install
#   ./install-local.sh --uninstall  remove exactly what an install put there
#
# What it writes, and nothing else:
#   $XDG_DATA_HOME/porelo/filter/porelo-message-sluice-filter
set -euo pipefail

DATA="${XDG_DATA_HOME:-$HOME/.local/share}"
FILTER="$DATA/porelo/filter/porelo-message-sluice-filter"
REPO="$(cd "$(dirname "$0")" && pwd)"

if [ "${1:-}" = "--uninstall" ]; then
    rm -f "$FILTER"
    echo "removed $FILTER"
    exit 0
fi

install_binary() { # built target
    [ -x "$1" ] || {
        echo "$1 is missing; build it with -Pnative on a GraalVM first" >&2
        exit 2
    }
    mkdir -p "$(dirname "$2")"
    # Copied beside the target and renamed, so Porelo never executes half a binary. Porelo looks for
    # programs in that directory, so the copy in progress has a name starting with a dot, is not
    # executable until it is complete, and is removed if the copy fails.
    partial="$(dirname "$2")/.$(basename "$2").partial"
    cat "$1" >"$partial"
    chmod 755 "$partial"
    mv "$partial" "$2"
    partial=
    echo "$2"
}

partial=
trap '[ -z "$partial" ] || rm -f "$partial"' EXIT
trap 'exit 129' HUP; trap 'exit 130' INT; trap 'exit 143' TERM

install_binary "$REPO/filter/app/target/porelo-message-sluice-filter" "$FILTER"
