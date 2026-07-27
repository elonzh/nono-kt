#!/usr/bin/env bash
set -euo pipefail

nono_version="${NONO_VERSION:-0.69.0}"
repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
install_directory="${NONO_INSTALL_DIR:-${repository_root}/build/nono/bin}"
temporary_directory="$(mktemp -d)"
trap 'rm -rf "${temporary_directory}"' EXIT

case "$(uname -s):$(uname -m)" in
  Darwin:arm64)
    target="aarch64-apple-darwin"
    ;;
  Darwin:x86_64)
    target="x86_64-apple-darwin"
    ;;
  Linux:aarch64 | Linux:arm64)
    target="aarch64-unknown-linux-gnu"
    ;;
  Linux:x86_64)
    target="x86_64-unknown-linux-gnu"
    ;;
  *)
    echo "Unsupported host: $(uname -s) $(uname -m)" >&2
    exit 1
    ;;
esac

archive="nono-v${nono_version}-${target}.tar.gz"
release_base="https://github.com/nolabs-ai/nono/releases/download/v${nono_version}"

curl --http1.1 --fail --location --silent --show-error \
  --retry 5 \
  --retry-all-errors \
  "${release_base}/${archive}" \
  --output "${temporary_directory}/${archive}"
curl --http1.1 --fail --location --silent --show-error \
  --retry 5 \
  --retry-all-errors \
  "${release_base}/SHA256SUMS.txt" \
  --output "${temporary_directory}/SHA256SUMS.txt"

expected_checksum="$(
  awk -v archive="${archive}" '$2 == archive { print $1 }' \
    "${temporary_directory}/SHA256SUMS.txt"
)"
test -n "${expected_checksum}"
actual_checksum="$(
  shasum -a 256 "${temporary_directory}/${archive}" | awk '{ print $1 }'
)"
test "${actual_checksum}" = "${expected_checksum}"

tar -xzf "${temporary_directory}/${archive}" -C "${temporary_directory}"
mkdir -p "${install_directory}"
install -m 0755 "${temporary_directory}/nono" "${install_directory}/nono"
"${install_directory}/nono" --version
