# 7. Module boundaries and data ownership

- Status: accepted
- Date: 2026-11-19

## Context

Jobs, per-user evaluations and proposals had to be split between modules before any feature code exists. Three
facts shape the split: the same API job is relevant to many users, data read through the browser extension comes
from one user's own session, and the product may later serve agencies with several freelancers.

## Decision

- `job` is a **catalog** without per-user data. A new `matching` module owns per-workspace filters, pre-filtering
  and fit scores, and re-scores when the profile or filters change.
- Jobs from official APIs are **public** (one shared copy); jobs captured by the extension are **private** to the
  capturing workspace.
- User data is owned by a **workspace** (`WorkspaceId`), not by a user. Each user has one workspace for now; the
  `identity` module owns both.
- The product scope ends at the **proposal outcome** (won / lost). Managing won projects stays on the platforms.
- `platform` is the only module with platform-specific knowledge; proposal status changes reach `proposal` as events.

## Consequences

- API jobs are fetched and stored once regardless of the number of users; scoring cost stays per workspace.
- Extension data is never redistributed, which keeps the extension within platform terms.
- Team or agency accounts need new membership rules in `identity`, but no data migration.
- Project management after a win would need a new module and an ADR.
