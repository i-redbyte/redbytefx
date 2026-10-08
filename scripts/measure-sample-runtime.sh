#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEVICE="${DEVICE:-emulator-5554}"
PACKAGE="ru.redbyte.redbytefx.sample"
ACTIVITY="${PACKAGE}/.MainActivity"
EXTRA_KEY="ru.redbyte.redbytefx.sample.extra.START_DEMO"
OUT_DIR="${ROOT_DIR}/build/device-measurements"
WARMUP_SECONDS="${WARMUP_SECONDS:-8}"
DEVICE_SLUG="$(printf '%s' "${DEVICE}" | tr '[:upper:]' '[:lower:]' | tr -c '[:alnum:]' '-')"
DEVICE_OUT_DIR="${OUT_DIR}/${DEVICE_SLUG}"

mkdir -p "${DEVICE_OUT_DIR}"

cd "${ROOT_DIR}"
if [[ "${SKIP_INSTALL:-0}" != "1" ]]; then
  ./gradlew :sample:installDebug >/dev/null
fi

if [[ $# -eq 0 ]]; then
  DEMOS=("Radar" "Circuit")
else
  DEMOS=("$@")
fi

echo "Device: ${DEVICE}"
echo "Warmup seconds: ${WARMUP_SECONDS}"
echo

measure_demo() {
  local demo="$1"
  local slug
  slug="$(printf '%s' "${demo}" | tr '[:upper:]' '[:lower:]')"
  local raw_out="${DEVICE_OUT_DIR}/${slug}-gfxinfo.txt"
  local summary_out="${DEVICE_OUT_DIR}/${slug}-summary.txt"
  local meminfo_out="${DEVICE_OUT_DIR}/${slug}-meminfo.txt"
  local meminfo_summary_out="${DEVICE_OUT_DIR}/${slug}-meminfo-summary.txt"

  adb -s "${DEVICE}" shell am force-stop "${PACKAGE}" >/dev/null
  adb -s "${DEVICE}" shell dumpsys gfxinfo "${PACKAGE}" reset >/dev/null
  adb -s "${DEVICE}" shell am start -W -n "${ACTIVITY}" --es "${EXTRA_KEY}" "${demo}" >/dev/null

  sleep "${WARMUP_SECONDS}"

  adb -s "${DEVICE}" shell dumpsys gfxinfo "${PACKAGE}" > "${raw_out}"
  adb -s "${DEVICE}" shell dumpsys meminfo "${PACKAGE}" > "${meminfo_out}"

  {
    printf 'Demo: %s\n' "${demo}"
    printf 'Device: %s\n' "${DEVICE}"
    printf 'Warmup seconds: %s\n' "${WARMUP_SECONDS}"
    awk '
      /^(Total frames rendered|Janky frames|50th percentile|90th percentile|95th percentile|99th percentile|50th gpu percentile|90th gpu percentile|95th gpu percentile|99th gpu percentile|Number Missed Vsync|Number High input latency|Number Slow UI thread|Number Slow bitmap uploads|Number Slow issue draw commands|Number Frame deadline missed)/ {
        print
        next
      }
      /^Total GPU memory usage:/ {
        if (getline gpu) {
          sub(/^[[:space:]]+/, "", gpu)
          print "Total GPU memory usage: " gpu
        } else {
          print
        }
      }
    ' "${raw_out}"
    if grep -q "Failure while dumping the app:" "${raw_out}"; then
      grep "Failure while dumping the app:" "${raw_out}"
    fi
    if grep -q "DUMP TIMEOUT" "${raw_out}"; then
      grep "DUMP TIMEOUT" "${raw_out}"
    fi
  } > "${summary_out}"
  grep -E "TOTAL PSS:|TOTAL RSS:|TOTAL SWAP PSS:|Java Heap:|Native Heap:|Code:|Stack:|Graphics:|Views:|ViewRootImpl:|Activities:" "${meminfo_out}" > "${meminfo_summary_out}" || true

  echo "== ${demo} =="
  if [[ -s "${summary_out}" ]]; then
    cat "${summary_out}"
  else
    echo "No summary lines matched; inspect ${raw_out}"
  fi
  if [[ -s "${meminfo_summary_out}" ]]; then
    cat "${meminfo_summary_out}"
  fi
  echo
}

for demo in "${DEMOS[@]}"; do
  measure_demo "${demo}"
done

echo "Saved raw reports under ${DEVICE_OUT_DIR}"
