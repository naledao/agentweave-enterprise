# AgentWeave Enterprise

AgentWeave Enterprise is an enterprise AI agent orchestration platform built with Spring Boot and Vue. It focuses on turning large-model capabilities into practical business workflows: knowledge retrieval, tool calling, task planning, permission control, execution tracing, and operations-grade observability.

## Live Demo

- URL: http://14.103.202.40
- Account: `admin`
- Password: `admin123`

## What It Does

- Provides JWT-based login, role management, user management, and permission-controlled console routes.
- Supports multi-turn AI conversations with SSE streaming, request tracing, persisted messages, and graceful cancellation/timeout handling.
- Builds a knowledge base from uploaded documents through parsing, cleaning, chunking, metadata tagging, vector indexing, and GraphRAG indexing.
- Combines Vector RAG and GraphRAG retrieval so answers can include citations, graph paths, and retrieval trace information.
- Exposes tool calling for business operations such as ticket queries, log search, and endpoint status checks, with permission checks and invocation audit records.
- Uses Planner, Executor, and Reviewer style agent roles for complex task decomposition and result review.
- Orchestrates longer-running agent workflows with stateful steps, retries, checkpoints, approval records, and execution timelines.
- Provides an operations console for chat, conversations, knowledge documents, tools, tool invocation records, workflows, users, and roles.

## Tech Stack

Backend:

- Java 17
- Spring Boot 3.5
- Spring Security + JWT
- Spring AI ChatClient
- PostgreSQL + pgvector
- RabbitMQ
- MinIO
- Neo4j GraphRAG integration
- LangChain4j
- LangGraph4j
- Flyway
- Actuator + Micrometer

Frontend:

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- TanStack Query for Vue
- Element Plus
- Axios
- SSE client handling
- Vitest
- Playwright

## Project Structure

```text
backend/   Spring Boot backend service
frontend/  Vue operations console
```

## Getting Started

### Backend

Requirements:

- Java 17+
- PostgreSQL with pgvector
- RabbitMQ
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

Run backend tests:

```bash
cd backend
./mvnw test
```

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

Useful checks:

```bash
npm run typecheck
npm run test:unit
npm run test:e2e
npm run build
```
