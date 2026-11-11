"""Prepare editable Smali and rebuild an unsigned LTMori APK."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import urllib.request
import zipfile

URL = ("https://github.com/tranlocbtr99/DynAmic_Origin/releases/download/"
       "v2.0.4/LTMori-2.0.4-Origin-signed.apk")
SHA256 = "5aafcf8bcefe5d78dd54c980df4dd1ec03f7d440339baaca651cc70bd975e2f4"


def signature_entry(name):
    return bool(re.fullmatch(r"META-INF/(MANIFEST\.MF|[^/]+\.(RSA|DSA|EC|SF))", name, re.I))


def smali_digests(decoded):
    fingerprints = {}
    for directory in sorted(decoded.glob("smali*")):
        if not directory.is_dir():
            continue
        dex_name = directory.name.removeprefix("smali_") if directory.name != "smali" else "classes"
        if not re.fullmatch(r"classes\d*", dex_name):
            continue
        digest = hashlib.sha256()
        for source in sorted(directory.rglob("*.smali")):
            digest.update(source.relative_to(directory).as_posix().encode() + b"\0")
            digest.update(hashlib.sha256(source.read_bytes()).digest())
        fingerprints[dex_name + ".dex"] = digest.hexdigest()
    return fingerprints


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["prepare", "build"])
    parser.add_argument("--apktool", required=True, type=Path)
    parser.add_argument("--work", required=True, type=Path)
    args = parser.parse_args()
    work = args.work.resolve()
    work.mkdir(parents=True, exist_ok=True)
    base = work / "baseline.apk"
    decoded = work / "decoded"
    fingerprints = work / "baseline-smali.json"
    apktool = ["java", "-jar", str(args.apktool.resolve())]

    if args.action == "prepare" and not base.exists():
        with tempfile.TemporaryDirectory(dir=work) as temp:
            download = Path(temp) / "download.apk"
            urllib.request.urlretrieve(URL, download)
            if hashlib.sha256(download.read_bytes()).hexdigest() != SHA256:
                parser.error("Downloaded APK digest does not match release 2.0.4")
            download.replace(base)

    if not base.is_file() or hashlib.sha256(base.read_bytes()).hexdigest() != SHA256:
        parser.error("Expected the official 2.0.4 APK at " + str(base))

    if args.action == "prepare":
        if decoded.exists():
            parser.error("Decoded source already exists. Use a new work directory.")
        subprocess.run([*apktool, "d", "-r", str(base), "-o", str(decoded)], check=True)
        fingerprints.write_text(json.dumps(smali_digests(decoded)))
        return

    if not decoded.is_dir() or not fingerprints.is_file():
        parser.error("Run prepare before build")

    with tempfile.TemporaryDirectory(dir=work) as temp:
        output = Path(temp) / "unsigned.apk"
        current = smali_digests(decoded)
        baseline = json.loads(fingerprints.read_text())
        if current.keys() != baseline.keys():
            parser.error("Unexpected change to DEX layout")
        changed = {name for name in baseline if current[name] != baseline[name]}
        if changed:
            subprocess.run([*apktool, "b", "-f", "-na", str(decoded)], check=True)
        with zipfile.ZipFile(base) as original, zipfile.ZipFile(output, "w") as target:
            for entry in original.infolist():
                if signature_entry(entry.filename):
                    continue
                payload = ((decoded / "build" / "apk" / entry.filename).read_bytes()
                           if entry.filename in changed else original.read(entry.filename))
                target.writestr(entry, payload)
        output.replace(work / "unsigned.apk")


if __name__ == "__main__":
    main()
