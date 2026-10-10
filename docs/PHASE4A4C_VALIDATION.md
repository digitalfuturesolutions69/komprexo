# FASE 4A.4C — Custom domain and website preparation

Baseline verified locally and after `git fetch origin work`:
`e86c456dcd0b1167d5546eade44273cf441fd65c`. Branch `work`, clean at inspection.
Repository `digitalfuturesolutions69/komprexo`. Prepared 10 October 2026.
Final SHA, remote synchronization and exact-commit CI results are recorded in the
accompanying delivery report; this document does not claim a future run has passed.

## Scope and preservation

- Planned official host: `https://komprexo.digitalfuturesolutions.my.id/`;
  English Privacy alias `/privacy/`; Terms, Premium, Support, About and Home aliases retained.
- All five language routes retained: en, id, es, pt-BR, hi. Relative assets/navigation
  still work at root and beneath `/komprexo/`; no behavior or legal paragraph changes.
- Exact validated CNAME source is copied to the 40-file review artifact.
  Incorrect hosts, schemes, multiple lines, extra private files and symbolic/hard links
  are rejected. Publication still fails on unresolved legal drafts.
- Only inactive deployment-template comments changed. Active six-job CI is unchanged,
  read-only, and cannot deploy Pages. Repository metadata check: public, default work,
  `has_pages=false`; no settings were changed.
- All Android source/assets/configuration, SDK versions and SDK audit/Data Safety
  evidence remain byte-identical to baseline. Application ID remains
  `com.digitalfuturesolutions.komprexo`; namespace `com.komprexo.app` retained.
  Billing, quotas and image processing are untouched.
- Public support remains `komprexo.support@gmail.com`. Professional mailbox pending;
  no automatic contact replacement, sent messages or email-server changes.

## Actual local validation

- Website build: 36 HTML pages, five languages plus English aliases; PASS.
- Static Pages archive: 40 exact allowed files with CNAME; PASS, NOT DEPLOYED.
- Python asset/localization/legal/security/preparation tests: **18 PASS**, 0 failures.
- Browser regressions: **142 PASS**, 0 failed/skipped, real local HTTP root/project path.
  Includes root aliases/CNAME, navigation and assets, unchanged Gmail, five-language
  offline parity, compact/landscape/200% text, themes, touch targets and local-only requests.
- Gradle unit/lint/debug APK/instrumentation APK/release Kotlin/runtime audit command:
  BUILD SUCCESSFUL, 100 tasks (4 executed, 96 up-to-date). Existing unchanged task outputs
  were reused; a separate unit execution forced Test tasks to run without changing source.
  That execution succeeded (54 seconds): **171 tests PASS**, 0 failures/errors/skips.
- Android runtime/security audit: PASS, 99 resolved runtime artifacts, unchanged four
  audited permissions. No new SDK, networking integration or permissions.
- Local lint report: 0 errors, 12 warnings. Existing warnings remain; exact final CI lint
  report is independently reviewed and may contain additional CI-only warnings.
- Trusted Gradle 8.11.1 wrapper SHA256:
  `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.
- Local instrumentation execution is unavailable without the supported emulator/KVM
  environment. Existing API23/26/28/36 coverage remains in CI; do not infer current counts
  from the preceding phase. Final CI reports/artifacts must be verified at the final SHA.

## Publication and DNS/email hold

See [domain/DNS/mail preparation](google-play/CUSTOM_DOMAIN_PREPARATION.md) and
[existing publication decision](legal/OWNER_PUBLICATION_DECISION.md).
Requested CNAME targets `digitalfuturesolutions69.github.io` without repository path.
It is conditional: website hostname equals the professional mail domain, so a CNAME
cannot coexist with MX/SPF TXT at that same name. Owner/provider resolution is required
before applying DNS. Current DNS records, domain ownership, public TLS and professional
mail send/receive were not verified; no records or server settings were changed.

GitHub Actions does not use CNAME to configure its custom domain; separate authorized
repository settings are required. This phase only prepares the artifact and instructions.
DRAFT/effective-date placeholders, unknown SDK collection/sharing/retention/deletion,
Console/operator/legal/native-language and support-operation blockers remain preserved.
No Pages activation, deployment, public legal policy, DNS/email changes, Play submission,
APK/AAB release, AdMob, merge, force-push or additional phase is authorized or performed.

Technical preparation can pass tests while publication remains HOLD.
