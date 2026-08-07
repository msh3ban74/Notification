# Auto-deploy Firestore rules from GitHub

`firestore.rules` is published to the live Firebase project
(`notification-76116`) automatically on every push that changes it, by the
workflow `.github/workflows/deploy-firestore-rules.yml`.

It needs **one GitHub secret** — a Google service-account key — set up once:

## 1. Create a service account with deploy permission

1. Open the Google Cloud console for the project:
   https://console.cloud.google.com/iam-admin/serviceaccounts?project=notification-76116
2. **Create service account** → name it e.g. `github-rules-deployer` → Create.
3. Grant it the role **Firebase Rules Admin**
   (or, more broadly, **Firebase Admin**). → Done.
4. Open the new service account → **Keys** tab → **Add key** → **Create new
   key** → **JSON** → Download. A `.json` file lands on your computer.

## 2. Store the key as a GitHub secret

1. Open: https://github.com/msh3ban74/Notification/settings/secrets/actions
2. **New repository secret**
   - **Name:** `FIREBASE_SERVICE_ACCOUNT`
   - **Secret:** paste the **entire contents** of the downloaded JSON file.
3. **Add secret**.

## Done

From now on, any push that edits `firestore.rules` publishes the new rules
to Firebase within a minute. You can also trigger it by hand from the
**Actions** tab → *Deploy Firestore rules* → **Run workflow**.

The current rules restrict every user to their own `users/{uid}` document
and everything nested under it — no one can read or write anyone else's
data, and unauthenticated requests are denied.
