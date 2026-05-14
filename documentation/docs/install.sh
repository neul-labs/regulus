#!/usr/bin/env sh
#
# Regulus CLI installer.
#
# Usage:
#   curl -fsSL https://regulus.neullabs.com/install.sh | sh
#
# What it does:
#   - downloads the latest regulus-cli.jar to ~/.regulus/bin/
#   - drops a shell shim at ~/.regulus/bin/regulus
#   - prints the PATH line to add to your shell profile
#

set -eu

INSTALL_DIR="${REGULUS_INSTALL_DIR:-$HOME/.regulus/bin}"
RELEASES_API="https://api.github.com/repos/neul-labs/regulus/releases/latest"

red()   { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
blue()  { printf '\033[34m%s\033[0m\n' "$*"; }

require() {
    if ! command -v "$1" >/dev/null 2>&1; then
        red "regulus install: missing required command: $1"
        exit 1
    fi
}

require curl
require java

mkdir -p "$INSTALL_DIR"

blue "regulus install: resolving latest release..."
ASSET_URL=$(curl -fsSL "$RELEASES_API" \
    | grep -E '"browser_download_url".*regulus-cli.*\.jar"' \
    | head -n 1 \
    | sed -E 's/.*"browser_download_url": "([^"]+)".*/\1/')

if [ -z "$ASSET_URL" ]; then
    red "regulus install: could not find regulus-cli jar in latest release."
    red "  Visit https://github.com/neul-labs/regulus/releases for manual install."
    exit 1
fi

blue "regulus install: downloading $ASSET_URL"
curl -fsSL -o "$INSTALL_DIR/regulus-cli.jar" "$ASSET_URL"

cat > "$INSTALL_DIR/regulus" <<'WRAPPER'
#!/usr/bin/env sh
exec java -jar "$(dirname "$0")/regulus-cli.jar" "$@"
WRAPPER
chmod +x "$INSTALL_DIR/regulus"

green "regulus install: installed to $INSTALL_DIR"
echo
case ":$PATH:" in
    *":$INSTALL_DIR:"*) green "PATH already includes $INSTALL_DIR — you're set." ;;
    *)
        blue "Add this to your shell profile (~/.zshrc, ~/.bashrc, etc.):"
        echo
        echo "    export PATH=\"$INSTALL_DIR:\$PATH\""
        echo
        ;;
esac

echo "Verify:"
echo "    $INSTALL_DIR/regulus --version"
echo
echo "Docs: https://regulus.neullabs.com"
