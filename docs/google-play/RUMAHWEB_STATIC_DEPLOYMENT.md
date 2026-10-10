# Rumahweb static deployment preparation — NOT DEPLOYED

FASE 4A.4D, 10 October 2026. Owner-approved preparation target: existing Rumahweb
Unlimited S shared hosting, cPanel, document root `public_html/komprexo`.
Planned host: `https://komprexo.digitalfuturesolutions.my.id/`.
Public developer Digital Future Solutions; app `com.digitalfuturesolutions.komprexo`.
This is preparation only. No hosting credentials, uploads, DNS/server changes,
legal publication, live effective date, Play submission or app release are authorized.

## Build and review

From repository root:

```sh
python3 scripts/generate-legal-assets.py
python3 scripts/build-static-site.py
python3 scripts/prepare-static-artifact.py
python3 scripts/test-phase4-assets.py
npm --prefix website ci
npx --prefix website playwright install --with-deps chromium
npm --prefix website test
```

Alternatively run browser installation inside `website`: `npx playwright install --with-deps chromium`.
CI static-preview builds/generates/checks the same sources and uploads review output;
it does not upload to Rumahweb. Android CI retains debug/unit/lint/security checks
and API23/26/28/36 instrumentation. Review the exact-final-commit six-job run.

`website/build` contains **38 public files**: 36 HTML pages (six English root aliases
plus six pages for en/id/es/pt-BR/hi), `style.css`, `assets/icon.svg`.
Routes: `/`, `/privacy/`, `/terms/`, `/premium/`, `/support/`, `/about/`, plus the
same routes beneath each locale. Relative navigation/assets work at hostname root.
A historical `/komprexo/` prefix regression remains a secondary portability check;
production does not depend on that prefix or GitHub Pages.

`website/deployment/komprexo-static.zip` contains only these files, with `index.html`
at ZIP root. Extracting its contents into `public_html/komprexo` must therefore create
`public_html/komprexo/index.html`, not `public_html/komprexo/website/build/index.html`.
No CNAME, `.nojekyll`, backend/PHP, Android source/APK/AAB, audit documents, credentials,
keys, logs, tracking scripts, support message contents or customer records are included.
All files must match the exact allowlist; symlinks and hard links are rejected.

Outside the deployable ZIP:

- `manifest.json`: every public relative path, byte length and SHA256.
- `komprexo-static.zip.sha256`: exact ZIP checksum.
- `project-preview/`: local secondary prefix-test copy, not a production upload.

Do not upload a CI outer ZIP, test reports, the repository, manifest/internal preparation
documents or the project-preview directory into the public document root.
The deterministic ZIP uses fixed timestamps/permissions; identical public bytes
produce the same package hash. CI outer artifact ZIPs have independent hashes.

Verify the inner ZIP before future upload:

```sh
cd website/deployment
sha256sum -c komprexo-static.zip.sha256
unzip -l komprexo-static.zip
```

Check each ZIP entry against manifest SHA256/length, no absolute paths or `..`, no
extra nested root folder, and no unapproved files. Current ZIP is a **review draft**,
not authorized public content. Passing checksum means byte integrity, not legal approval.

## Draft publication gate — HOLD

`python3 scripts/prepare-static-artifact.py --publication` deliberately rejects
current DRAFT/OWNER ACTION REQUIRED HTML. It validates before writing output.
An ordinary review ZIP remains possible so the owner can inspect source and layout;
this is not a public staging deployment. Do not make previews public to bypass approval.

Before publication, resolve [OWNER_PUBLICATION_DECISION.md](../legal/OWNER_PUBLICATION_DECISION.md),
SDK/Data Safety/vendor/runtime/Console gaps, operator disclosures, legal/native-language
review and support handling; approve exact content and hosting privacy evidence.
Only approved actual publication may establish an effective date. Replace draft-only
assertions through a reviewed publication change, retaining content parity, SDK facts,
quotas/Billing accuracy, accessibility/security, archive and navigation checks.
No active Play Store download is advertised; the app is not released.

## Future File Manager upload — explicit separate approval only

1. Obtain explicit owner authorization for the exact commit, legal text, artifact hash,
   hosting upload, DNS and publication actions as applicable. Keep each approval scoped.
2. Owner verifies through the provider/authenticated cPanel that the hostname maps
   exactly to `public_html/komprexo`, the web-hosting IP is correct, and the account is
   the approved existing service. Do not request credentials in chat or repository.
   Preserve apex/VPS services and all other document roots.
3. Build and pass all six exact-commit CI jobs. Run the publication guard and review
   all 38 file hashes. Save an approved copy outside short CI artifact retention.
4. Before changes, back up only the current Komprexo document root into a private
   location outside public_html. Record source/hash/date; do not back up or alter
   unrelated VPS/apps. There is no verified prior Komprexo public deployment now.
5. Owner uses cPanel File Manager over its verified HTTPS service. Navigate to
   `public_html/komprexo`, confirm the path, and upload only the approved inner ZIP.
   Review replacement/removal of stale site files; do not blindly overwrite unrelated
   files or create an extra folder. Avoid extraction in the apex public_html directory.
6. Extract ZIP contents into this exact directory; confirm `index.html`, `style.css`,
   `assets/icon.svg` and all locale directories. Remove the upload ZIP from the public
   directory afterward; keep checksums/manifests/backups private. No PHP/backend required.
   Coordinate a controlled replacement/maintenance window with the owner to avoid a
   partly updated public site. No atomicity guarantee for File Manager is claimed.
7. Only authorized administrators may perform the DNS/SSL steps below. Verify the live
   hostname and certificate hostname/chain/expiry, HTTPS redirect behavior and absence
   of mixed content. Do not claim SSL is issued merely because cPanel offers an SSL tool.
8. Verify `https://komprexo.digitalfuturesolutions.my.id/privacy/` and Home/Terms/Premium/
   Support/About return expected static HTML and all five language links/assets work.
   Test compact, landscape, 200% text, contact mailto and refund links. Verify DRAFT is
   absent only after approved publication; record actual public URL/date/commit/hash.
   Console URL entry and Android release still need separate authorization.

## DNS readiness — no changes now

The external Managed DNS panel is authoritative per owner context. Keep nameservers,
apex records and existing application subdomain records pointing to the production
VPS exactly as they are. Do not move the primary domain or alter VPS services.

Future website record: **A**, name `komprexo.digitalfuturesolutions.my.id` (often
`komprexo` in the `digitalfuturesolutions.my.id` zone), value **OWNER/PROVIDER VERIFIED
RUMAHWEB WEB-HOSTING IPv4 REQUIRED**. No numeric IP is known or invented here.
Obtain it from Rumahweb or authenticated cPanel and verify the domain/vhost mapping;
do not use a guessed IP, the VPS IP, a mail-server IP or a GitHub Pages IP.
Inspect existing exact-host A/AAAA/CNAME before approving a change. Do not leave stale
conflicting website CNAME/AAAA records. No AAAA is proposed without provider verification.
**Do not propose a website CNAME at this hostname.**

A records permit mail MX/TXT at the same hostname, unlike the superseded Pages CNAME.
Preserve provider-required MX and SPF TXT at the mail domain and DKIM selector TXT/
DMARC records at the correct provider-specified names. Exact targets, selectors,
policy/TTL and authentication status remain unverified; do not invent or apply values.
No mail-related records, cPanel settings or nameservers were inspected or modified.
Actual DNS, certificate provisioning and ownership validation remain future actions.

## Contact migration — separate editorial approval

Current public contact is **komprexo.support@gmail.com** everywhere. Professional
`support@komprexo.digitalfuturesolutions.my.id` is CREATED per owner but external
send/receive and SPF/DKIM/DMARC authentication are NOT VERIFIED.

Owner/provider must test independent external inbound messages and outbound replies,
authentication/header results and spam delivery, mailbox access/recovery/security,
and privacy-request handling/retention/deletion. Keep evidence sanitized and private.
After explicit approval, update canonical legal JSON, Android contact resources/actions,
Store documentation and static contact generation together across all five languages.
Run parity/contact regressions and review the exact migration commit. Keep Gmail
monitored for an owner-approved transition; no forwarding or retention interval is
invented. No mailbox changes or emails sent here.

## Hosting privacy evidence and limitations

Owner confirms provider/service/document root, not logging/data handling practices.
Five Privacy drafts now disclose planned browser-to-Rumahweb requests with network/IP
information needed for page delivery, no photo upload/accounts/tracking scripts, and
no public deployment. This is protocol/data-flow context, not observed production logs.
Rumahweb-specific log schema, purposes, recipients/sharing, server retention/deletion,
security/encryption practices and controls remain **UNKNOWN / OWNER ACTION REQUIRED**.
Before publication obtain current provider evidence and account-specific configuration,
actual hosted tests and qualified review. No GitHub privacy statement is presented as
Rumahweb evidence. Historical GitHub evidence remains in prior audit records explicitly
scoped to the superseded host; Android Billing/SDK findings are unchanged.

## Rollback — future approval only

Retain the last owner-approved public package, manifest/hash, source commit, policy
version/effective date, contact and private document-root backup. Review ZIPs expire
in CI and are not a permanent backup. Current drafts are not rollback candidates.

After an authorized deployment, diagnose the affected Komprexo files and obtain
rollback approval. Restore only the previously approved root contents; verify root,
privacy, assets, locales, HTTPS and contact. Record rollback time/hash and whether
public policy dates require review. Do not roll back app/Billing/quotas or other sites.
A content rollback should not need DNS, mail, nameserver or VPS changes. If a first
publication fails and no approved prior site exists, use an owner-approved maintenance
or unpublish procedure; do not expose drafts. Any DNS/SSL rollback is a separate,
provider-verified owner action. Never force-push.

## Pages retirement

`website/CNAME` is removed. Current build/package has no CNAME or `.nojekyll`.
Active CI uses `prepare-static-artifact.py`, no Pages deploy/configure/upload action
and no Pages/OIDC write permission. `.github/pages-deploy.yml.template` and old Pages
preparation script are marked SUPERSEDED/HISTORICAL, not active or current procedures.
Previous phase reports and SDK evidence are preserved for traceability.
