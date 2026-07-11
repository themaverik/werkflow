# Werkflow Marketplace

The Werkflow Marketplace is the official catalog of connector definitions for the Werkflow platform. Connectors describe how Werkflow workflows connect to external systems — REST APIs, databases, messaging systems, and more.

---

## Catalog

### Official Connectors (Core Team)

| Connector | Transport | Auth | Description |
|---|---|---|---|
| [werkflow-erp](connectors/werkflow-erp/connector.json) | REST | API Key | Werkflow ERP — HR, Procurement, Inventory, Finance |

### Community Connectors

| Connector | Transport | Auth | Description |
|---|---|---|---|
| [slack-web-api](connectors/community/slack/connector.json) | REST | Bearer | Slack Web API — post messages, list channels |
| [github-rest-api](connectors/community/github/connector.json) | REST | Bearer | GitHub REST API — issues, repos, pull requests |
| [postgres-readonly](connectors/community/postgres-readonly/connector.json) | Database | Datasource | PostgreSQL read-only access via named queries |
| [openai-chat](connectors/community/openai-chat/connector.json) | REST | Bearer | OpenAI Chat Completions — GPT-4o, GPT-4, GPT-3.5-turbo |

---

## How Connectors Work

A connector definition is a `connector.json` file that follows the `ConnectorDefinition` envelope schema (`apiVersion: werkflow/connector/v1`). The file describes:

- **Transport** — how to reach the external system (REST, database, GraphQL, ...)
- **Auth** — which auth profiles are supported (API key, bearer token, OAuth2, ...)
- **Operations** — the set of named operations that BPMN process designers can select
- **Input/Output schemas** — drive the process designer's form fields and output mappings
- **Constraints** — timeouts, rate limits, and response size caps

When a workflow hits a service task that uses a connector, the engine resolves the connector definition, selects the appropriate transport adapter, and executes the operation — with credentials drawn from the tenant's secret store, never from the connector definition itself.

---

## Installing a Connector

From the Werkflow Portal, navigate to **Admin → Marketplace**. Each connector card shows an Install button that pre-fills the connector registration form. You supply the runtime credential (API key, token, etc.) and the connector becomes available in the BPMN process designer.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for a step-by-step guide to submitting a new connector.

**Quick summary:**
1. Fork this repository
2. Create `marketplace/connectors/community/<your-connector>/connector.json`
3. Validate locally: `ajv validate --spec=draft2020 -s marketplace/schema/connector-definition.schema.json -d your/connector.json --strict=false`
4. Open a pull request — CI validates automatically

---

## Schema

The schema is at [`marketplace/schema/connector-definition.schema.json`](schema/connector-definition.schema.json) — JSON Schema 2020-12. It is **generated** and kept in sync with the platform's runtime validator (see [`schema/README.md`](schema/README.md)); do not edit it here — propose changes via issue/PR.

---

## License

All official connectors are licensed under Apache-2.0. Community connectors declare their own `metadata.license` (SPDX expression). See individual `connector.json` files.
