# helper-730 – frontend

React + TypeScript + Vite. Talks to the backend only through the REST API (`/api/v1`).

```bash
npm install        # once
npm run dev        # http://localhost:5173, /api is forwarded to http://localhost:8080
npm run build      # type check + production build in dist/
```

Start the backend first (`docker compose up -d db backend`, or `mvn spring-boot:run`).

## Structure

| Path              | Content                                                                                   |
| ----------------- | ----------------------------------------------------------------------------------------- |
| `src/api.ts`      | One function per endpoint; errors become `ApiError` with the translated `detail`          |
| `src/types.ts`    | Shapes of the JSON returned by the backend                                                |
| `src/i18n.tsx`    | Interface texts in Italian and English; the backend translates its own messages           |
| `src/format.ts`   | Display formatting of amounts, quantities, percentages and dates                          |
| `src/useLoad.ts`  | Loads data and reloads it when the inputs change                                          |
| `src/pages/`      | One component per tab of the sidebar                                                      |
| `src/components/` | Notice, confirmation dialog, donut chart                                                  |
| `src/styles.css`  | Single light theme, palette of the brief, Neuton font (bundled from `@fontsource/neuton`) |
