# React and TypeScript Project Profile

## Applies to

Use this profile for React applications written in TypeScript.

This includes projects using frameworks and libraries such as:

- Vite
- React Router
- TanStack Router
- Next.js client components
- Zustand
- Redux Toolkit
- React Query or TanStack Query

Follow the technologies already used by the repository.

Do not add or replace frameworks merely because another tool is preferred.

## SmartLab notes

For this repository, the frontend application lives in `frontend/` and uses React, TypeScript, Vite, TanStack Router/Start, Tailwind CSS, and the existing UI components. Run frontend commands with `npm --prefix frontend ...` from the repository root, or from inside `frontend/`.

The frontend development server is expected on port `5173`.

Do not modify frontend source for documentation-only or backend-only tasks.

## Project inspection

Before editing, inspect:

```text
package.json
tsconfig.json
vite.config.*
next.config.*
src/
package-lock.json
pnpm-lock.yaml
yarn.lock
bun.lock

Determine:

The package manager.
The build tool.
The routing library.
The state-management approach.
The API client.
The component library.
The styling system.
The existing testing framework.

Do not assume the project uses Vite, Tailwind, React Router, or another library without checking.

Package manager

Use the package manager identified by the lockfile:

package-lock.json -> npm
pnpm-lock.yaml    -> pnpm
yarn.lock         -> yarn
bun.lock          -> bun

Do not:

Create a second lockfile.
Switch package managers without approval.
Delete the existing lockfile.
Run a different package manager merely because it is installed globally.
Architecture

Inspect and preserve the existing project architecture.

A common dependency direction is:

Route
    -> Feature
    -> Components
    -> Hooks or State
    -> API Client
    -> Backend

This is guidance, not a requirement for every project.

Do not introduce unnecessary layers into a small assignment.

Route rules

Route components should:

Define page-level behavior.
Read route parameters.
Invoke page-level data loading.
Compose feature components.
Handle route-level errors and redirects.

Route components should not:

Contain large reusable UI sections.
Duplicate API client logic.
Contain unrelated domain workflows.
Define repeated formatting helpers.
Implement backend authorization.

Keep route files reasonably thin when the current architecture supports it.

Component rules

Components should:

Have a clear responsibility.
Receive explicit props.
Reuse existing design-system elements.
Remain easy to test and understand.
Handle relevant visual states.

Do not:

Create a component for every small HTML element.
Create unnecessary wrapper components.
Duplicate an existing component with minor styling differences.
Put unrelated data-fetching logic inside presentation-only components.
Build speculative components for possible future reuse.

Extract a component when it improves:

Reuse.
Readability.
Testability.
Responsibility separation.

Do not extract merely to reduce file length.

TypeScript rules

Preserve strict typing when configured.

Do not use:

any
@ts-ignore
@ts-nocheck

unless necessary and explicitly justified.

Prefer:

Existing domain types.
Explicit function return types for public utilities.
Narrow union types.
Type guards.
Typed API responses.
Typed component props.
Explicit handling of nullable values.

Do not:

Duplicate the same role, status, or domain type in multiple files.
Cast values merely to silence the compiler.
Use broad type assertions without validating runtime data.
Change strictness settings to make errors disappear.
Replace useful types with string.

When data comes from an external API, remember that TypeScript types do not validate runtime values automatically.

React state rules

Before adding state, determine whether the value can be:

Calculated directly from props.
Derived during rendering.
Stored in the URL.
Managed by an existing state store.
Loaded by an existing query library.

Do not create state for values that can be derived safely.

Avoid duplicated state that can become inconsistent.

Use local component state for local UI behavior.

Use shared state only when multiple unrelated components genuinely need the same state.

Do not introduce Redux, Zustand, Context, or another state library without necessity and approval.

Hook rules

Follow the Rules of Hooks.

Do not:

Call hooks conditionally.
Call hooks inside loops.
Call hooks inside ordinary helper functions.
Suppress exhaustive dependency warnings without understanding them.
Create effects to synchronize values that can be derived directly.
Use an effect as a substitute for an event handler.

Use useEffect only for synchronization with an external system, such as:

Browser APIs.
Network subscriptions.
Timers.
Non-React widgets.
External storage.
Event listeners.

Clean up subscriptions, timers, and listeners.

API rules

Reuse the project's existing API client.

Do not:

Duplicate fetch or Axios configuration across pages.
Hardcode backend base URLs when configuration already exists.
Ignore non-successful HTTP responses.
Assume every response contains valid data.
Store access tokens insecurely without following project conventions.
implement authorization only in frontend code.

Handle relevant responses:

Success.
Validation failure.
Unauthorized.
Forbidden.
Not found.
Conflict.
Server error.
Network failure.

Keep request and response types consistent with the backend contract.

Authentication and authorization

Frontend role checks control presentation only.

They do not replace backend authorization.

Do not trust:

User IDs supplied by the browser.
Role values stored only in UI state.
Hidden buttons.
Disabled controls.
Client-side route guards.

Protected operations must still be verified by the backend.

Preserve the project's existing authentication and session strategy.

Do not weaken authentication or bypass route protection to make a feature work.

Form rules

Forms should handle:

Initial values.
Required values.
Invalid values.
Submission state.
Server validation errors.
Successful submission.
Unexpected server failures.

Do not clear valid user input after a failed submission unless explicitly required.

Prevent duplicate submission while a request is in progress when appropriate.

Keep frontend validation consistent with backend rules, but do not rely only on frontend validation.

Do not invent validation ranges that are not defined by requirements.

Data loading states

Every changed data-driven screen should consider:

Initial loading.
Background refresh.
Empty results.
Successful results.
Validation error.
Server error.
Network error.
Unauthorized.
Forbidden.
Not found.

Do not render an empty white screen when data is loading or failed.

Do not show an error state as an empty state.

UI rules

Preserve the existing design system.

Reuse:

Existing components.
Existing spacing.
Existing colors.
Existing typography.
Existing icons.
Existing interaction patterns.
Existing responsive breakpoints.

Do not:

Add a UI library without approval.
Mix multiple design systems.
Redesign unrelated pages.
Add decorative effects that harm readability.
Hardcode inconsistent colors repeatedly.
Use placeholder data in production paths without clearly marking it.

Changed screens should support:

Long text.
Small screens.
Keyboard navigation.
Visible focus.
Disabled states.
Error messages.
Empty states.
Loading states.
Permission-denied states.
Accessibility

Use semantic HTML where possible.

Check:

Labels associated with form controls.
Button elements for actions.
Anchor elements for navigation.
Keyboard access.
Visible focus indicators.
Meaningful image alternative text.
Proper heading order.
Sufficient readable text.
Error messages associated with fields.
Accessible names for icon-only buttons.

Do not use clickable <div> elements when a button or link is appropriate.

Styling

Follow the styling system already used by the project:

Plain CSS.
CSS Modules.
Tailwind CSS.
Styled Components.
Emotion.
Component-library tokens.

Do not introduce a second styling approach without approval.

Avoid large inline style objects when the project has an established styling system.

Do not rewrite unrelated CSS during a functional change.

Routing

Preserve the existing router.

Do not install or replace routing libraries without approval.

When changing routes, check:

Route path.
Route parameters.
Search parameters.
Navigation links.
Redirect behavior.
Authentication guards.
Not-found behavior.
Direct browser access.
Browser back and forward navigation.

Do not assume client-side navigation is the only way a page will be opened.

Error handling

Do not silently swallow errors.

Show user-facing errors where appropriate.

Log errors only through the project's established mechanism.

Do not expose:

Tokens.
Passwords.
Internal stack traces.
Sensitive server details.
Personal data not required by the screen.

Keep developer diagnostics separate from user-facing messages.

Dependencies

Do not add, remove, or upgrade dependencies unless the active task requires it.

Before proposing a dependency, explain:

The requirement it solves.
Why existing project tools are insufficient.
Bundle-size impact.
Maintenance impact.
Security implications.
Browser compatibility implications.

Do not add a library merely to implement a small utility that the project or platform already supports.

Testing

Use the testing approach already configured by the project.

Possible tools include:

Vitest
Jest
React Testing Library
Playwright
Cypress

Do not claim a testing tool exists without checking project configuration.

Test relevant behavior such as:

Rendering.
Loading.
Empty results.
User interactions.
Validation.
Successful submission.
Server error.
Permission restrictions.
Navigation.
Regression behavior for fixed bugs.

Do not test implementation details when user-visible behavior can be tested instead.

Verification

First inspect available scripts:

cat package.json

For an npm project, run available commands such as:

npm run lint
npm run test --if-present
npm run build

For pnpm:

pnpm lint
pnpm test --if-present
pnpm build

For Yarn:

yarn lint
yarn test
yarn build

For Bun:

bun run lint
bun test
bun run build

Only run scripts that exist in the current project.

Also run:

git diff --check

Inspect:

git status
git diff

Do not claim completion when:

TypeScript compilation fails.
Lint fails.
Required tests fail.
The production build fails.
Unrelated files were modified.
Acceptance criteria are not satisfied.
