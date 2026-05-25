# Contributing Connectors to the Werkflow Marketplace

The Werkflow Marketplace is the community-maintained catalog of connector definitions that extend Werkflow's integration surface. Anyone can contribute a connector by submitting a pull request.

---

## What Is a Connector Definition?

A connector definition is a single `connector.json` file that describes how Werkflow connects to an external system. It is transport-agnostic: the same envelope supports REST APIs, PostgreSQL databases, GraphQL, and more. The `spec.transport.type` field selects the adapter.

The schema is versioned at `marketplace/schema/connector-definition.schema.json`. All submissions are validated against it automatically by the CI pipeline.

---

## Directory Layout

```
marketplace/
  connectors/
    werkflow-erp/          ← official connectors (core team only)
      connector.json
    community/             ← community contributions
      your-connector/
        connector.json
  schema/
    connector-definition.schema.json   ← canonical schema (do not edit)
  CONTRIBUTING.md
  README.md
```

Community connectors live under `marketplace/connectors/community/<your-connector-name>/`.

---

## Step-by-Step Contribution Guide

### 1. Fork and clone

Fork this repository on GitHub and clone your fork locally.

### 2. Create your connector directory

```bash
mkdir -p marketplace/connectors/community/my-connector
```

Use a short, lowercase, hyphenated name that reflects the target system.

### 3. Write connector.json

Start from the template below and fill in your connector's details:

```json
{
  "apiVersion": "werkflow.io/connector/v1",
  "kind": "ConnectorDefinition",
  "metadata": {
    "key": "my-connector",
    "displayName": "My Service",
    "description": "A short description of what this connector does.",
    "version": "1.0.0",
    "category": "custom",
    "tags": ["my-service", "community"],
    "vendor": "Vendor Name",
    "license": "Apache-2.0",
    "documentationUrl": "https://docs.example.com/api"
  },
  "spec": {
    "transport": {
      "type": "rest",
      "config": {
        "baseUrl": "https://api.example.com"
      }
    },
    "auth": {
      "profiles": [
        {
          "id": "bearer",
          "displayName": "API Token",
          "type": "bearer"
        }
      ]
    },
    "operations": [
      {
        "id": "exampleOperation",
        "displayName": "Example Operation",
        "description": "What this operation does.",
        "category": "read",
        "input": {
          "type": "object",
          "required": ["id"],
          "properties": {
            "id": { "type": "string" }
          }
        },
        "output": {
          "type": "object",
          "properties": {
            "result": { "type": "string" }
          }
        },
        "transportSpecific": {
          "method": "GET",
          "path": "/v1/items/{id}"
        }
      }
    ],
    "constraints": {
      "defaultTimeoutSeconds": 30
    }
  }
}
```

### 4. Validate locally before pushing

Install `ajv-cli` and run validation against the schema:

```bash
npm install -g ajv-cli ajv-formats
ajv validate --spec=draft2020 \
  -s marketplace/schema/connector-definition.schema.json \
  -d marketplace/connectors/community/my-connector/connector.json \
  --strict=false
```

All validation errors must be resolved before the pull request can be merged.

### 5. Open a pull request

Submit a pull request from your fork targeting the `main` branch of this repository. The CI pipeline runs the schema validation automatically. Address any failures before requesting review.

---

## Connector Quality Guidelines

**Required:**
- `metadata.license` must be a valid SPDX expression (e.g. `Apache-2.0`, `MIT`)
- `metadata.key` must be globally unique in the marketplace — check existing connectors
- All operations must have `id`, `displayName`, and `transportSpecific`
- `auth.profiles[].type` declares only the **supported scheme** (`none`/`basic`/`bearer`/`api-key`) — never embed keys, passwords, tokens, or secret references in the connector definition. The actual credential is bound per-tenant at registration and stored in the secret manager (OpenBao)

**Strongly recommended:**
- Include `metadata.documentationUrl` pointing to the external API reference
- Include `metadata.description` explaining what the connector is and when to use it
- Provide at least one `examples` entry per operation
- Define `errors[]` for common failure codes (rate limits, auth failures, not found)
- Set `constraints.defaultTimeoutSeconds` appropriate to the API's expected latency
- For list operations, configure `pagination` strategy

**Not permitted:**
- Embedding credentials or secrets in the connector definition
- Connectors that target private/internal IP ranges (SSRF guard will reject them at runtime)
- Database connectors with DML operations unless `readOnly: false` is explicitly set and `writeOperation: true` is set on each write query

---

## Auth Types Reference

The `type` declares the **supported scheme** only — no secret material lives in the definition. The credential VALUE is bound per-tenant at registration and stored in OpenBao under the tenant's `credentialRef`, then resolved server-side at call time.

| Type | Use case | Definition declares | Credential fields (in OpenBao, per tenant) |
|---|---|---|---|
| `none` | Public APIs, or datasource-managed credentials (DB connectors) | — | — |
| `basic` | HTTP Basic Auth | `type` only | `username`, `password` |
| `bearer` | OAuth tokens, PATs, JWT | `type` only | `token` |
| `api-key` | API key in a header | `type`, `config.headerName` | `headerName`, `headerValue` |

> Removed 2026-05-25: `secret-ref` and `oauth2-client-credentials` (never functional) and `mtls` (unbuilt — needs a per-tenant client keystore). They are no longer valid `type` values.

---

## Transport Types Reference

| Type | Use case |
|---|---|
| `rest` | HTTP/HTTPS APIs |
| `database` | JDBC-compatible databases (PostgreSQL, MySQL, etc.) |
| `graphql` | GraphQL APIs |
| `webhook` | Inbound webhook receivers |
| `mock` | Testing and development |

---

## Getting Help

Open a [GitHub Discussion](https://github.com/werkflow-platform/werkflow-public/discussions) if you have questions about the connector format or need guidance on authoring a specific integration.
