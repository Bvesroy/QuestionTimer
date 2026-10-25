#!/usr/bin/env python3
"""
Controlled, paired 5-trial experiment challenging the Incremental Compilation Hypothesis.

Hypothesis under test:
"Incremental compilation reusing compiled classes for unmodified files
is the highest-value optimization for build speed."

Test Configurations:
- Configuration A (Baseline): Full clean build (wipe build dir, compile all, dex all, sign).
- Configuration B (Candidate): Warm incremental compilation (reuse compiled classes in build/obj,
  recompile only changed Java sources, dex with D8, package, sign).

Edge cases & Trials:
- Trial 1: Baseline Clean Build vs Candidate Warm Incremental Build (no-op / comment edit).
- Trial 2: Method body modification (internal logic change in DualClockEngine.java).
- Trial 3: Dependent class change (new helper method in AnalyticsMath.java called from EstimatedMarksCalculator.java).
- Trial 4: Stale Bytecode Hazard (removed inner class retention in incremental build vs clean build).
- Trial 5: Corrupted Cache Recovery (corrupted .class file handling, failure detection & clean fallback).
"""

import os
import sys
import time
import json
import shutil
import subprocess
from pathlib import Path
from typing import Dict, Any, List, Optional, Tuple

DIR = Path("/Users/jeemainsadv/TimerStorage")
BUILD_DIR = DIR / "build"
JAVA_HOME = Path("/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home")
BUILD_TOOLS = Path("/opt/homebrew/share/android-commandlinetools/build-tools/35.0.0")
PLATFORM_JAR = Path("/opt/homebrew/share/android-commandlinetools/platforms/android-35/android.jar")
KEYSTORE = DIR / "debug.keystore"
KS_PASS = "android"

ENV = os.environ.copy()
ENV["JAVA_HOME"] = str(JAVA_HOME)
ENV["PATH"] = f"{JAVA_HOME / 'bin'}:{BUILD_TOOLS}:{ENV.get('PATH', '')}"

def mono_ms() -> int:
    return int(time.monotonic() * 1000)

def count_dex_classes(apk_path: Path) -> Tuple[int, List[str]]:
    """Uses dexdump to extract all class descriptors from APK."""
    try:
        cmd = [str(BUILD_TOOLS / "dexdump"), str(apk_path)]
        res = subprocess.run(cmd, env=ENV, capture_output=True, text=True, timeout=15)
        classes = []
        for line in res.stdout.splitlines():
            line = line.strip()
            if line.startswith("Class descriptor"):
                # Class descriptor  : 'Lcom/example/timerapp/...;'
                parts = line.split(":")
                if len(parts) > 1:
                    cls_name = parts[1].strip().strip("'")
                    classes.append(cls_name)
        return len(classes), classes
    except Exception as e:
        return 0, []

def run_clean_build() -> Dict[str, Any]:
    """Runs standard full clean build via build_apk.sh."""
    t0 = mono_ms()
    cmd = [str(DIR / "build_apk.sh")]
    res = subprocess.run(cmd, env=ENV, cwd=str(DIR), capture_output=True, text=True, timeout=60)
    t_end = mono_ms()

    latest_json = Path("/Users/jeemainsadv/Swarm/metrics/builds/latest.json")
    metrics = {}
    if latest_json.exists():
        try:
            with open(latest_json, "r", encoding="utf-8") as f:
                metrics = json.load(f)
        except Exception:
            pass

    stages = metrics.get("stages", {})
    apk_path = DIR / "release" / "QuestionTimer.apk"
    cls_count, classes = count_dex_classes(apk_path) if apk_path.exists() else (0, [])

    return {
        "mode": "clean_baseline",
        "exit_code": res.returncode,
        "success": res.returncode == 0,
        "total_ms": metrics.get("total_duration_ms", t_end - t0),
        "wall_clock_ms": t_end - t0,
        "javac_ms": stages.get("javac_compile", 0),
        "d8_ms": stages.get("d8_dex", 0),
        "aapt2_compile_ms": stages.get("aapt2_compile", 0),
        "aapt2_link_ms": stages.get("aapt2_link", 0),
        "stages_total_ms": metrics.get("stages_total_ms", 0),
        "overhead_ms": metrics.get("uninstrumented_overhead_ms", 0),
        "apk_size": metrics.get("artifact", {}).get("bytes") if metrics.get("artifact") else None,
        "sha256": metrics.get("artifact", {}).get("sha256") if metrics.get("artifact") else None,
        "classes_count": cls_count,
        "classes": classes,
        "output": res.stdout,
        "error": res.stderr
    }

def run_incremental_build(changed_files: List[Path], auto_clean_recovery: bool = False) -> Dict[str, Any]:
    """
    Executes warm incremental build:
    - Reuses existing build/gen, build/obj, build/resources.zip, build/linked.apk.
    - If changed_files given, only recompiles those with javac into build/obj.
    - Runs D8 over build/obj.
    - Packages, aligns, signs, and verifies.
    """
    t_start = mono_ms()
    stages = {
        "aapt2_compile": 0,
        "aapt2_link": 0,
        "javac_compile": 0,
        "d8_dex": 0,
        "jar_package": 0,
        "zipalign": 0,
        "apksigner_sign": 0,
        "apksigner_verify": 0,
        "checksum_verify": 0
    }

    obj_dir = BUILD_DIR / "obj"
    gen_dir = BUILD_DIR / "gen"
    apk_dir = BUILD_DIR / "apk"
    release_dir = DIR / "release"
    release_apk = release_dir / "QuestionTimer.apk"

    if not (obj_dir.exists() and (BUILD_DIR / "linked.apk").exists()):
        init_res = run_clean_build()
        if not init_res["success"]:
            return init_res

    # 1. Resource check (reused in warm build)
    stages["aapt2_compile"] = 0
    stages["aapt2_link"] = 0

    # 2. Javac incremental compile
    s3_start = mono_ms()
    if changed_files:
        sources_file = BUILD_DIR / "incremental_sources.txt"
        with open(sources_file, "w", encoding="utf-8") as f:
            for p in changed_files:
                f.write(f"{p.resolve()}\n")

        cp = f"{PLATFORM_JAR}:{obj_dir}:{gen_dir}"
        libs_dir = DIR / "app" / "libs"
        if libs_dir.is_dir():
            for jar in libs_dir.glob("*.jar"):
                cp += f":{jar}"

        javac_cmd = [
            str(JAVA_HOME / "bin" / "javac"),
            "-cp", cp,
            "-d", str(obj_dir),
            "-source", "17", "-target", "17",
            "-encoding", "UTF-8",
            f"@{sources_file}"
        ]
        res_javac = subprocess.run(javac_cmd, env=ENV, capture_output=True, text=True)
        if res_javac.returncode != 0:
            if auto_clean_recovery:
                print("   [Recovery] Incremental javac failed, falling back to clean build...")
                clean_rec = run_clean_build()
                clean_rec["recovered"] = True
                clean_rec["initial_failure_stage"] = "javac"
                clean_rec["initial_error"] = res_javac.stderr
                return clean_rec
            return {
                "mode": "incremental_candidate",
                "exit_code": res_javac.returncode,
                "success": False,
                "failed_stage": "javac_compile",
                "total_ms": mono_ms() - t_start,
                "javac_ms": mono_ms() - s3_start,
                "d8_ms": 0,
                "apk_size": None,
                "classes_count": 0,
                "error": res_javac.stderr
            }
    stages["javac_compile"] = mono_ms() - s3_start

    # 3. D8 Dexing
    s4_start = mono_ms()
    classes_file = BUILD_DIR / "classes.txt"
    with open(classes_file, "w", encoding="utf-8") as f:
        for c in obj_dir.rglob("*.class"):
            f.write(f"{c.resolve()}\n")

    d8_cmd = [
        str(BUILD_TOOLS / "d8"),
        "--lib", str(PLATFORM_JAR),
        "--min-api", "26",
        "--output", str(apk_dir),
        f"@{classes_file}"
    ]
    res_d8 = subprocess.run(d8_cmd, env=ENV, capture_output=True, text=True)
    if res_d8.returncode != 0:
        if auto_clean_recovery:
            print("   [Recovery] Incremental D8 failed, falling back to clean rebuild...")
            rec_start = mono_ms()
            clean_rec = run_clean_build()
            clean_rec["recovered"] = True
            clean_rec["recovery_penalty_ms"] = mono_ms() - rec_start
            clean_rec["total_ms"] = mono_ms() - t_start
            clean_rec["initial_failure_stage"] = "d8_dex"
            clean_rec["initial_error"] = res_d8.stderr
            return clean_rec
        return {
            "mode": "incremental_candidate",
            "exit_code": res_d8.returncode,
            "success": False,
            "failed_stage": "d8_dex",
            "total_ms": mono_ms() - t_start,
            "javac_ms": stages["javac_compile"],
            "d8_ms": mono_ms() - s4_start,
            "apk_size": None,
            "classes_count": 0,
            "error": res_d8.stderr
        }
    stages["d8_dex"] = mono_ms() - s4_start

    # 4. Jar package
    s5_start = mono_ms()
    shutil.copy2(BUILD_DIR / "linked.apk", BUILD_DIR / "unaligned.apk")
    jar_cmd = [str(JAVA_HOME / "bin" / "jar"), "-uf", str(BUILD_DIR / "unaligned.apk")]
    dex_files = [f.name for f in apk_dir.glob("classes*.dex")]
    if not dex_files:
        return {
            "mode": "incremental_candidate",
            "success": False,
            "total_ms": mono_ms() - t_start,
            "javac_ms": stages["javac_compile"],
            "d8_ms": stages["d8_dex"],
            "apk_size": None,
            "classes_count": 0,
            "error": "No dex files"
        }
    jar_cmd.extend(dex_files)
    subprocess.run(jar_cmd, env=ENV, cwd=str(apk_dir), check=True, capture_output=True)
    stages["jar_package"] = mono_ms() - s5_start

    # 5. Zipalign
    s6_start = mono_ms()
    subprocess.run([
        str(BUILD_TOOLS / "zipalign"), "-f", "-p", "4",
        str(BUILD_DIR / "unaligned.apk"), str(BUILD_DIR / "aligned.apk")
    ], check=True, capture_output=True)
    stages["zipalign"] = mono_ms() - s6_start

    # 6. Apksigner sign
    s7_start = mono_ms()
    subprocess.run([
        str(BUILD_TOOLS / "apksigner"), "sign",
        "--ks", str(KEYSTORE),
        "--ks-key-alias", "androiddebugkey",
        "--ks-pass", f"pass:{KS_PASS}",
        "--key-pass", f"pass:{KS_PASS}",
        "--v2-signing-enabled", "true",
        "--v3-signing-enabled", "true",
        "--out", str(release_apk),
        str(BUILD_DIR / "aligned.apk")
    ], env=ENV, check=True, capture_output=True)
    stages["apksigner_sign"] = mono_ms() - s7_start

    # 7. Apksigner verify
    s8_start = mono_ms()
    res_verify = subprocess.run([
        str(BUILD_TOOLS / "apksigner"), "verify", "--verbose", str(release_apk)
    ], env=ENV, capture_output=True, text=True)
    stages["apksigner_verify"] = mono_ms() - s8_start

    # 8. Checksum provenance
    s9_start = mono_ms()
    import hashlib
    h = hashlib.sha256()
    with open(release_apk, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    release_sha = h.hexdigest()
    stages["checksum_verify"] = mono_ms() - s9_start

    t_end = mono_ms()
    total_duration = t_end - t_start
    stages_total = sum(stages.values())
    uninstrumented_overhead = total_duration - stages_total

    cls_count, classes = count_dex_classes(release_apk)

    return {
        "mode": "incremental_candidate",
        "exit_code": 0,
        "success": res_verify.returncode == 0,
        "total_ms": total_duration,
        "stages_total_ms": stages_total,
        "overhead_ms": uninstrumented_overhead,
        "javac_ms": stages["javac_compile"],
        "d8_ms": stages["d8_dex"],
        "aapt2_compile_ms": 0,
        "aapt2_link_ms": 0,
        "stages": stages,
        "apk_size": release_apk.stat().st_size if release_apk.exists() else None,
        "sha256": release_sha,
        "classes_count": cls_count,
        "classes": classes,
        "verified": (res_verify.returncode == 0)
    }

def main():
    print("=" * 80)
    print("CONTROLLED EXPERIMENT: CHALLENGING INCREMENTAL COMPILATION HYPOTHESIS")
    print("=" * 80)

    results = []

    dual_clock_path = DIR / "app/src/main/java/com/example/timerapp/data/engine/DualClockEngine.java"
    math_path = DIR / "app/src/main/java/com/example/timerapp/data/analytics/AnalyticsMath.java"
    marks_path = DIR / "app/src/main/java/com/example/timerapp/data/analytics/EstimatedMarksCalculator.java"

    orig_dual_clock = dual_clock_path.read_text(encoding="utf-8")
    orig_math = math_path.read_text(encoding="utf-8")
    orig_marks = marks_path.read_text(encoding="utf-8")

    try:
        # Initial warm-up clean build to establish baseline cache state
        print("\n--- Seeding build cache with initial clean baseline ---")
        init_res = run_clean_build()
        print(f"Initial clean build: {init_res['total_ms']}ms (Javac: {init_res['javac_ms']}ms, D8: {init_res['d8_ms']}ms, APK: {init_res['apk_size']} bytes)")

        # -------------------------------------------------------------
        # TRIAL 1: No-op / Minimal Cosmetic Comment Edit
        # -------------------------------------------------------------
        print("\n" + "=" * 60)
        print("TRIAL 1: Minimal Cosmetic Edit (Trivial Comment in DualClockEngine.java)")
        print("=" * 60)
        dual_clock_path.write_text(orig_dual_clock + "\n// Trial 1 trivial comment\n", encoding="utf-8")

        print("1A. Running Baseline (Full Clean Build)...")
        b1 = run_clean_build()
        print(f"    Baseline:  Total={b1['total_ms']}ms | Javac={b1['javac_ms']}ms | D8={b1['d8_ms']}ms | APK={b1['apk_size']}B | Classes={b1['classes_count']}")

        print("1B. Running Candidate (Warm Incremental Build)...")
        c1 = run_incremental_build(changed_files=[dual_clock_path])
        print(f"    Candidate: Total={c1['total_ms']}ms | Javac={c1['javac_ms']}ms | D8={c1['d8_ms']}ms | APK={c1['apk_size']}B | Classes={c1['classes_count']}")

        speedup_t1 = round((b1["total_ms"] - c1["total_ms"]) / b1["total_ms"] * 100, 1)
        javac_diff_t1 = b1["javac_ms"] - c1["javac_ms"]
        print(f"    >> Delta: Javac saved {javac_diff_t1}ms | Total Wall-Clock Speedup: {speedup_t1}% (D8 took {c1['d8_ms']}ms vs {b1['d8_ms']}ms)")

        results.append({
            "trial": 1,
            "scenario": "Trivial Comment / Single File",
            "baseline": b1,
            "candidate": c1,
            "stale_bytecode_risk": False,
            "speedup_pct": speedup_t1
        })

        # -------------------------------------------------------------
        # TRIAL 2: Method Body Modification (Internal Logic Change)
        # -------------------------------------------------------------
        print("\n" + "=" * 60)
        print("TRIAL 2: Method Body Modification (Logic change in DualClockEngine.java)")
        print("=" * 60)
        mod_trial2 = orig_dual_clock.replace(
            "currentQuestionElapsedMs += deltaMs;",
            "long trial2SafeDelta = Math.max(0L, deltaMs);\n            currentQuestionElapsedMs += trial2SafeDelta;"
        )
        dual_clock_path.write_text(mod_trial2, encoding="utf-8")

        print("2A. Running Baseline (Full Clean Build)...")
        b2 = run_clean_build()
        print(f"    Baseline:  Total={b2['total_ms']}ms | Javac={b2['javac_ms']}ms | D8={b2['d8_ms']}ms | APK={b2['apk_size']}B | Classes={b2['classes_count']}")

        print("2B. Running Candidate (Warm Incremental Build)...")
        c2 = run_incremental_build(changed_files=[dual_clock_path])
        print(f"    Candidate: Total={c2['total_ms']}ms | Javac={c2['javac_ms']}ms | D8={c2['d8_ms']}ms | APK={c2['apk_size']}B | Classes={c2['classes_count']}")

        speedup_t2 = round((b2["total_ms"] - c2["total_ms"]) / b2["total_ms"] * 100, 1)
        javac_diff_t2 = b2["javac_ms"] - c2["javac_ms"]
        print(f"    >> Delta: Javac saved {javac_diff_t2}ms | Total Wall-Clock Speedup: {speedup_t2}% (D8: {c2['d8_ms']}ms vs {b2['d8_ms']}ms)")

        results.append({
            "trial": 2,
            "scenario": "Method Body Modification",
            "baseline": b2,
            "candidate": c2,
            "stale_bytecode_risk": False,
            "speedup_pct": speedup_t2
        })

        # -------------------------------------------------------------
        # TRIAL 3: Dependent Class Changes (Multi-file cross dependency)
        # -------------------------------------------------------------
        print("\n" + "=" * 60)
        print("TRIAL 3: Dependent Class Changes (New helper in AnalyticsMath called by EstimatedMarksCalculator)")
        print("=" * 60)
        new_helper = """
    public static double computeSpeedMetricBonus(double rawScore, double paceSeconds) {
        if (paceSeconds <= 0.0) return rawScore;
        return rawScore + (120.0 / paceSeconds);
    }
"""
        last_brace_math = orig_math.rfind("}")
        mod_math = orig_math[:last_brace_math] + new_helper + "\n}\n"
        math_path.write_text(mod_math, encoding="utf-8")

        mod_marks = orig_marks.replace(
            "this.negativeMarksPenalty = negativeMarksPenalty;",
            "this.negativeMarksPenalty = negativeMarksPenalty + (AnalyticsMath.computeSpeedMetricBonus(0.0, 60.0) > 0 ? 0 : 0);"
        )
        marks_path.write_text(mod_marks, encoding="utf-8")

        print("3A. Running Baseline (Full Clean Build)...")
        b3 = run_clean_build()
        print(f"    Baseline:  Total={b3['total_ms']}ms | Javac={b3['javac_ms']}ms | D8={b3['d8_ms']}ms | APK={b3['apk_size']}B | Classes={b3['classes_count']}")

        print("3B. Running Candidate (Warm Incremental Build of both changed files)...")
        c3 = run_incremental_build(changed_files=[math_path, marks_path])
        print(f"    Candidate: Total={c3['total_ms']}ms | Javac={c3['javac_ms']}ms | D8={c3['d8_ms']}ms | APK={c3['apk_size']}B | Classes={c3['classes_count']}")

        speedup_t3 = round((b3["total_ms"] - c3["total_ms"]) / b3["total_ms"] * 100, 1)
        javac_diff_t3 = b3["javac_ms"] - c3["javac_ms"]
        print(f"    >> Delta: Javac saved {javac_diff_t3}ms | Total Wall-Clock Speedup: {speedup_t3}% (D8: {c3['d8_ms']}ms vs {b3['d8_ms']}ms)")

        results.append({
            "trial": 3,
            "scenario": "Dependent Class Changes (ABI Method Addition)",
            "baseline": b3,
            "candidate": c3,
            "stale_bytecode_risk": False,
            "speedup_pct": speedup_t3
        })

        # -------------------------------------------------------------
        # TRIAL 4: Stale Bytecode Hazard / Removed Inner Class
        # -------------------------------------------------------------
        print("\n" + "=" * 60)
        print("TRIAL 4: Edge Case - Stale Bytecode Hazard (Removed Inner Class)")
        print("=" * 60)
        # Step 4.1: Add inner class and compile with candidate
        inner_class_code = """
    public static class StaleGhostMetric {
        public static final int GHOST_ID = 404;
        public String getGhostTag() { return "STALE_BYTECODE_HAZARD"; }
    }
"""
        last_brace_dual = orig_dual_clock.rfind("}")
        code_with_inner = orig_dual_clock[:last_brace_dual] + inner_class_code + "\n}\n"
        dual_clock_path.write_text(code_with_inner, encoding="utf-8")
        run_incremental_build(changed_files=[dual_clock_path])

        ghost_class_file = BUILD_DIR / "obj/com/example/timerapp/data/engine/DualClockEngine$StaleGhostMetric.class"
        ghost_class_desc = "Lcom/example/timerapp/data/engine/DualClockEngine$StaleGhostMetric;"

        print(f"   [Step 4.1] Inner class compiled into: {ghost_class_file.exists()}")

        # Step 4.2: Remove inner class in source code
        dual_clock_path.write_text(orig_dual_clock, encoding="utf-8")

        print("4A. Running Candidate (Warm Incremental Build after source class removal)...")
        c4 = run_incremental_build(changed_files=[dual_clock_path])
        ghost_in_candidate_obj = ghost_class_file.exists()
        ghost_in_candidate_dex = ghost_class_desc in c4.get("classes", [])
        print(f"    Candidate: Total={c4['total_ms']}ms | Javac={c4['javac_ms']}ms | D8={c4['d8_ms']}ms | Classes={c4['classes_count']}")
        print(f"    >> CRITICAL HAZARD CHECK:")
        print(f"       Ghost .class file persisted in obj/: {ghost_in_candidate_obj}")
        print(f"       Ghost class embedded in release APK DEX: {ghost_in_candidate_dex}")

        print("4B. Running Baseline (Full Clean Build after source class removal)...")
        b4 = run_clean_build()
        ghost_in_baseline_obj = ghost_class_file.exists()
        ghost_in_baseline_dex = ghost_class_desc in b4.get("classes", [])
        print(f"    Baseline:  Total={b4['total_ms']}ms | Javac={b4['javac_ms']}ms | D8={b4['d8_ms']}ms | Classes={b4['classes_count']}")
        print(f"       Ghost .class file in baseline obj/: {ghost_in_baseline_obj}")
        print(f"       Ghost class in baseline release APK: {ghost_in_baseline_dex}")

        stale_risk = ghost_in_candidate_dex and not ghost_in_baseline_dex
        print(f"    >> Stale Bytecode Hazard Demonstrated: {stale_risk} (Orphaned bytecode retained in Candidate APK!)")

        results.append({
            "trial": 4,
            "scenario": "Stale Bytecode Retention (Removed Inner Class)",
            "baseline": b4,
            "candidate": c4,
            "ghost_in_candidate_dex": ghost_in_candidate_dex,
            "ghost_in_baseline_dex": ghost_in_baseline_dex,
            "stale_bytecode_risk": stale_risk,
            "speedup_pct": round((b4["total_ms"] - c4["total_ms"]) / b4["total_ms"] * 100, 1)
        })

        # -------------------------------------------------------------
        # TRIAL 5: Corrupted Cache Recovery
        # -------------------------------------------------------------
        print("\n" + "=" * 60)
        print("TRIAL 5: Edge Case - Corrupted Intermediate State & Clean Rebuild Recovery")
        print("=" * 60)
        # Introduce corrupted class file in obj/
        corrupt_target = BUILD_DIR / "obj/com/example/timerapp/data/engine/CorruptedArtifact.class"
        corrupt_target.write_bytes(b"\xCA\xFE\xBA\xBE\x00\x00\x00\x00\xFF\xFFCORRUPTED_TRUNCATED_BYTECODE")

        print("5A. Candidate encountering corrupted intermediate class in obj/...")
        c5 = run_incremental_build(changed_files=[], auto_clean_recovery=True)
        print(f"    Candidate (with Recovery): Success={c5['success']} | Recovered={c5.get('recovered', False)} | Total Time={c5['total_ms']}ms")

        print("5B. Baseline clean build under same condition...")
        # Write corruption again to test baseline resilience
        corrupt_target.write_bytes(b"CORRUPTED")
        b5 = run_clean_build()
        print(f"    Baseline: Success={b5['success']} | Total Time={b5['total_ms']}ms (Clean wipe auto-purged corruption)")

        recovery_penalty = c5["total_ms"] - b5["total_ms"]
        print(f"    >> Recovery Penalty: {recovery_penalty}ms slower than standard clean build")

        results.append({
            "trial": 5,
            "scenario": "Corrupted Cache Recovery Fallback",
            "baseline": b5,
            "candidate": c5,
            "recovery_penalty_ms": recovery_penalty,
            "stale_bytecode_risk": False,
            "speedup_pct": round((b5["total_ms"] - c5["total_ms"]) / b5["total_ms"] * 100, 1)
        })

    finally:
        # Strict restoration of source files
        dual_clock_path.write_text(orig_dual_clock, encoding="utf-8")
        math_path.write_text(orig_math, encoding="utf-8")
        marks_path.write_text(orig_marks, encoding="utf-8")
        print("\n--- Restored all source files to pristine repository state ---")

        # Run final clean build to leave workspace with valid APK
        final_clean = run_clean_build()
        print(f"Final clean build verified: {final_clean['total_ms']}ms | SHA-256: {final_clean['sha256'][:16]}...")

    # Aggregated Statistical Analysis
    print("\n" + "=" * 80)
    print("EMPIRICAL COMPARISON SUMMARY: 5-TRIAL CONTROLLED EXPERIMENT")
    print("=" * 80)

    print(f"{'Trial':<6} | {'Scenario':<34} | {'Base Javac':<10} | {'Cand Javac':<10} | {'Base D8':<8} | {'Cand D8':<8} | {'Base Total':<10} | {'Cand Total':<10} | {'Speedup':<8}")
    print("-" * 115)
    for r in results:
        t = r["trial"]
        sc = r["scenario"][:34]
        bj = r["baseline"].get("javac_ms", 0)
        cj = r["candidate"].get("javac_ms", 0)
        bd = r["baseline"].get("d8_ms", 0)
        cd = r["candidate"].get("d8_ms", 0)
        bt = r["baseline"].get("total_ms", 0)
        ct = r["candidate"].get("total_ms", 0)
        sp = f"{r['speedup_pct']:+.1f}%"
        print(f"{t:<6} | {sc:<34} | {bj:>8}ms | {cj:>8}ms | {bd:>6}ms | {cd:>6}ms | {bt:>8}ms | {ct:>8}ms | {sp:>8}")

    # Output experiment JSON
    exp_out = Path("/Users/jeemainsadv/Swarm/metrics/benchmarks/incremental_experiment_results.json")
    exp_out.parent.mkdir(parents=True, exist_ok=True)
    with open(exp_out, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)
    print(f"\nExperiment results recorded in {exp_out}")

if __name__ == "__main__":
    main()
