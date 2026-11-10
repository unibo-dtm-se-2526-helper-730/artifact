# helper-730 – frontend

React + TypeScript + Vite single-page app. Talks to the backend only through the REST API
(`/api/v1`); in production it is served by nginx, which forwards `/api` to the backend.

```bash
npm ci                           # once
npm run dev                      # http://localhost:5173, /api is forwarded to http://localhost:8080
npm run build                    # type check + production build in dist/
```

Start the backend first (`docker compose up -d db backend` from the root, or
`./mvnw spring-boot:run` in `backend/`).

## Structure

| Path              | Content                                                                                          |
| ----------------- | ------------------------------------------------------------------------------------------------ |
| `src/api.ts`      | One function per endpoint; errors become `ApiError` with the translated `detail`                 |
| `src/types.ts`    | Shapes of the JSON returned by the backend                                                       |
| `src/i18n.tsx`    | Interface texts in Italian and English; the backend translates its own messages                  |
| `src/format.ts`   | Display formatting of amounts, quantities, percentages and dates                                 |
| `src/useLoad.ts`  | Loads data and reloads it when the inputs change                                                 |
| `src/pages/`      | One component per tab: import, transactions, instruments, summary, IVAFE, declaration, portfolio |
| `src/components/` | Notice, confirmation dialog, donut chart                                                         |
| `src/styles.css`  | Single light theme, Neuton font (bundled from `@fontsource/neuton`)                              |
| `test/`           | Vitest + Testing Library tests, separated from the sources                                       |

## Tests and checks

```bash
npm test                         # unit and component tests (jsdom)
npm run coverage                 # coverage report in coverage/index.html
npm run typecheck                # tsc
npm run lint                     # ESLint
npm run format:check             # Prettier (npm run format to fix)
```

The tests replace `fetch` or the `api` module with mocks, so they do not need the backend.

## Docker

`Dockerfile` builds the app and serves `dist/` with nginx (`nginx.conf`), which expects the
backend at `http://backend:8080`, the service name in `docker-compose.yml`.
