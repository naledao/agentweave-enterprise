# AgentWeave Enterprise

AgentWeave Enterprise is an enterprise AI agent orchestration platform built with Spring Boot and Vue. It turns large-model capabilities into practical internal workflows by combining knowledge retrieval, GraphRAG, tool calling, task planning, permission control, audit logging, and operations-grade observability.

## Live Demo

- URL: http://14.103.202.40
- Account: `admin`
- Password: `admin123`

## Current Status

The project is a runnable end-to-end demo with a Spring Boot backend, a Vue operations console, database migrations, document processing, AI chat, tool execution, workflow orchestration, and observability screens.

Implemented capabilities include:

- JWT login, user management, role management, route-level permissions, and backend permission checks.
- Multi-turn AI conversations with persisted messages, SSE streaming, cancellation, timeout handling, trace IDs, citations, graph paths, tool-call events, and workflow-step events.
- Knowledge document upload, parsing, cleaning, chunking, metadata tagging, MinIO object storage, PostgreSQL/pgvector indexing, and GraphRAG indexing.
- Hybrid retrieval that combines Vector RAG and GraphRAG results, returns source citations and graph paths, and records retrieval traces.
- Tool center with tool definitions, ticket query, log search, endpoint status query, permission checks, risk handling, rate limiting, timeouts, and invocation records.
- Agent workflow runs powered by Planner, Executor, and Reviewer roles, with LangGraph4j state orchestration, checkpoints, retries, human approvals, tool execution, citations, graph paths, and timelines.
- Observability dashboard for model calls, RAG retrieval, GraphRAG indexing/retrieval, tool invocations, workflow metrics, SSE connections, audit logs, and health indicators.
- Frontend console pages for chat, conversations, knowledge documents, tools, tool invocation history, workflow runs, observability, users, and roles.

## Architecture

```text
frontend/  Vue 3 operations console
backend/   Spring Boot API, AI, workflow, tool, RAG, and observability service
```

Backend modules are organized around business capabilities:

- `auth` - users, roles, permissions, JWT authentication, and bootstrap admin data.
- `conversation` - chat APIs, SSE streaming, message persistence, model-call logging, and RAG context assembly.
- `knowledge` - document lifecycle, object storage, parsing, chunking, indexing, and RabbitMQ document pipeline.
- `springai.rag` - vector retrieval, metadata filters, query routing, retrieval planning, and retrieval logs.
- `graphrag` - entity/relationship extraction, Neo4j integration, graph-path retrieval, and GraphRAG logs.
- `tool` - tool definitions, security checks, demo tools, invocation persistence, and business tool adapters.
- `workflow` - agent runs, steps, checkpoints, approvals, LangGraph4j graph execution, and workflow tool execution.
- `langchain4j` - Planner, Executor, Reviewer, tool bindings, prompt templates, and token observation.
- `observability` - dashboards, query services, health indicators, Micrometer metrics, and SSE connection tracking.
- `shared` - exceptions, security support, tracing, audit AOP, and common infrastructure.

## Tech Stack

Backend:

- Java 17
- Spring Boot 3.5.14
- Spring Security + JWT
- Spring AI ChatClient
- PostgreSQL + pgvector
- RabbitMQ
- Redis
- MinIO
- Neo4j GraphRAG integration
- LangChain4j
- LangGraph4j
- Flyway
- Actuator + Micrometer + Prometheus metrics

Frontend:

- Vue 3 + TypeScript
- Vite
- Vue Router
- Pinia
- TanStack Query for Vue
- Element Plus
- Axios
- SSE client handling
- ECharts
- VueUse
- Vitest
- Playwright

## Main Screens

- `/app/chat` - AI chat workspace with streaming responses, citations, graph paths, RAG trace panels, tool calls, and workflow step events.
- `/app/conversations` - conversation history and conversation-level management.
- `/app/knowledge/documents` - document list, upload flow, processing state, chunks, citations, and GraphRAG indexing state.
- `/app/tools` - tool definition center.
- `/app/tools/invocations` - tool invocation list, summary metrics, details, input/output payloads, and trace information.
- `/app/workflows/runs` - workflow run list, creation dialog, status, retry/resume/cancel flows, and detailed timelines.
- `/app/observability` - model, RAG, GraphRAG, tool, workflow, SSE, audit, and health dashboards.
- `/app/settings/users` and `/app/settings/roles` - user, role, and permission management.

## Backend APIs

The backend exposes versioned REST APIs under `/api/v1`, including:

- Authentication and current-user APIs.
- User, role, and permission management APIs.
- Conversation and SSE chat APIs.
- Document upload, document query, detail, delete, and reindex APIs.
- Vector RAG and GraphRAG retrieval/index APIs.
- Tool definition and tool invocation APIs.
- Workflow run, step, checkpoint, retry, resume, cancel, and plan preview APIs.
- Observability APIs for model calls, audit logs, RAG retrieval logs, GraphRAG logs, tool metrics, and summary dashboards.

Actuator endpoints are exposed for `health`, `info`, `metrics`, and `prometheus`. Health readiness includes database, Redis, MinIO, model provider, vector store, and Neo4j checks.

## Database Migrations

Schema changes are managed with Flyway in `backend/src/main/resources/db/migration`. The current migration set covers authentication, conversations, model call logs, documents, pgvector storage, GraphRAG tables, RabbitMQ document consumption logs, tool definitions, tool invocations, workflow runs/steps/checkpoints/approvals, audit enhancements, model/RAG/GraphRAG/tool/workflow observability, and correlation fields.

## Configuration

Runtime configuration is environment-variable driven. Important variables include:

```bash
AGENTWEAVE_OPENAI_BASE_URL
AGENTWEAVE_OPENAI_API_KEY
AGENTWEAVE_DATASOURCE_URL
AGENTWEAVE_DATASOURCE_USERNAME
AGENTWEAVE_DATASOURCE_PASSWORD
AGENTWEAVE_REDIS_HOST
AGENTWEAVE_RABBITMQ_HOST
AGENTWEAVE_RABBITMQ_USERNAME
AGENTWEAVE_RABBITMQ_PASSWORD
AGENTWEAVE_MINIO_ENDPOINT
AGENTWEAVE_MINIO_ACCESS_KEY
AGENTWEAVE_MINIO_SECRET_KEY
AGENTWEAVE_GRAPHRAG_NEO4J_BASE_URL
AGENTWEAVE_GRAPHRAG_NEO4J_USERNAME
AGENTWEAVE_GRAPHRAG_NEO4J_PASSWORD
AGENTWEAVE_JWT_SECRET
AGENTWEAVE_ADMIN_PASSWORD
```

The default chat model is configured as `mimo-v2.5` through the OpenAI-compatible Spring AI configuration. The default embedding provider is Ollama-compatible and uses `qwen3-embedding:0.6b`.

## Getting Started

### Backend

Requirements:

- Java 17+
- PostgreSQL with pgvector
- RabbitMQ
- Redis
- MinIO
- Neo4j
- OpenAI-compatible chat model endpoint
- Ollama-compatible embedding endpoint

Create a local environment file:

```bash
cp backend/.env.example backend/.env
```

Update the values in `backend/.env`, then start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

The backend listens on `http://localhost:8080` by default.

### Frontend

Requirements:

- Node.js 20+
- npm

Install dependencies and start the dev server:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` to the backend.

Useful local commands:

```bash
cd frontend
npm run typecheck
npm run test:unit
npm run test:e2e
npm run build
```
