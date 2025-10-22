# Signed APK Setup Guide

This guide explains how to set up signed APKs for Readle releases.

## Step 1: Create the Keystore (One-time setup)

Run this command to create your release keystore:

```bash
bash /tmp/create_keystore.sh
```

You will be asked for:
- **Keystore password**: Choose a strong password (save it!)
- **Key password**: Can be the same as keystore password  
- **Your name**
- **Organization** (can be anything, e.g., "Readle")
- **City, State, Country**

**IMPORTANT**: 
- The keystore will be created at `~/readle-release-key.jks`
- **Back it up immediately** to multiple secure locations (USB stick, encrypted cloud storage)
- **Save your passwords** in a password manager
- **DO NOT lose this** - without it, you cannot publish updates!

## Step 2: Prepare Keystore for GitHub

Convert your keystore to Base64:

```bash
base64 ~/readle-release-key.jks > ~/readle-keystore-base64.txt
```

This creates a text file with your keystore encoded.

## Step 3: Add GitHub Secrets

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** and add these 4 secrets:

| Secret Name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | Paste the content of `~/readle-keystore-base64.txt` |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `readle-key` (or what you chose) |
| `KEY_PASSWORD` | Your key password |

**Security note**: These secrets are encrypted and only accessible during GitHub Actions builds.

## Step 4: Clean up temporary files

```bash
rm ~/readle-keystore-base64.txt  # Remove the Base64 text file
# Keep ~/readle-release-key.jks for local builds
```

## Step 5: Create a Release

### Option A: Using GitHub Web Interface

1. Go to your repository on GitHub
2. Click **Releases** → **Draft a new release**
3. Click **Choose a tag** → Type `v1.0.0` → **Create new tag**
4. Fill in release title and description
5. Click **Publish release**

### Option B: Using Git Command Line

```bash
git tag v1.0.0
git push origin v1.0.0
```

Then create the release on GitHub web interface using that tag.

## What Happens Automatically

When you push a tag (e.g., `v1.0.0`):
1. GitHub Actions runs automatically
2. Tests are executed
3. Signed release APK is built
4. APK is uploaded to the GitHub release
5. Users can download it directly

## Local Signed Builds (Optional)

To build signed APKs locally, create a file `gradle.properties` in your home directory:

```bash
echo "READLE_RELEASE_STORE_FILE=$HOME/readle-release-key.jks" >> ~/.gradle/gradle.properties
echo "READLE_RELEASE_STORE_PASSWORD=YOUR_PASSWORD_HERE" >> ~/.gradle/gradle.properties
echo "READLE_RELEASE_KEY_ALIAS=readle-key" >> ~/.gradle/gradle.properties
echo "READLE_RELEASE_KEY_PASSWORD=YOUR_PASSWORD_HERE" >> ~/.gradle/gradle.properties
```

**Security**: This file is in your home directory, NOT in the project (won't be committed to git).

Then build locally:
```bash
./gradlew assembleRelease
```

The signed APK will be at: `app/build/outputs/apk/release/app-release.apk`

## Verification

To verify your APK is signed:

```bash
# Check signature
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk

# Should show:
# - Owner: Your name/organization
# - Signature algorithm: SHA256withRSA
# - Valid from/until dates
```

## Troubleshooting

### "Keystore password incorrect"
- Double-check your password
- Make sure you used the correct password in GitHub Secrets

### "Key alias not found"
- Default alias is `readle-key`
- Check with: `keytool -list -keystore ~/readle-release-key.jks`

### "Cannot recover key"
- Key password might be different from keystore password
- Try using the same password for both

## Important Reminders

- **Back up your keystore** to multiple locations
- **Save your passwords** in a password manager  
- **Never commit** the keystore to git (it's in `.gitignore`)
- **Keep the keystore secret** - anyone with it can sign apps as you
- **Version your releases** using semantic versioning (v1.0.0, v1.1.0, etc.)

## Next Steps

After setup:
1. Test by creating a `v1.0.0` release
2. Check GitHub Actions completed successfully
3. Download and install the APK from the release
4. Verify updates work by releasing `v1.0.1`

