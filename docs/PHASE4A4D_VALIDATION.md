# FASE 4A.4D — Rumahweb static hosting preparation

Baseline local HEAD: `2cc20232dc32ebe1e3786061015bb4e28fbba372`, clean before edits.
Remote work confirmed at the identical SHA using the supported GitHub connector.
Git HTTPS fetch/ls-remote failed with `could not read Username for https://github.com`;
no credential helper or runtime-bound credentials are available for that transport.
No secrets were requested/read/stored. Supported authenticated connector Git object/ref
operations are used where authorized; final delivery records actual synchronization/CI.
No successful shell fetch/push is claimed when unavailable.

## Scoped changes

Existing Rumahweb Unlimited S/cPanel is now the planned host, root
`public_html/komprexo`, hostname `komprexo.digitalfuturesolutions.my.id`.
Root English aliases plus en/id/es/pt-BR/hi remain, without dependence on project paths.
Historical project-prefix regression is retained separately. No active Play download.

Current build has 38 static files (36 HTML, CSS, icon), no CNAME/.nojekyll.
New deterministic `prepare-static-artifact.py` creates root-level public ZIP,
external per-file SHA256/size manifest and ZIP checksum. Exact allowlist, symlink/
hardlink rejection, CRC and draft-publication gates remain enforced. Added reproducibility
and real extraction checks confirm index.html directly in public_html/komprexo.
No internal audit, Android/APK/AAB, credential/private key, logs or backend is included.

Current CI static job regenerates legal assets/builds/packages/tests/uploads review
artifacts only. Six jobs and API23/26/28/36 matrix retained; no deployment or Pages/OIDC
write permission. Pages script/template and previous instructions clearly marked
HISTORICAL/SUPERSEDED; prior records and original audit evidence preserved.

Five Privacy source/offline/web versions now describe planned Rumahweb browser/network
requests and explicitly unresolved host logging, uses, recipients, retention/deletion
and security. No GitHub fact is relabeled as Rumahweb evidence. Existing Android SDK
findings and Data Safety/publication HOLD remain. Qualified native/legal review required.
Published support Gmail unchanged; professional mailbox NOT externally verified.
No SDK/permissions, Billing/quota/image engine/application identity changes.

## Actual local checks

- Legal generation and static build PASS; five languages and 36 HTML routes.
- Python asset/legal/localization/archive/security checks **19 PASS**, 0 failures.
- Browser root/historical-prefix regression **142 PASS**, 0 failed/skipped (1.9 minutes).
- Explicitly rerun JVM Test task with unchanged source: **171 PASS**, 0 failures/errors/skips.
- Gradle unit/lint/debug and instrumentation APK/release Kotlin/runtime audit command
  BUILD SUCCESSFUL (1m32s, 100 tasks:10 executed/90 up-to-date).
- Local lint 0 errors/12 warnings; runtime audit99 and security check PASS.
- Local instrumentation emulator execution unavailable; exact final CI reports required.
- Wrapper unchanged, trusted8.11.1 SHA256:
  `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.
- Review ZIP38files/86,201bytes SHA256:
  `e3995b406d0e3d2e6774719a3197d12221929d5e0c682aefd6aa6e2f172ffe70`.
  Reproducible and extraction-root/manifest/source-parity checks PASS.

Final exact commit, API counts/CI lint/artifacts, authenticated branch update and
local=remote confirmation are reported with delivery. Previous CI results are historical.

## Remaining holds

[Current deployment/DNS/mail/rollback procedure](google-play/RUMAHWEB_STATIC_DEPLOYMENT.md).
Provider-verified hosting IP/vhost, actual DNS, SSL/HTTPS, hosted tests, mailbox external
send/receive/authentication, host privacy evidence, SDK/vendor/runtime/Console/identity/
legal/native-language approvals and effective date remain unresolved as applicable.
Future website A record value is UNKNOWN; no CNAME or guessed IP is proposed.
Apex, VPS/app subdomains, nameservers, MX/SPF/DKIM/DMARC and server settings unchanged.
No credentials, uploads, public staging/deployment, DNS/email changes, policy publication,
Play submission, app release, AdMob/analytics/backend, merge or force-push.
Stop after FASE4A.4D; technical preparation does not grant publication permission.
