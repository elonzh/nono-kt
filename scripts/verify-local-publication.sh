#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
local_repository="${repository_root}/build/local-maven"
version="$("${repository_root}/gradlew" -q printVersion --no-daemon)"
artifact_directory="${local_repository}/com/elonzh/nono/nono-kt/${version}"

"${repository_root}/gradlew" \
  publishToMavenLocal \
  "-Dmaven.repo.local=${local_repository}" \
  --no-daemon

required_artifacts=(
  "nono-kt-${version}.jar"
  "nono-kt-${version}-sources.jar"
  "nono-kt-${version}-javadoc.jar"
  "nono-kt-${version}.pom"
  "nono-kt-${version}.module"
)

for artifact in "${required_artifacts[@]}"; do
  test -f "${artifact_directory}/${artifact}"
done

"${repository_root}/gradlew" \
  -p "${repository_root}/publication-test" \
  clean \
  compileJava \
  "-PlocalMavenRepository=${local_repository}" \
  "-PnonoVersion=${version}" \
  --no-daemon
