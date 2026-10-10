# Free GitHub Pages readiness — NOT ACTIVATED / NOT DEPLOYED

FASE 4A.4, prepared 10 October 2026 (Asia/Jakarta). This procedure is for a **future separately authorized publication**, not permission to execute it now. Final legal gates: [approval checklist](../legal/FINAL_LEGAL_APPROVAL_CHECKLIST.md). Effective date is unset until final approval and actual publication.

## Hosting and current state

FASE 4A.4C planned official URL: **https://komprexo.digitalfuturesolutions.my.id/**, privacy alias **https://komprexo.digitalfuturesolutions.my.id/privacy/**. Historical project-path compatibility remains **https://digitalfuturesolutions69.github.io/komprexo/**. GitHub Free supports Pages from public repositories. Read-only repository metadata at review confirms public visibility, default branch **work**, and `has_pages=false`. This establishes eligibility under published guidance, not tested deployment, configured protection or a live privacy URL. No settings were changed. No extra repository or paid hosting is needed. The owner now requests a custom subdomain; historical komprexo.click plans are superseded. CNAME source/artifact preparation is authorized, but repository settings, public DNS and publication remain unauthorized. Read [custom-domain preparation and website/mail conflict](CUSTOM_DOMAIN_PREPARATION.md) before any future configuration.

The static site has no scripts/forms/photo upload/accounts/analytics. GitHub itself logs/stores visitors' IP addresses for security, including signed-out visitors. All five draft Privacy versions disclose this planned hosting behavior; GitHub-specific retention/deletion remain unverified. Local image processing does not send selected photos to this website.

## Static build and review artifacts

Python stdlib `scripts/build-static-site.py` builds `website/build`: Home/Privacy/Terms/Premium/Support/About in en/id/es/pt-BR/hi, plus six English root aliases. Relative navigation, language links, stylesheet and icon resolve under `/komprexo/` and at root. Offline Android text and website policy paragraphs share canonical `content/legal/*.json`.

Run from repository root:

```sh
python3 scripts/generate-legal-assets.py
python3 scripts/generate-store-listing.py
python3 scripts/build-static-site.py
python3 scripts/prepare-pages-artifact.py
python3 scripts/test-phase4-assets.py
cd website
npm ci
npx playwright install --with-deps chromium
npm test
```

Browser tests serve real HTTP at root and `/komprexo/`, including compact/landscape/200% text, navigation, dark theme, touch targets and policy parity. Ordinary read-only CI uploads review artifacts, not deployments. `prepare-pages-artifact.py` allows exactly40site files:36HTML pages, CSS, icon, `.nojekyll` and validated `CNAME`. It rejects extra files and symbolic/hard links before producing `website/pages-preview/pages-artifact.tar.gz` and the local `/komprexo/` preview. Upload `website/build` only, never the repository, private audit, Android reports, APK or credentials.

## Inactive workflow and security review

`.github/pages-deploy.yml.template` is outside `.github/workflows` with an unrecognized extension, so GitHub cannot run it. Active Android/static CI has `contents: read`, no Pages/OIDC write or deployment actions. The inactive template uses manual dispatch only, exact `approved_commit == GITHUB_SHA`, the explicit `PUBLISH KOMPREXO PAGES` phrase, branch `work`, bounded jobs and noncancelling deployment concurrency. An approval phrase is a confirmation, not proof of legal approval; retain owner evidence independently.

Build has read-only contents permission, checkout credentials are not persisted, and the generated relative-path site requires no configure-pages metadata. The unnecessary configure-pages step was removed: future settings activation is a separate owner action, not automatic enablement. Only deploy requests `pages: write` and `id-token: write`, waits for build, and uses `github-pages` environment. No custom PAT, secret or broad write grant is proposed. Official checkout/setup-node/upload-pages-artifact/deploy-pages versions are used; major-version tags are mutable. Re-review versions and pin reviewed action SHAs at separately authorized activation if required by owner security policy.

`--publication` **intentionally rejects current DRAFT/OWNER ACTION REQUIRED content**. Draft/inactive assertions in current tests must be replaced by approved-public-state assertions during the separately authorized publication change, retaining language parity, exact quotas/Billing disclosures, link/accessibility/artifact safeguards. Do not disable checks to deploy a draft. Template is a reviewed starting point, not an immediately executable publication workflow.

Manual `workflow_dispatch` requires a workflow on the default branch. Default is currently `work`, so no main merge is needed if unchanged. Recheck before activation. If the default changes, obtain separate authorization for any necessary default-branch change/workflow addition/merge; none is authorized here. Environment protection/reviewer availability depends on repository plan: verify the actual supported controls, restrict deployment to `work`, use a reviewer where available, and retain explicit approval if technical protection is unavailable. No environment was configured or independently verified.

## Deployment procedure — future approval only

1. Complete every applicable legal/SDK/native-speaker/support decision in the checklist; approve exact public text and required operator disclosures without unnecessarily exposing personal information. Keep mandatory consumer rights, adults18 positioning without invented age checks, actual Play price and24h offline limits. Resolve unknown vendor claims rather than fabricating them.
2. Obtain explicit owner authorization for publication preparation, settings/workflow activation and identified final publication commit. Change draft-only notices/About/download claims to factual approved public wording; do not advertise a Play download while no app release exists. Set the effective date only in coordination with actual approved publication, record actual time, and regenerate website/offline documents together. A cancelled or failed publication must not be represented as an effective public policy.
3. Run all Android/static/localization checks and approved-public-state assertions. Review the40-file site archive, hashes and contents. Retain the approved source SHA and artifact outside short CI retention as an owner-managed publication record. Verify exact-commit six-job CI, not a previous commit's run.
4. Recheck visibility/default branch/Pages eligibility and action versions. Under explicit approval only, activate the template in `.github/workflows`, configure Settings → Pages → GitHub Actions, and protect `github-pages` as described above. Resolve the website/mail hostname conflict and follow the separately authorized domain sequence in CUSTOM_DOMAIN_PREPARATION.md; CNAME alone does not configure Actions-hosted Pages. Commit activation changes and reverify final exact-SHA CI before dispatch.
5. Owner manually dispatches on `work`, supplying the exact approved SHA and phrase. Gate fails if branch HEAD changed; review and approve a new SHA instead of bypassing it. Review build result and deployment environment approval before deployment. Only the site artifact is published; no app release, Console submission or merge is implicit.
6. Verify all36pages/assets on the approved custom-domain HTTPS root and retained project-path preview, language/alias navigation, Privacy/Terms/Premium/Support/About, mobile200%/landscape/dark and support/refund/host links. Record Pages deployment/run URL, commit, artifact hash, approval and actual publication/effective date. A private local preview or uploaded review artifact is not a public site.
7. Only after a live successful privacy page and separate Console authorization, use the verified privacy URL in Play documentation/Console. Android application publication remains separate.

## Rollback procedure — future approval only

No prior approved public site exists now; current drafts are **not** rollback candidates. Before each future publication retain the last approved public source SHA,40-file artifact, SHA256, effective date/version and approval record. CI review artifacts expire; do not rely on them as permanent backups.

1. Diagnose and document the incorrect deployment/affected links without collecting visitor personal data. Obtain explicit owner rollback approval identifying the prior approved public version. If no approved version exists, obtain separate authorization to unpublish; do not expose historical DRAFT content as a fallback.
2. Check whether the prior text is still legally accurate (SDK/Billing identity and rights may have changed). Rollback must not silently remove current mandatory disclosures. Have owner/legal reviewer approve any version/date correction and offline/Store synchronization needed. Keep historical effective dates truthful; record rollback time separately.
3. Restore the approved site sources through a normal new commit on `work` (selected reviewed files, not a blanket reset); preserve unrelated Android implementation. No force push, history rewrite, main merge or Billing/processing rollback. Keep approved-public-state checks and publication safeguards active.
4. Regenerate the site, verify exact archive contents/hash and all regressions on the new SHA, push and confirm local=remote. Obtain owner approval for that SHA and dispatch the same gated workflow; never republish an unverified arbitrary artifact or bypass a failed test.
5. Verify HTTPS/project-path36pages, navigation and policy parity; record new run/deployment/SHA, previous version and reason. If emergency withdrawal is approved instead, owner uses Settings → Pages to unpublish and confirms the endpoint no longer serves the site. Account for cached copies/search indexes and links already distributed; do not promise instant deletion everywhere. Coordinate any Play privacy URL obligations separately.

## Official evidence reviewed 10 October 2026

- [Pages hosting/Free eligibility/project URL/IP logging](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages)
- [GitHub privacy statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement)
- [Manual dispatch/default branch](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)
- [Custom Pages workflows/permissions](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages)
- [configure-pages action inputs](https://github.com/actions/configure-pages/blob/v5/action.yml): automatic enablement defaults false, unnecessary for this generator.

No Pages activation, deployment, domain configuration, Console submission, app release, merge, AdMob or backend was performed. Readiness means locally/CI validated preparation; public legal approval and actual deployment are pending.


## FASE 4A.4A additional publication hold

[Owner publication decision](../legal/OWNER_PUBLICATION_DECISION.md) is **BLOCKED — SPECIFIC EVIDENCE REQUIRED.** Read the SDK, Data Safety, identity, mailbox and qualified-review gaps before using the deployment or rollback procedure above. A passing build or prepared Pages archive is not approval to activate hosting, publish drafts or set an effective date. This phase leaves the deployment template inactive and all settings unchanged.
