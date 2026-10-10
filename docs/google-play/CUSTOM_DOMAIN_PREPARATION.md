# FASE 4A.4C — Custom domain preparation, NOT PUBLICATION

Prepared 10 October 2026. Approved planned host: `komprexo.digitalfuturesolutions.my.id`.
Hosting remains free GitHub Pages. No Pages activation, DNS changes, mailbox configuration,
deployment, legal publication, effective date, Play submission or app release is authorized.

## Static URLs and review artifact

| Route | Planned URL |
| --- | --- |
| Home | https://komprexo.digitalfuturesolutions.my.id/ |
| Privacy | https://komprexo.digitalfuturesolutions.my.id/privacy/ |
| Terms | https://komprexo.digitalfuturesolutions.my.id/terms/ |
| Premium | https://komprexo.digitalfuturesolutions.my.id/premium/ |
| Support | https://komprexo.digitalfuturesolutions.my.id/support/ |
| About | https://komprexo.digitalfuturesolutions.my.id/about/ |

These are prepared URLs, not confirmed live endpoints. English root aliases and
`/en/`, `/id/`, `/es/`, `/pt-BR/`, `/hi/` routes use relative navigation/assets.
The same files work beneath the historical `/komprexo/` project prefix.
No remote scripts, tracking, forms, image uploads or new website dependencies are added.

`website/CNAME` contains exactly the approved hostname and a final newline, without
scheme, path, repository name or DNS target. The builder copies it into
`website/build/CNAME`; preparation validates its exact bytes and includes it in
the 40-file review archive (36 HTML, CSS, icon, `.nojekyll`, CNAME).
The archive is not a deployment. Private files and symbolic/hard links remain rejected.
`--publication` still rejects DRAFT/OWNER ACTION REQUIRED pages.

**GitHub Actions distinction:** official GitHub documentation says existing CNAME
files are ignored and not required for custom Actions publishing. Including this
file preserves a reviewed host declaration and branch-publishing compatibility;
it does not configure the GitHub Pages custom-domain setting. A future authorized
operator must separately configure the repository domain. No settings are changed here.

## DNS instructions — conditional, DO NOT EXECUTE NOW

Requested future record, only after the conflict below is resolved and publication
and DNS changes receive explicit approval:

| Type | Fully qualified name | Target |
| --- | --- | --- |
| CNAME | komprexo.digitalfuturesolutions.my.id | digitalfuturesolutions69.github.io |

In the `digitalfuturesolutions.my.id` zone a provider may expect the name `komprexo`;
confirm its UI rather than duplicating the zone. Target is a bare hostname, not
`https://`, `/komprexo/`, an IP address or a `*.pages.github.io` deployment host.
Use the provider's approved TTL. Do not add wildcard records. Review the exact host's
existing A/AAAA/CNAME records before proposing any change; do not leave competing
records or remove anything as part of this phase.

### Website/mail conflict — OWNER ACTION REQUIRED

The professional mailbox is `support@komprexo.digitalfuturesolutions.my.id`.
Its mail domain is exactly the planned website hostname. Standard DNS does not
allow a CNAME to coexist with ordinary MX or TXT records at that same name
(RFC 2181 section 10.1; DNSSEC metadata exceptions do not permit mail records).
SPF TXT at the same name would also conflict. DKIM/DMARC often use different
names; their real configuration has not been inspected. Do not infer a working
mail setup from the owner's mailbox-creation statement.

**Do not create the requested CNAME if that host has or needs MX/TXT mail records.**
Do not delete or modify MX, SPF, DKIM, DMARC, or cPanel/email settings to make it fit.
The owner and DNS/mail provider must choose and verify a compatible architecture:
retain the professional mail domain and approve a different website DNS method,
or approve a distinct website/mail hostname. Neither alternative is configured
or supplied as additional DNS records here. The requested CNAME is conditional,
not a ready-to-apply configuration that overrides email. Domain ownership,
current DNS records, mail routing and public HTTPS remain unverified in this phase.

## Controlled email migration — HOLD

All current public application, offline policy, website and Store disclosures retain
**komprexo.support@gmail.com**. The professional address is pending and appears
only in preparation documentation, not as the current published support contact.

Before a separately approved migration, the owner/mail administrator must:

1. Resolve the website/mail DNS conflict without disrupting either service.
2. Verify provider-required DNS records, SPF/DKIM/DMARC and actual delivery using
   independent external senders and recipients. Test inbound support delivery,
   outbound replies and spam handling; retain sanitized results, not message contents.
3. Verify mailbox access, recovery, account protection and support/privacy request
   handling. Resolve the existing retention/deletion operational gaps.
4. Approve an exact contact migration commit. Update canonical five-language legal
   content, Store documentation, Android contact actions and static pages together;
   regenerate and run regression/parity checks. Do not claim verification prematurely.
5. Keep the Gmail contact monitored during an owner-approved transition period;
   no duration, forwarding, auto-response or deletion practice is invented here.

No mail was sent, credentials read, DNS verified or server settings modified here.

## Future publication and rollback

Preserve all SDK/Data Safety evidence and the BLOCKED decision in
[OWNER_PUBLICATION_DECISION.md](../legal/OWNER_PUBLICATION_DECISION.md).
Effective date remains unresolved until approved actual publication. See the
[publication procedure](STATIC_POLICY_PUBLISHING.md) and legal checklist.

Only after legal, domain/mail and explicit publication approval: verify domain
ownership using GitHub's actual generated challenge (do not invent a TXT value),
review/protect the deployment environment, approve the source commit, activate
only the reviewed manual template, and set the repository custom domain before
pointing authorized DNS at GitHub. GitHub warns that unbound DNS can enable domain
takeover. Recheck exact-commit CI, then dispatch the owner-approved deployment.
Verify DNS, HTTPS issuance/enforcement and all localized routes on the live host;
no current claim of working public DNS/TLS is made.

Rollback must use an approved prior public commit/artifact with its recorded
host, contact and effective date. There is no prior approved public site now.
A content rollback does not require changing DNS or mail. Any domain/settings
rollback needs separate owner authorization, coordinated DNS review and the
GitHub removal procedure; avoid leaving a dangling hostname pointing at an
unbound Pages site. Never force-push or redeploy current drafts as a fallback.

## Security and evidence

The inactive `.github/pages-deploy.yml.template` stays outside active workflows.
Active six-job CI has read-only contents access and no Pages/OIDC write or deployment
actions. Future manual template checks `work`, exact SHA and approval phrase,
does not persist checkout credentials, and grants Pages/OIDC write only to deploy.
The approval phrase alone is not legal approval. Mutable action tags and deployment
protection must be re-reviewed at authorized activation, as documented previously.

Authoritative sources accessed 10 October 2026:

- GitHub custom-domain management: https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site — subdomain CNAME target excludes repository; configure GitHub before DNS; Actions ignores CNAME files; domain verification recommended.
- DNS clarification RFC 2181 §10.1: https://datatracker.ietf.org/doc/html/rfc2181#section-10.1 — a CNAME name cannot have other ordinary data. This is a standards constraint, not evidence of current DNS records.

The prepared site is technically testable; publication remains blocked by actual
vendor/runtime/Console Data Safety evidence, operator disclosure/legal review,
qualified native-language review and unresolved support operations. Five languages
do not establish launch markets. Custom-domain preparation resolves none of those
privacy classifications and does not authorize publishing legal drafts.
