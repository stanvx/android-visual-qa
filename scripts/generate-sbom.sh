#!/usr/bin/env bash
# =============================================================================
# generate-sbom.sh — Software Bill of Materials (CycloneDX 1.5)
# =============================================================================
# Produces an SBOM JSON file at build/sbom/sbom.cdx.json by parsing the
# Gradle dependency tree for the release runtime classpath.
#
# Why not the cyclonedx-gradle-plugin?
#   The manual script approach is more reliable across CI environments and
#   avoids requiring a Gradle plugin.  It parses `./gradlew :app:dependencies
#   --configuration releaseRuntimeClasspath` output.
#
# Usage:
#   bash scripts/generate-sbom.sh
#
# Output:
#   build/sbom/sbom.cdx.json  — CycloneDX 1.5 compliant SBOM
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_SBOM_DIR="$PROJECT_DIR/build/sbom"
DEPS_FILE="/tmp/android-visual-qa-deps.txt"
SBOM_FILE="$BUILD_SBOM_DIR/sbom.cdx.json"

echo "→ Generating SBOM for Android Visual QA (CycloneDX 1.5)"
echo ""

# ---------------------------------------------------------------------------
# Step 1 — Dump the release runtime classpath dependency tree
# ---------------------------------------------------------------------------
echo "→ Dumping release runtime classpath..."
"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" :app:dependencies --configuration releaseRuntimeClasspath --no-daemon 2>/dev/null \
    | sed 's/\x1b\[[0-9;]*m//g' \
    > "$DEPS_FILE"
echo "   Wrote $(wc -l < "$DEPS_FILE") lines to $DEPS_FILE"
echo ""

# ---------------------------------------------------------------------------
# Step 2 — Parse dependency coordinates using Python3
# ---------------------------------------------------------------------------
# Python3 is available on macOS and Ubuntu CI runners.  We use it for
# reliable regex and dict deduplication rather than fighting awk/sed
# differences between platforms.
echo "→ Parsing dependency coordinates..."

python3 - "$DEPS_FILE" "$SBOM_FILE" <<'PYEOF'
import json
import os
import re
import sys
from datetime import datetime, timezone
from uuid import uuid4

deps_file = sys.argv[1]
sbom_file = sys.argv[2]

# Read deps, skip ANSI codes
with open(deps_file) as f:
    lines = f.read().splitlines()

# Pattern: group:artifact:version
# Tree lines look like:
#   +--- androidx.core:core-ktx:1.13.1
#   |    +--- androidx.annotation:annotation:1.8.0 -> 1.9.0 (c)
# We strip tree prefix chars, resolve "->", strip markers, then extract.
deps: dict[str, str] = {}
line_pattern = re.compile(r'^[+|\\\s-]+\s*')
coord_pattern = re.compile(
    r'^([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+):([a-zA-Z0-9._()-]+)$'
)

for line in lines:
    # Skip lines without dependency tree branches
    if '---' not in line:
        continue

    # Strip ANSI codes and tree prefix
    clean = line_pattern.sub('', line)

    # Skip local module references: "project :xxx"
    if clean.startswith('project '):
        continue

    # Resolve version: "foo:bar:1.0 -> 2.0" -> "foo:bar:2.0"
    clean = re.sub(r'\s*->\s*', ':', clean)

    # Strip trailing markers: (c), (n), (*)
    clean = re.sub(r'\s+\(.*?\)', '', clean)
    clean = re.sub(r'\s+\*$', '', clean)

    # Match the coordinate
    m = coord_pattern.match(clean)
    if m:
        g, a, v = m.group(1), m.group(2), m.group(3)
        key = f"{g}:{a}"
        # Keep the resolved version (last occurrence wins)
        deps[key] = f"{g}:{a}:{v}"

# Generate SBOM
bom_serial = f"urn:uuid:{uuid4()}"
created = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')

components = []
for key in sorted(deps.keys()):
    g, a, v = deps[key].split(':', 2)
    purl = f"pkg:maven/{g}/{a}@{v}"
    components.append({
        "type": "library",
        "name": f"{g}:{a}",
        "version": v,
        "purl": purl,
    })

sbom = {
    "$schema": "http://cyclonedx.org/schema/bom-1.5.schema.json",
    "bomFormat": "CycloneDX",
    "specVersion": "1.5",
    "serialNumber": bom_serial,
    "version": 1,
    "metadata": {
        "timestamp": created,
        "tools": [
            {
                "vendor": "Android Visual QA",
                "name": "generate-sbom.sh",
                "version": "1.0.0",
            }
        ],
        "component": {
            "type": "application",
            "name": "android-visual-qa",
            "version": "0.1.0",
            "purl": f"pkg:github/{os.environ.get('GITHUB_REPOSITORY', 'stanvx/android-visual-qa')}@0.1.0",
        },
    },
    "components": components,
}

os.makedirs(os.path.dirname(sbom_file), exist_ok=True)
with open(sbom_file, 'w') as f:
    json.dump(sbom, f, indent=2)

print(f"   Found {len(components)} unique dependencies")
print(f"   Wrote {os.path.getsize(sbom_file)} bytes to {sbom_file}")
PYEOF

echo ""
echo "✓ SBOM written: $SBOM_FILE"

# ---------------------------------------------------------------------------
# Step 3 — Validate the SBOM is valid JSON
# ---------------------------------------------------------------------------
python3 -m json.tool "$SBOM_FILE" > /dev/null && echo "✓ SBOM JSON is valid"

# ---------------------------------------------------------------------------
# Cleanup — remove temporary dependency dump
# ---------------------------------------------------------------------------
rm -f "$DEPS_FILE"

echo ""
echo "Done."
