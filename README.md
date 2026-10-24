# telemed-ia-worker

> Asynchronous jobs and background processing for TeleMed-IA

Part of the **TeleMed-IA** distributed system — team `code-corhuila`, Grupo 2.
Governance and documentation live in [`telemed-ia-docs`](https://github.com/code-corhuila/telemed-ia-docs).

## Responsibilities
- Consume `PostConsultationSummaryGenerated` from RabbitMQ.
- Request a service token from `identity-and-access-api`.
- Call `document-generation-api` to trigger PDF generation.

## Branching
Three permanent branches. **None of them accepts a direct commit**.
