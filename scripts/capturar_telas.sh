#!/usr/bin/env bash
# Roda dentro do emulador do workflow "Fotos das telas".
set -e
PKG=com.example.clinicavidaplus
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell am instrument -w -e class "$PKG.CapturaTelasTest" -e senha "$SENHA_TESTE" \
  "$PKG.test/androidx.test.runner.AndroidJUnitRunner" | tee instrument.log
adb logcat -d -s CapturaTelas Videochamada JitsiMeetSDK ReactNativeJS AndroidRuntime > captura.log || true
rm -rf docs/telas && mkdir -p docs/telas
adb exec-out run-as "$PKG" tar -cf - -C files telas | tar -xf - -C docs
ls -la docs/telas
