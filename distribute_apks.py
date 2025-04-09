import subprocess
import os
import sys
import shutil

# Replace with your actual Firebase App IDs for each flavor
FIREBASE_APP_IDS = {
    "server": "1:117465196008:android:6d07f7a8bba00fcef7305d",  # Replace with real ID
    "remote": "1:117465196008:android:cc33de71e5ae216bf7305d",  # Replace with real ID
}

APK_OUTPUT_PATH = "app/build/outputs/apk"
GOOGLE_SERVICES_SOURCE_DIR = "firebase-configs"
GOOGLE_SERVICES_DEST = "app/google-services.json"

def run_command(command, cwd=None):
    print(f"\n🔧 Running: {' '.join(command)}")
    result = subprocess.run(command, cwd=cwd, capture_output=True, text=True)
    
    # Always print both stdout and stderr
    if result.stdout:
        print("🔹 Output:\n" + result.stdout)
    if result.stderr:
        print("🔸 Error Output:\n" + result.stderr)
    
    if result.returncode != 0:
        print(f"❌ Command failed with exit code {result.returncode}")
        sys.exit(result.returncode)

def copy_google_services_json(flavor):
    source_path = os.path.join(GOOGLE_SERVICES_SOURCE_DIR, flavor, "google-services.json")
    if not os.path.exists(source_path):
        print(f"❌ google-services.json for flavor '{flavor}' not found at {source_path}")
        sys.exit(1)
    print(f"📄 Copying google-services.json for '{flavor}' flavor")
    shutil.copyfile(source_path, GOOGLE_SERVICES_DEST)

def build_apk(flavor):
    build_command = ["./gradlew", f"assemble{flavor.capitalize()}Release"]
    run_command(build_command)

def upload_to_firebase(flavor):
    app_id = FIREBASE_APP_IDS.get(flavor)
    if not app_id:
        print(f"❌ No Firebase App ID found for flavor '{flavor}'")
        return

    apk_path = os.path.join(
        APK_OUTPUT_PATH, flavor, "release", f"app-{flavor}-release.apk"
    )

    if not os.path.exists(apk_path):
        print(f"❌ APK not found at {apk_path}")
        return

    upload_command = [
        "firebase", "appdistribution:distribute", apk_path,
        "--app", app_id,
        "--groups", "qa-group",
        "--release-notes", f"Auto-upload for {flavor} flavor"
    ]

    run_command(upload_command)

def main():
    for flavor in ["server", "remote"]:
        print(f"\n🚀 Starting build and upload for '{flavor}' flavor")
        copy_google_services_json(flavor)
        build_apk(flavor)
        upload_to_firebase(flavor)

if __name__ == "__main__":
    main()