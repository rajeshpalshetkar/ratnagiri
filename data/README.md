# Ratnagiri Police weekly data refresh

- Source: https://ratnagiripolice.gov.in/
- Scope: police station IDs 37–55 (19 stations).
- Schedule: every Sunday at 06:00 IST (00:30 UTC), plus manual run via GitHub Actions.
- Outputs: `data/police_stations.xlsx` and `data/police_stations.json`.

The collector extracts selected fields from the official station page HTML. It keeps previously stored values when a fresh page does not expose a field, and records the latest check status. An extraction result is not an official verification. Review records against the source before relying on them.
