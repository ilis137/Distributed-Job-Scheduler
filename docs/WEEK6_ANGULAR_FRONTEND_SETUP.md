# Week 6: Angular Frontend Setup

**Date**: 2026-03-14  
**Status**: ✅ **COMPLETE** (Task 1: Angular Project Setup)

---

## Overview

Implemented the **Angular 17 Frontend** for the Distributed Job Scheduler. This provides a modern, responsive web UI for managing jobs, viewing execution history, and monitoring cluster status.

---

## Task 1: Angular Project Setup ✅

### Components Implemented

#### 1. **Project Structure**
Created a complete Angular 17 project with the following structure:

```
scheduler-ui/
├── src/
│   ├── app/
│   │   ├── core/                    # Core services, models, interceptors
│   │   │   ├── models/              # TypeScript interfaces (DTOs)
│   │   │   │   ├── job.model.ts
│   │   │   │   ├── job-execution.model.ts
│   │   │   │   └── cluster.model.ts
│   │   │   ├── services/            # API services
│   │   │   │   ├── job.service.ts
│   │   │   │   ├── job-execution.service.ts
│   │   │   │   └── cluster.service.ts
│   │   │   └── interceptors/        # HTTP interceptors
│   │   │       ├── api.interceptor.ts
│   │   │       └── error.interceptor.ts
│   │   ├── features/                # Feature modules
│   │   │   ├── jobs/                # Job management components
│   │   │   │   ├── job-list/
│   │   │   │   ├── job-form/
│   │   │   │   └── job-detail/
│   │   │   └── cluster/             # Cluster monitoring components
│   │   │       └── cluster-status/
│   │   ├── app.component.*          # Root component
│   │   ├── app.config.ts            # Application configuration
│   │   └── app.routes.ts            # Routing configuration
│   ├── environments/                # Environment configurations
│   │   ├── environment.ts           # Development config
│   │   └── environment.prod.ts      # Production config
│   ├── assets/                      # Static assets
│   ├── styles.scss                  # Global styles
│   ├── index.html                   # HTML entry point
│   ├── main.ts                      # TypeScript entry point
│   └── proxy.conf.json              # Proxy configuration for dev server
├── angular.json                     # Angular CLI configuration
├── package.json                     # npm dependencies
├── tsconfig.json                    # TypeScript configuration
└── README.md                        # Project documentation
```

#### 2. **Technology Stack**
- **Angular 17.3.0** - Latest Angular with standalone components
- **Angular Material 17.3.0** - Material Design UI components
- **RxJS 7.8.0** - Reactive programming
- **TypeScript 5.4.2** - Type-safe JavaScript
- **SCSS** - Styling with Sass

#### 3. **Environment Configuration**
Created environment files for different deployment scenarios:

**Development (`environment.ts`)**:
- API Base URL: `http://localhost:8080/api/v1`
- Polling Interval: 5 seconds
- Debug Logs: Enabled

**Production (`environment.prod.ts`)**:
- API Base URL: `/api/v1` (relative URL)
- Polling Interval: 10 seconds
- Debug Logs: Disabled

#### 4. **Proxy Configuration**
Created `proxy.conf.json` to avoid CORS issues during development:
- Proxies `/api` requests to `http://localhost:8080`
- Enables seamless local development without CORS errors

#### 5. **HTTP Interceptors**

**API Interceptor** (`api.interceptor.ts`):
- Automatically adds base URL to all API requests
- Adds common headers (`Content-Type`, `Accept`)
- Logs requests in development mode

**Error Interceptor** (`error.interceptor.ts`):
- Handles HTTP errors globally
- Provides user-friendly error messages
- Distinguishes between client-side and server-side errors
- Special handling for connection errors (status 0)

#### 6. **TypeScript Models (DTOs)**
Created TypeScript interfaces matching backend DTOs:

**Job Models** (`job.model.ts`):
- `Job` - Job response DTO
- `JobStatus` - Enum matching backend
- `CreateJobRequest` - Create job request
- `UpdateJobRequest` - Update job request
- `JobListResponse` - Paginated job list

**Job Execution Models** (`job-execution.model.ts`):
- `JobExecution` - Execution response DTO
- `ExecutionStatus` - Enum matching backend
- `ExecutionHistoryResponse` - Paginated execution history

**Cluster Models** (`cluster.model.ts`):
- `ClusterStatus` - Cluster status response
- `NodeStatus` - Node status response
- `NodeState` - Enum matching backend

#### 7. **API Services**

**JobService** (`job.service.ts`):
- `getJobs(page, size, status)` - List jobs with pagination
- `getJobById(id)` - Get job details
- `createJob(request)` - Create new job
- `updateJob(id, request)` - Update existing job
- `deleteJob(id)` - Delete job
- `pauseJob(id)` - Pause job
- `resumeJob(id)` - Resume job
- `cancelJob(id)` - Cancel job
- `triggerJob(id)` - Trigger job immediately

**JobExecutionService** (`job-execution.service.ts`):
- `getExecutionById(id)` - Get execution details
- `getJobExecutionHistory(jobId, page, size)` - Get execution history for a job
- `getAllExecutions(page, size, status)` - Get all executions with pagination

**ClusterService** (`cluster.service.ts`):
- `getClusterStatus()` - Get cluster status
- `getLeaderNodeId()` - Get current leader node ID

---

## Features Implemented

### 1. **Job List Component** (`job-list`)
- Displays all jobs in a Material table
- Pagination support (10, 20, 50, 100 items per page)
- Status badges with color coding
- Actions menu for each job (View, Edit, Trigger, Pause/Resume, Delete)
- Empty state when no jobs exist
- Loading spinner during API calls
- Error message display

### 2. **Job Form Component** (`job-form`)
- Create and edit jobs
- Form validation with Angular Reactive Forms
- Fields: name, description, cron expression, enabled, max retries, timeout
- Cancel and submit buttons
- Error handling

### 3. **Job Detail Component** (`job-detail`)
- View job details
- Display execution history table
- Actions: Edit, Trigger, Delete
- Status badges
- Formatted dates and times

### 4. **Cluster Status Component** (`cluster-status`)
- Overview cards showing total nodes, active nodes, leader node, current epoch
- Node details table with status, role, epoch, last heartbeat, uptime
- Auto-refresh every 5 seconds
- Color-coded status indicators
- Leader node highlighted with star icon

### 5. **Root App Component**
- Material toolbar with app title
- Side navigation drawer with menu items (Jobs, Cluster Status, Settings)
- Responsive layout
- Router outlet for lazy-loaded components

---

## Routing Configuration

Implemented lazy-loaded routes for better performance:

```typescript
/                    → Redirect to /jobs
/jobs                → Job list component
/jobs/create         → Job form (create mode)
/jobs/:id            → Job detail component
/jobs/:id/edit       → Job form (edit mode)
/cluster             → Cluster status component
/**                  → Redirect to /jobs (404 handling)
```

---

## Styling

### Global Styles (`styles.scss`)
- Material Design theme (Indigo-Pink)
- Utility classes for layout (flex, gap, container)
- Status badge styles matching backend statuses
- Node status styles (leader, follower, offline)
- Loading spinner and error message styles

### Component-Specific Styles
- Each component has its own SCSS file
- Responsive grid layouts
- Material Design principles
- Consistent spacing and typography

---

## Build & Run Commands

```bash
# Install dependencies
npm install

# Start development server (with proxy)
npm start
# or
ng serve

# Build for production
npm run build:prod

# Run tests
npm test
```

---

## API Integration

The frontend integrates with the backend REST API:

**Base URL**:
- Development: `http://localhost:8080/api/v1` (via proxy)
- Production: `/api/v1` (relative URL)

**Endpoints Used**:
- `GET /jobs` - List jobs
- `POST /jobs` - Create job
- `GET /jobs/{id}` - Get job
- `PATCH /jobs/{id}` - Update job
- `DELETE /jobs/{id}` - Delete job
- `POST /jobs/{id}/pause` - Pause job
- `POST /jobs/{id}/resume` - Resume job
- `POST /jobs/{id}/trigger` - Trigger job
- `GET /executions/job/{jobId}` - Get execution history
- `GET /cluster/status` - Get cluster status

---

## Next Steps

**Task 2: Job Dashboard Enhancements** (Optional):
- Add filtering by status
- Add search functionality
- Add bulk actions
- Add real-time updates with WebSockets
- Add charts and visualizations

**Task 3: Cluster Visualization Enhancements** (Optional):
- Add leader election timeline
- Add fencing token history
- Add node health graphs
- Add failover event log

**Task 4: Testing** (Optional):
- Unit tests for services
- Component tests
- E2E tests with Cypress

---

## Interview Talking Points

**Q: "Why did you choose Angular 17 over React or Vue?"**
- Angular provides a complete framework with built-in routing, HTTP client, forms, and dependency injection
- TypeScript is first-class in Angular, providing excellent type safety
- Angular Material provides production-ready UI components
- Standalone components (Angular 17+) simplify the architecture

**Q: "How did you handle CORS issues during development?"**
- Used Angular's proxy configuration (`proxy.conf.json`) to forward API requests to the backend
- This avoids CORS errors during local development
- In production, the frontend is served from the same domain as the backend

**Q: "How did you ensure type safety between frontend and backend?"**
- Created TypeScript interfaces matching backend DTOs exactly
- Used the same field names and types
- This ensures compile-time type checking and prevents runtime errors

**Q: "How did you handle errors globally?"**
- Implemented an HTTP error interceptor that catches all HTTP errors
- Provides user-friendly error messages
- Logs errors in development mode for debugging
- Distinguishes between client-side and server-side errors

---

**Completed**: 2026-03-14  
**Next Task**: Task 2 - Job Dashboard Enhancements (Optional)

