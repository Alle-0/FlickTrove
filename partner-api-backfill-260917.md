# Partner API — comment backfill guide

This document covers **scripted historical backfill** of your app's existing,
user-authorized original comments into the shared archive. Backfill may start with
one coordinated migration, then continue incrementally whenever another user grants
sharing consent that covers their existing comments. It is not permission to publish
every historical row in a partner database. For day-to-day integration — reads, live
writes, moderation, quotas — use
[partner-api-integration.md](partner-api-integration.md).

Partners are strongly encouraged to offer this backfill: contributing authorized
existing comments and replies helps the shared community start with useful
conversations across both recent titles and the deeper catalogue. Participation must
still be the user's free choice through the consent flow below.

Backfill uses the same write routes as normal activity (`POST …/comments`,
`POST …/replies`). The same optional `createdAt` field is available on normal
comment/reply writes, so migration scripts and live write queues share one
timestamp contract.

---

## Prerequisites

Your API key must have:

| Scope | Purpose |
| ----- | ------- |
| `archive:write` | Required for all comment/reply writes (same as live activity). |

Coordinate large initial or catch-up batches with the archive operator before sending
them. The same write key and timestamp contract also apply to later per-user backfills,
live comments, and retry queues.

---

## What to backfill

**Include** comments and replies that **originated in your app** — text your
users wrote inside your product before archive integration — **only for users whose
affirmative CommsUni sharing consent covers their earlier comments**. This may be one
combined consent for the shared feature and its backfill; it does not need to be a
separate historical-consent step. The consent copy must make clear that sharing
includes the user's existing comments and replies.

**Exclude**:

- Comments your users imported from TV Time (they already exist in the archive
  under `origin.kind: "tvtime"`).
- Comments copied from another partner's source.
- Anything you cannot confidently attribute to your own app's original authorship.
- Rows belonging to a user whose effective sharing decision is `keep_private` or
  `pending`, whose recorded consent does not cover existing comments, or who withdrew
  sharing before the backfill ran. If the partner supports an app-only default for
  future posts, selecting it later does not by itself withdraw consent for rows that
  were already shared.
- Historical likes, spoiler marks, and other engagement. They are separate user
  actions, not content that accompanies a consenting author's comment.

If you cannot distinguish imported vs native rows in your database, stop and
coordinate with the archive operator before backfilling.

---

## Consent and privacy gate

CommsUni publication and author display are related but distinct choices. A product
may offer the following outcomes; app-only posting is optional for the partner:

| Choice | Historical and future comments | Author shown as |
| ------ | ------------------------------ | --------------- |
| **Share publicly with my profile** | Sent to CommsUni; visible across connected apps | The display name and avatar sent by your app |
| **Share publicly with a generated persona** | Sent to the same public, cross-app corpus | A generated name/avatar; the comment text itself is still public and may identify its author |
| **Keep app-only (if supported)** | Kept only by your app; never sent to this API | Whatever your app shows locally |

The second choice hides the partner profile overlay; it does **not** make the
comment text private or guarantee legal anonymization. The API implements the two
shared identity choices with `PUT /v1/authors/me/profile` and
`DELETE /v1/authors/me/profile`. A profile overlay is actor-wide and resolved at
read time, so changing it affects the identity shown on that user's existing and
future shared comments. If offered, app-only is a publication choice implemented
entirely in the partner app; the API does not require partners to support it.

A partner may choose not to support app-only **new** posts. In that product, a user
who does not want to share cancels instead of posting or does not enable the shared
feature. The same sharing prompt may authorize both the feature and backfill, but it
must say that existing comments and replies are included before the user confirms. A
user who declines or dismisses that prompt must not have existing rows published.

### Required user flow

Ask when the CommsUni feature is introduced (for example in a dismissible feature
announcement) or immediately before the user's first new comment would be shared.
Merely opening the app, dismissing the prompt, inactivity, or accepting unrelated
app terms is not permission to backfill. If the prompt is deferred, keep the user's
old comments local and ask again at the next relevant writing or settings action.

The prompt must:

- explain that shared comments are public and cross-app, before the user confirms;
- present two explicit, unselected buttons, such as **“Share my comments”** and
  **“Don't share my comments”**. Invite contribution, but make both actions clear and
  freely selectable. If app-only posting is supported, declining may keep comments
  app-only; otherwise it means not enabling the feature or canceling the post. The
  sharing action may cover both existing and future comments; a separate
  historical-consent button or screen is not required. Dismissing the prompt records
  neither choice;
- describe sharing as a contribution to the community and show the approximate
  number and date range covered when practical. State that enabling sharing also
  covers the user's own existing comments and replies, but not likes or other
  reactions. The explanation may recommend contribution, but sharing and declining
  must both remain clear and actionable;
- after the user enables sharing, ask whether shared comments should use the user's
  public app profile or a generated persona. Do not ask for an identity choice when
  the user declines sharing;
- say that only comments originally written in this app are eligible and that TV
  Time imports will not be duplicated; and
- link to settings where the user can later change profile visibility, choose the
  default for future posts if the partner offers that choice, delete individual
  shared comments, or request account-wide erasure.

Persist a versioned decision record server-side: actor/user id, prompt version, the
sharing decision (`share` or `keep_private`), the shared identity choice when
applicable, the covered comment scope, and timestamp. The scope must record whether
the consent copy covered existing comments and replies. A dismissed or unanswered
prompt remains `pending`, not `keep_private`. Consent to the shared feature authorizes
backfill when the prompt disclosed that existing comments were included; consent to
one new comment without that disclosure does not.

Run backfill for each consenting user after that record exists, not as one blind export
of the partner database. Run the same per-user process when users consent later; the
end of an initial migration batch is not the end of historical backfill. Re-check the
current decision before enqueueing each user; stopping or changing a decision must
prevent unsent rows from being uploaded. A later switch to generated persona changes
display identity through the profile route. If app-only future posts are supported,
switching to that setting does not silently delete comments already shared. Explain
that distinction and expose deletion/erasure controls.

---

## Comment body with `createdAt`

Use the standard comment body documented in the main integration guide, plus an
optional `createdAt`:

```json
{
  "text": "That finale was unhinged",
  "language": "en",
  "createdAt": "2020-05-18T21:04:11Z"
}
```

### Rules

- **`createdAt` is optional.** When omitted, the server uses the current time.
  Strongly prefer sending the true user-action timestamp on every comment and
  reply. This is especially important for offline clients, maintenance windows,
  and retry queues.
- If your live write path relies on server time, any queued or replayed request
  must inject the original user-action timestamp before sending it later.
- **Format:** ISO 8601 timestamp with an explicit `Z` or UTC offset, e.g.
  `2020-05-18T21:04:11Z` or `2020-05-18T23:04:11+02:00`. Naive datetimes such
  as `2020-05-18T21:04:11` are rejected because they depend on server timezone.
- **Range:** on or after `2010-01-01`, and not more than 5 minutes in the future.
  Violations return `400 invalid_created_at`.
- **Replies:** `createdAt` must be **on or after** the comment being replied to
  (the target of `POST /v1/comments/{id}/replies`). Otherwise
  `400 invalid_created_at`. This check uses the parent's stored timestamp. If
  that parent was synced without `createdAt` (server time), a queued reply with
  a user-action `createdAt` can fail even when the user replied after the parent
  offline — send `createdAt` on the whole thread from the first sync, or omit it
  on replies to server-timestamped parents.
- All other comment body rules still apply (`text`, `language`, `mentions`,
  `attachments`, etc.) — see the main integration doc §3.

### What backfill does not restore today

- **Like counts and spoiler marks** from your app's history. Those ledgers use
  server time at write time, and comment-sharing consent does not authorize replaying
  a user's separate engagement actions. If historical engagement is
  supported later, give it its own clear user choice and dependency rules.
- **Comment media.** Attachments at write time follow the normal allowlist rules.
  For images on rows you already posted without media, see the main doc's
  media-grants section — there is no bulk image backfill route yet.
- **Comment IDs.** The archive assigns new UUIDs at write time. Map your local
  ids to archive ids in your migration log; use stable `Idempotency-Key` values
  (below) so retries do not create duplicates.

---

## Script design

### 1. Map authors

Select only authors with a current, versioned sharing record whose scope covers
existing comments and replies.
Each selected comment author needs a stable `X-TVTA-Actor-ID` — the same opaque id you
will use when that user writes live comments later. Derive it server-side from
your authenticated user id; never reuse a shared or guest bucket.

For users who chose public profile display, call
`PUT /v1/authors/me/profile` once per author (with that actor id) before posting
their comments. For users who chose a generated persona, do not create the overlay
(or clear an existing one with `DELETE /v1/authors/me/profile`) before backfill.

### 2. Implement the two backfill triggers

A reply is eligible only when **both** of these are true at send time:

1. The reply's own author currently has a `share` decision covering that reply.
2. The exact comment or reply it answered already exists as a visible archive row.

Implement backfill as two event handlers. Together they cover the initial batch and
users who consent later without repeatedly rescanning every reply in the database.

#### A/B. When a user approves sharing that covers existing comments

Scan all of that user's unpublished, partner-native comments and replies:

- **A — Top-level comments:** enqueue all eligible top-level comments written by the
  user.
- **B — Replies:** enqueue a reply written by the user only if its exact parent is
  already mapped to a visible archive row. If the parent is not published yet, mark
  the reply `waiting_parent`; do not keep polling it.

#### A2. Whenever a row is successfully published

After a top-level comment or reply is successfully published and its local-id →
archive-id mapping is stored, scan all of its **direct** local replies, regardless of
who wrote them:

- enqueue each reply whose author currently has a `share` decision covering it;
- mark each reply by a `pending` or `keep_private` author as
  `waiting_author_consent`.

Publishing an eligible reply runs A2 again for that reply, so approved descendants
are released recursively, parent before child. A descendant cannot be published
through a private or unpublished intermediate reply because its exact parent does not
yet exist in the archive.

These triggers are complementary:

- if the parent is already public when the reply author approves, B releases the
  reply;
- if the reply author approved first, publishing the parent later causes A2 to
  release the reply.

No periodic re-check of B is required. Persist and process both events reliably so a
crash cannot lose an unlock; retrying either handler must be safe. Track at least
`waiting_author_consent`, `waiting_parent`, `eligible`, and `published` (with the
resulting archive id) in the migration log.

Immediately before each POST, re-check the row author's current decision and the
target's visible archive mapping so a withdrawal, deletion, or race does not publish
stale work. For top-level comments, only the author-decision check applies.

If a partner-native parent is still `pending` or `keep_private`, keep the reply
local even when its own author selected Share. If that parent is published later, A2
releases already-approved replies. If the reply author approves only after the parent
is published, B releases it then.
Do not turn the reply into a top-level comment, attach it to a different ancestor,
or create a placeholder containing or describing the private parent.

Reply targets that are **already archived TV Time comments** need no partner-author
consent because the target is already public. `POST …/replies` on the TV Time row id
is supported; supply a `createdAt` after the target's timestamp.

### 3. Sort eligible rows oldest-first

For each entity (episode, movie, etc.):

1. Post top-level comments in ascending `createdAt` order.
2. Post eligible replies only after their exact target exists — parents first,
   then children, still sorted by original time.

### 4. Idempotency

Every backfill POST needs an `Idempotency-Key` header — one key per source
comment, stable across retries within the API idempotency window (e.g.
`backfill:<your-local-comment-id>`).

Replaying the same key with the same body returns the original result with
`Idempotency-Replayed: true` and does not create a duplicate.

The same key with a **different route target or body** (including a different
logical `createdAt` after offset normalization) is `409 idempotency_key_reused`.
Equivalent timestamps in different formats, e.g. `2020-05-18T23:04:11+02:00`
and `2020-05-18T21:04:11.000Z`, hash the same.

The API idempotency window is **24 hours** and is scoped to `(source, actor,
key)`. After that window expires, or if you resend with a different
`X-TVTA-Actor-ID`, the API no longer has the ledger entry and can create a new
comment. Your primary deduplication guard is therefore your migration log:
persist `local comment id -> archive comment id` as each row succeeds, and resume
from that log rather than replaying the export from the top.

### 5. Rate and quota

Each comment/reply costs one `write_unit`, same as live writes. Plan batch size
and schedule accordingly; run large migrations off-peak if possible.

There is no bulk-import endpoint. Throttle your script to stay within your key's
write limits and avoid tripping operator alerts. Quota might be increased during
migration, coordinate on this with the operator.

### 6. Entity references

Address titles by TVDB reference (`tvdb-…-s1e1`, etc.) or by archive `entityId`
if you already resolved them. The first comment on a never-archived TVDB
reference may need an `entity` block (`title`, `showTitle`) — same rules as live
writes.

Use the synthetic test reference `tvdb-900000001-s1e1` only for integration
smoke tests, not for real catalogue backfill.

### 7. Verify and finish

After a batch:

- Spot-check reads (`GET …/comments`) for sort order (`most_recent`) and
  `createdAt` on your source slug.
- Store the mapping from local comment id → archive comment id for support and
  deduplication.
- Tell the operator when the coordinated initial or catch-up batch is complete so any
  temporary quota changes can be restored. Continue running the per-user A/B and A2
  triggers for users who consent later.

### 8. Fixing mistakes

There is no bulk delete. Remove bad backfill rows off-peak with
`DELETE /v1/comments/{id}` using the **original author's**
`X-TVTA-Actor-ID`, one row at a time. See the main doc §10 (pre-send
moderation / cleanup).

---

## Minimal checklist

- [ ] Key has `archive:write`.
- [ ] Every included user has a current, versioned `share` decision whose disclosed
  scope covers existing comments and replies; `keep_private` and `pending` users are
  excluded.
- [ ] Consent uses two explicit, unselected Share/Don't share buttons; sharing is
  encouraged without preselecting or obscuring either choice.
- [ ] Export includes only your app's original comments/replies, not TV Time imports.
- [ ] Stable `X-TVTA-Actor-ID` per author; profile PUT or DELETE matches the user's
  selected shared identity.
- [ ] Sharing consent may cover both existing and future content, but covers the
  user's own comments and replies—not likes, spoiler marks, or other engagement.
- [ ] User approval runs A/B: enqueue the user's top-level comments and only those
  replies whose exact parents are already visible.
- [ ] Every successful publication runs A2: scan its direct replies from all authors,
  enqueue approved ones, and recurse as replies are published.
- [ ] A/B and A2 are durable and idempotent; no periodic B rescan is required.
- [ ] No reply is reparented, converted to top-level, or represented by a placeholder.
- [ ] Eligible rows are sorted oldest-first; parents before replies.
- [ ] Every POST has `Idempotency-Key` and original `createdAt`.
- [ ] Script throttled to write quota; migration log maps local id → archive id.
- [ ] Operator is told when each coordinated batch is complete; later user consent
  still triggers per-user backfill.

---

## Related

- [Partner API integration guide](partner-api-integration.md) — full v1
  contract, live writes, reads, moderation, quotas.
- Operator contact for imported-vs-native ambiguity you cannot resolve locally.
