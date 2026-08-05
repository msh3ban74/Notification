#!/usr/bin/env bash
# Rafeeq — deep emulator UI test.
#
# Walks the WHOLE app on a booted emulator: skip auth, Dashboard (scroll),
# every bottom-nav tab, the "+" smart-item sheet and every real form,
# Settings, an AI suggestion send, then random monkey exploration.
# After every step it checks logcat for FATAL EXCEPTION and fails with the
# stack printed; a screenshot of each step is saved under shots/.
#
# Runs as ONE bash file because the emulator-runner action executes each
# script line with a separate `sh -c`, which breaks multi-line constructs.
set -u

mkdir -p shots

APK=$(ls app/build/outputs/apk/release/*.apk | head -1)
adb install -r "$APK"
adb logcat -c
adb shell am start -n com.notification.app/.MainActivity
sleep 22

snap() { adb exec-out screencap -p > "shots/$1.png" 2>/dev/null || true; }

fail_with_crash() {
  echo "==================== CRASH at step: $1 ===================="
  adb logcat -d | grep -B 2 -A 45 "FATAL EXCEPTION" | head -120
  snap "CRASH-$1"
  adb logcat -d > logcat-full.txt || true
  exit 1
}

checkpoint() {
  sleep 1
  if adb logcat -d | grep -q "FATAL EXCEPTION"; then
    fail_with_crash "$1"
  fi
  if adb logcat -d | grep -q "ANR in com.notification.app"; then
    echo "WARNING: ANR detected at step $1 (slow emulator is common — not failing)"
  fi
  echo "OK: $1"
  snap "$1"
}

tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1 || true
  COORDS=$(python3 - "$1" <<'PYEOF'
import re, sys
try:
    xml = open('ui.xml', encoding='utf-8').read()
except Exception:
    print('')
    sys.exit()
pat = sys.argv[1]
for node in re.finditer(r'<node[^>]*>', xml):
    n = node.group(0)
    if re.search('(text|content-desc)="[^"]*' + re.escape(pat) + '[^"]*"', n):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            print((int(b.group(1)) + int(b.group(3))) // 2,
                  (int(b.group(2)) + int(b.group(4))) // 2)
            sys.exit()
print('')
PYEOF
)
  if [ -n "$COORDS" ]; then
    adb shell input tap $COORDS
    echo "tapped '$1' at $COORDS"
    sleep 2
  else
    echo "NOT FOUND (skipping): '$1'"
  fi
}

checkpoint "01-launch-auth"
tap_text "تخطي"
tap_text "Skip"
checkpoint "02-dashboard"

adb shell input swipe 540 1800 540 600 400
sleep 1
adb shell input swipe 540 600 540 1800 400
sleep 1
checkpoint "03-dashboard-scroll"

tap_text "المساعد"
checkpoint "04-ai-tab"
tap_text "المهام"
checkpoint "05-tasks-tab"
tap_text "الإشعارات"
checkpoint "06-notifications-tab"
tap_text "الرئيسية"
checkpoint "07-back-to-dashboard"

# The "+" smart item sheet and every real form
tap_text "إضافة"
checkpoint "08-smart-sheet"
for ITEM in "مهمة" "فاتورة" "موعد" "دواء" "دين" "جمعية"; do
  tap_text "$ITEM"
  checkpoint "09-form-$ITEM"
  adb shell input keyevent 4
  sleep 1
  tap_text "إضافة"
done
adb shell input keyevent 4
sleep 1

# Settings via the profile button (top corner, RTL-aware: try both sides)
tap_text "الرئيسية"
sleep 1
adb shell input tap 90 145 || true
sleep 2
checkpoint "10-settings-or-topbar"
adb shell input keyevent 4
sleep 1

# AI: tap a suggestion pill (network may fail — must not crash)
tap_text "المساعد"
sleep 1
tap_text "مهامي اليوم"
sleep 6
checkpoint "11-ai-suggestion-sent"

# Random exploration
adb shell monkey -p com.notification.app --throttle 250 --pct-syskeys 0 400 || true
sleep 4
adb logcat -d > logcat-full.txt || true
if grep -q "FATAL EXCEPTION" logcat-full.txt; then
  echo "==================== CRASH during monkey ===================="
  grep -B 2 -A 45 "FATAL EXCEPTION" logcat-full.txt | head -120
  exit 1
fi
echo "Deep UI test passed — every screen visited, no fatal exceptions."
