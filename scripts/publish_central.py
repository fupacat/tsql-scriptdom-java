#!/usr/bin/env python3
import os
import sys
import hashlib
import subprocess
import zipfile
import base64

def get_md5_sha1(filepath):
    with open(filepath, 'rb') as f:
        data = f.read()
    return hashlib.md5(data).hexdigest(), hashlib.sha1(data).hexdigest()

def sign_file(filepath, passphrase, dry_run=False):
    asc_path = filepath + '.asc'
    if os.path.exists(asc_path):
        os.remove(asc_path)
    cmd = ['gpg', '--batch', '--yes', '--pinentry-mode', 'loopback', '--armor', '--detach-sign']
    input_bytes = None
    if passphrase:
        cmd.extend(['--passphrase-fd', '0'])
        input_bytes = (passphrase + '\n').encode()
    cmd.extend(['--output', asc_path, filepath])
    try:
        subprocess.run(cmd, input=input_bytes, check=True)
    except subprocess.CalledProcessError:
        if dry_run:
            print(f"[Dry run] GPG signing failed or not configured locally, creating dummy {asc_path}")
            with open(asc_path, 'w') as f:
                f.write("-----BEGIN PGP SIGNATURE-----\nDummy\n-----END PGP SIGNATURE-----\n")
        else:
            raise
    return asc_path

def add_file_and_checksums(bundle, arc_dir, filepath, passphrase, dry_run=False):
    if not os.path.exists(filepath):
        raise FileNotFoundError(f"Required artifact not found: {filepath}")
    filename = os.path.basename(filepath)
    arc_path = f"{arc_dir}/{filename}"
    bundle.write(filepath, arc_path)
    
    md5, sha1 = get_md5_sha1(filepath)
    bundle.writestr(f"{arc_path}.md5", md5)
    bundle.writestr(f"{arc_path}.sha1", sha1)
    
    asc_path = sign_file(filepath, passphrase, dry_run=dry_run)
    asc_arc_path = f"{arc_dir}/{filename}.asc"
    bundle.write(asc_path, asc_arc_path)
    asc_md5, asc_sha1 = get_md5_sha1(asc_path)
    bundle.writestr(f"{asc_arc_path}.md5", asc_md5)
    bundle.writestr(f"{asc_arc_path}.sha1", asc_sha1)

def main():
    if len(sys.argv) < 2:
        print("Usage: publish_central.py <version> [--dry-run]")
        sys.exit(1)
        
    version = sys.argv[1]
    dry_run = "--dry-run" in sys.argv
    username = os.environ.get('SONATYPE_USERNAME')
    password = os.environ.get('SONATYPE_PASSWORD')
    passphrase = os.environ.get('OSSRH_GPG_PASSPHRASE', '')
    
    if not dry_run and (not username or not password):
        print("Missing SONATYPE_USERNAME or SONATYPE_PASSWORD. Skipping Central publish.")
        return
        
    bundle_zip_path = "target/central-bundle.zip"
    os.makedirs("target", exist_ok=True)
    if os.path.exists(bundle_zip_path):
        os.remove(bundle_zip_path)
        
    print(f"Building Sonatype Central bundle for version {version}...")
    with zipfile.ZipFile(bundle_zip_path, 'w', zipfile.ZIP_DEFLATED) as bundle:
        # 1. Parent
        parent_dir = f"io/github/fupacat/tsql-scriptdom-parent/{version}"
        parent_target_pom = f"target/tsql-scriptdom-parent-{version}.pom"
        with open("pom.xml", 'rb') as src, open(parent_target_pom, 'wb') as dst:
            dst.write(src.read())
        add_file_and_checksums(bundle, parent_dir, parent_target_pom, passphrase, dry_run=dry_run)
        
        # 2. Astgen
        astgen_dir = f"io/github/fupacat/tsql-scriptdom-astgen/{version}"
        astgen_target_pom = f"tsql-scriptdom-astgen/target/tsql-scriptdom-astgen-{version}.pom"
        with open("tsql-scriptdom-astgen/pom.xml", 'rb') as src, open(astgen_target_pom, 'wb') as dst:
            dst.write(src.read())
        add_file_and_checksums(bundle, astgen_dir, astgen_target_pom, passphrase, dry_run=dry_run)
        add_file_and_checksums(bundle, astgen_dir, f"tsql-scriptdom-astgen/target/tsql-scriptdom-astgen-{version}.jar", passphrase, dry_run=dry_run)
        add_file_and_checksums(bundle, astgen_dir, f"tsql-scriptdom-astgen/target/tsql-scriptdom-astgen-{version}-sources.jar", passphrase, dry_run=dry_run)
        add_file_and_checksums(bundle, astgen_dir, f"tsql-scriptdom-astgen/target/tsql-scriptdom-astgen-{version}-javadoc.jar", passphrase, dry_run=dry_run)
        
        # 3. Core
        core_dir = f"io/github/fupacat/tsql-scriptdom-core/{version}"
        core_pom = "tsql-scriptdom-core/dependency-reduced-pom.xml"
        if not os.path.exists(core_pom):
            core_pom = "tsql-scriptdom-core/pom.xml"
        core_target_pom = f"tsql-scriptdom-core/target/tsql-scriptdom-core-{version}.pom"
        with open(core_pom, 'rb') as src, open(core_target_pom, 'wb') as dst:
            dst.write(src.read())
        add_file_and_checksums(bundle, core_dir, core_target_pom, passphrase, dry_run=dry_run)
        add_file_and_checksums(bundle, core_dir, f"tsql-scriptdom-core/target/tsql-scriptdom-core-{version}.jar", passphrase, dry_run=dry_run)
        add_file_and_checksums(bundle, core_dir, f"tsql-scriptdom-core/target/tsql-scriptdom-core-{version}-sources.jar", passphrase, dry_run=dry_run)
        add_file_and_checksums(bundle, core_dir, f"tsql-scriptdom-core/target/tsql-scriptdom-core-{version}-javadoc.jar", passphrase, dry_run=dry_run)

    print("Bundle created successfully. Archive contents:")
    with zipfile.ZipFile(bundle_zip_path, 'r') as bundle:
        for name in sorted(bundle.namelist()):
            print(f"  {name}")
            
    if dry_run:
        print("[Dry run] Skipping upload.")
        return

    username = username.strip().strip('"').strip("'")
    password = password.strip().strip('"').strip("'")
    token = base64.b64encode(f"{username}:{password}".encode()).decode()
    
    # Sonatype Central accepts 'UserToken' (used by official central-publishing plugin) or 'Bearer'
    last_response = ""
    for auth_type in ["UserToken", "Bearer"]:
        print(f"Uploading bundle to Sonatype Central Portal API using {auth_type} authorization...")
        cmd = [
            'curl', '-s', '-S', '-X', 'POST',
            f'https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC&name=tsql-scriptdom-{version}',
            '-H', f'Authorization: {auth_type} {token}',
            '-F', f'bundle=@{bundle_zip_path}',
            '-w', '\nHTTP_STATUS:%{http_code}'
        ]
        result = subprocess.run(cmd, capture_output=True, text=True)
        raw_output = result.stdout.strip()
        lines = raw_output.split('\n')
        status_code = ""
        body = raw_output
        if lines and lines[-1].startswith("HTTP_STATUS:"):
            status_code = lines[-1].split(":", 1)[1].strip()
            body = '\n'.join(lines[:-1]).strip()

        print(f"Sonatype Central HTTP Status: {status_code}")
        print(f"Response body: {body}")
        last_response = body

        if status_code in ["200", "201"] and "error" not in body.lower():
            print("Bundle successfully submitted to Sonatype Central!")
            print(f"Deployment ID: {body}")
            return

    print(f"Deployment rejected: {last_response}", file=sys.stderr)
    sys.exit(1)

if __name__ == '__main__':
    main()
