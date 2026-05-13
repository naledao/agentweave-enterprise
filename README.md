# AgentWeave Enterprise

AgentWeave Enterprise is an enterprise AI agent orchestration platform built around Spring Boot and Vue. It connects conversational AI, knowledge retrieval, tool calling, task execution, permission control, and observability into a reusable internal operations console.

## Architecture

- `backend/` - Spring Boot 3.x service with JWT authentication, REST APIs, SSE streaming chat, RAG document ingestion, GraphRAG indexing, audit logging, and observability hooks.
- `frontend/` - Vue 3, TypeScript, Vite, Vue Router, Pinia, TanStack Query for Vue, Element Plus, SSE client handling, and test coverage.

## Core Capabilities

- User login and JWT-based authorization.
- Document upload, parsing, cleaning, chunking, vector indexing, and GraphRAG indexing.
- Multi-turn knowledge-base chat with SSE streaming responses.
- Citations, graph paths, tool call records, workflow steps, and trace IDs surfaced to the UI.
- Role, permission, user, conversation, document, and settings management views.
- Flyway-managed database migrations and focused backend/frontend tests.

## Backend

Requirements:

- Java 17+
- Maven Wrapper from `backend/mvnw`
- PostgreSQL with pgvector
- RabbitMQ
- MinIO
- Neo4j, optional for GraphRAG
- An OpenAI-compatible chat model endpoint
- Ollama, optional for local embeddings

Create local environment variables from the sample file:

```bash
cp backend/.env.example backend/.env
```

Set real values for secrets before running locally:

- `AGENTWEAVE_OPENAI_API_KEY`
- `AGENTWEAVE_DATASOURCE_PASSWORD`
- `AGENTWEAVE_RABBITMQ_PASSWORD`
- `AGENTWEAVE_JWT_SECRET`
- `AGENTWEAVE_ADMIN_PASSWORD`
- `AGENTWEAVE_MINIO_ACCESS_KEY`
- `AGENTWEAVE_MINIO_SECRET_KEY`
- `AGENTWEAVE_GRAPHRAG_NEO4J_PASSWORD`

Run the backend:

```bash
cd backend
./mvnw spring-boot:run
```

Run backend tests:

```bash
cd backend
./mvnw test
```

## Frontend

Requirements:

- Node.js 20+
- npm

Install and run:

```bash
cd frontend
npm install
npm run dev
```

Useful checks:

```bash
npm run typecheck
npm run test:unit
npm run build
```

## Security Notes

This repository intentionally keeps runtime secrets out of committed configuration. Use environment variables, a local `.env` file, or a secret manager for credentials and model provider keys.
