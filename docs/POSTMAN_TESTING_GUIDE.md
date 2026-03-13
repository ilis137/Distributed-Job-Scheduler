# Postman Testing Guide - Distributed Job Scheduler REST API

**Version**: 1.0.0
**Last Updated**: 2026-03-12
**Base URL**: `http://localhost:8080`

---

## 🚀 Quick Start (5 Minutes)

**New to this project?** Follow these steps to get started immediately:

1. **[Create Environment](#step-by-step-creating-postman-environment)** (2 min)
   - Add `base_url`, `node2_url`, `node3_url` variables
   - Activate the environment

2. **[Create Collection](#collection-level-settings---step-by-step-guide)** (2 min)
   - Name: "Distributed Job Scheduler API"
   - Add headers: `Content-Type: application/json`, `Accept: application/json`

3. **[Send Your First Request](#quick-start-your-first-request)** (1 min)
   - GET `{{base_url}}/api/v1/cluster/status`
   - Verify you get a 200 OK response

**Already set up?** Jump to:
- [API Endpoint Reference](#api-endpoint-reference) - All 15 endpoints with examples
- [Testing Scenarios](#step-by-step-testing-scenarios) - Common workflows
- [Distributed Systems Tests](#distributed-systems-testing) - Leader failover, fencing tokens, etc.
- [Troubleshooting](#troubleshooting-guide) - Fix common issues

---

## 📋 Table of Contents

### Getting Started
1. [Environment Setup](#environment-setup) - Create and configure Postman environment
2. [Collection Setup](#collection-level-settings---step-by-step-guide) - Configure collection-level settings
3. [Quick Start Guide](#quick-start-your-first-request) - Your first request in 1 minute

### API Reference
4. [API Endpoint Reference](#api-endpoint-reference) - All 15 endpoints with request/response examples
   - Job Management (9 endpoints)
   - Job Execution (3 endpoints)
   - Cluster Management (3 endpoints)

### Testing Guides
5. [Step-by-Step Testing Scenarios](#step-by-step-testing-scenarios) - Common workflows
6. [Postman Test Scripts](#postman-test-scripts) - JavaScript test templates
7. [Distributed Systems Testing](#distributed-systems-testing) - Advanced distributed features
8. [Error Handling Tests](#error-handling-tests) - Validation and error scenarios

### Reference
9. [Postman Collection Structure](#postman-collection-structure) - How to organize requests
10. [Troubleshooting Guide](#troubleshooting-guide) - Fix common issues
11. [Tips and Best Practices](#postman-best-practices) - Pro tips for Postman

---

## 📊 What You'll Learn

This guide covers:

✅ **Basic CRUD Operations** - Create, read, update, delete jobs
✅ **Cron Scheduling** - Create recurring jobs with cron expressions
✅ **Job Lifecycle** - Trigger, pause, resume jobs
✅ **Execution History** - View job execution logs and metrics
✅ **Cluster Management** - Monitor leader election and node health
✅ **Distributed Systems** - Test leader failover, fencing tokens, distributed locking
✅ **Error Handling** - Validate request/response errors
✅ **Postman Automation** - Write test scripts and use Collection Runner

---

## Environment Setup

### Step-by-Step: Creating Postman Environment

#### Step 1: Open Environments Panel

1. In Postman, look at the **top-right corner**
2. Click the **"Environments"** dropdown (shows "No Environment" by default)
3. Click **"Manage Environments"** or the **gear icon ⚙️**
4. Alternatively, click **"Environments"** in the left sidebar

#### Step 2: Create New Environment

1. Click the **"+"** button or **"Create Environment"** button
2. Name your environment: `Job Scheduler - Local`
3. Click **"Add"** or press Enter

#### Step 3: Add Environment Variables

Add the following variables one by one:

| Variable | Type | Initial Value | Current Value | Description |
|----------|------|---------------|---------------|-------------|
| `base_url` | default | `http://localhost:8080` | `http://localhost:8080` | Base URL for node 1 |
| `node2_url` | default | `http://localhost:8081` | `http://localhost:8081` | Base URL for node 2 |
| `node3_url` | default | `http://localhost:8082` | `http://localhost:8082` | Base URL for node 3 |
| `job_id` | default | (leave empty) | (leave empty) | Stores created job ID |
| `execution_id` | default | (leave empty) | (leave empty) | Stores execution ID |
| `leader_node_id` | default | (leave empty) | (leave empty) | Stores current leader node ID |
| `current_epoch` | default | (leave empty) | (leave empty) | Stores current cluster epoch |

**How to add each variable:**
1. Click **"Add a new variable"** or click in the empty row
2. Enter the **Variable** name (e.g., `base_url`)
3. Enter the **Initial Value** (e.g., `http://localhost:8080`)
4. The **Current Value** will auto-fill (or enter it manually)
5. Press **Tab** or **Enter** to move to the next row

#### Step 4: Save and Activate Environment

1. Click **"Save"** button (or Ctrl+S / Cmd+S)
2. Close the environment editor
3. In the **top-right corner**, select your environment from the dropdown:
   - Click the dropdown that says "No Environment"
   - Select **"Job Scheduler - Local"**
4. You should now see **"Job Scheduler - Local"** displayed in the top-right

**Verification**: Click the **eye icon 👁️** next to the environment dropdown to view all variables.

---

### Visual Guide: Environment Setup

```
Postman UI - Top Right Corner:
┌────────────────────────────────────────────────────────────┐
│                    [Job Scheduler - Local ▼] [👁️] [⚙️]     │
│                     ↑                         ↑    ↑        │
│                     │                         │    │        │
│              Select environment          View  Manage       │
│                                         variables           │
└────────────────────────────────────────────────────────────┘
```

---

### Testing Your Environment Setup

After creating the environment, test it:

1. Create a new request (click **"+"** in the main panel)
2. Set the URL to: `{{base_url}}/api/v1/cluster/status`
3. Hover over `{{base_url}}` - you should see a tooltip showing: `http://localhost:8080`
4. If you see "Unresolved Variable", your environment is not activated

---

### Creating Additional Environments (Optional)

You may want to create multiple environments for different scenarios:

**Environment: Job Scheduler - Docker**
- `base_url`: `http://localhost:8080`
- `node2_url`: `http://localhost:8081`
- `node3_url`: `http://localhost:8082`

**Environment: Job Scheduler - Production**
- `base_url`: `https://scheduler.example.com`
- `node2_url`: `https://scheduler2.example.com`
- `node3_url`: `https://scheduler3.example.com`

**Environment: Job Scheduler - Single Node**
- `base_url`: `http://localhost:8080`
- (no node2_url or node3_url needed)

---

### Quick Reference: Environment Variables

Once set up, you can use these variables in any request:

- **URLs**: `{{base_url}}/api/v1/jobs`
- **Request Bodies**: `"jobId": {{job_id}}`
- **Test Scripts**: `pm.environment.get("job_id")`
- **Pre-request Scripts**: `pm.environment.set("job_id", 123)`

### Collection-Level Settings - Step-by-Step Guide

Follow these steps to configure your Postman collection:

#### Step 1: Create a New Collection

1. Open Postman application
2. In the left sidebar, click the **"Collections"** tab
3. Click the **"+"** button or **"Create Collection"** button
4. Name your collection: `Distributed Job Scheduler API`
5. Click **"Create"**

#### Step 2: Configure Collection Variables

1. Click on your newly created collection name in the left sidebar
2. In the collection details panel, click the **"Variables"** tab
3. Add a collection variable for the base path:
   - **Variable**: `api_base_path`
   - **Initial Value**: `/api/v1`
   - **Current Value**: `/api/v1`
4. Click **"Save"** (Ctrl+S or Cmd+S)

**Why?** This allows you to use `{{base_url}}{{api_base_path}}` in all requests, making it easy to change the API version later.

#### Step 3: Configure Collection-Level Headers

1. With your collection still selected, click the **"Headers"** tab (next to Variables)
2. Add the following headers:

   | Key | Value | Description |
   |-----|-------|-------------|
   | `Content-Type` | `application/json` | Tells server we're sending JSON |
   | `Accept` | `application/json` | Tells server we expect JSON response |

3. Make sure both headers are **checked** (enabled)
4. Click **"Save"** (Ctrl+S or Cmd+S)

**Note**: These headers will be automatically added to ALL requests in this collection.

#### Step 4: Configure Pre-Request Script (Optional but Recommended)

1. Click the **"Pre-request Script"** tab
2. Paste the following JavaScript code:

```javascript
// Set timestamp for unique job names
pm.collectionVariables.set("timestamp", Date.now());

// Log request details for debugging
console.log(`[${new Date().toISOString()}] ${pm.request.method} ${pm.request.url}`);
```

3. Click **"Save"**

**Why?** This script runs before every request and generates a unique timestamp for creating jobs with unique names.

#### Step 5: Configure Test Script (Optional)

1. Click the **"Tests"** tab
2. Paste the following JavaScript code:

```javascript
// Log response details
console.log(`Response Status: ${pm.response.code} ${pm.response.status}`);
console.log(`Response Time: ${pm.response.responseTime}ms`);

// Basic validation for all requests
pm.test("Response time is acceptable", function() {
    pm.expect(pm.response.responseTime).to.be.below(5000);
});
```

3. Click **"Save"**

**Why?** This script runs after every request and provides basic logging and validation.

#### Step 6: Verify Configuration

Your collection settings should now look like this:

**Variables Tab:**
```
api_base_path = /api/v1
```

**Headers Tab:**
```
Content-Type: application/json ✓
Accept: application/json ✓
```

---

### Visual Guide: Where to Find Settings

```
Postman UI Layout:
┌─────────────────────────────────────────────────────────────┐
│ File  Edit  View  Help                          [Search]    │
├──────────┬──────────────────────────────────────────────────┤
│          │  Distributed Job Scheduler API                   │
│ Collections                                                  │
│  └─ 📁 Distributed Job Scheduler API  ← Click here          │
│     ├─ 📁 1. Job Management                                 │
│     ├─ 📁 2. Job Execution           ┌──────────────────────┤
│     └─ 📁 3. Cluster Management      │ Variables  Headers   │
│                                       │ Pre-request  Tests   │
│                                       │ Authorization        │
│                                       ├──────────────────────┤
│                                       │ [Settings appear     │
│                                       │  here when you       │
│                                       │  click collection]   │
│                                       │                      │
└──────────────────────────────────────┴──────────────────────┘
```

---

### Alternative: Using Request URLs Directly

If you prefer NOT to use collection-level base paths, you can:

1. **Skip Step 2** (collection variables)
2. Use full URLs in each request:
   - `{{base_url}}/api/v1/jobs`
   - `{{base_url}}/api/v1/executions`
   - `{{base_url}}/api/v1/cluster/status`

**Pros**: More explicit, easier to understand
**Cons**: More repetitive, harder to change API version

---

### Quick Checklist

After completing the setup, verify:

- ✅ Collection created with name "Distributed Job Scheduler API"
- ✅ Environment created with `base_url`, `node2_url`, `node3_url` variables
- ✅ Collection headers include `Content-Type` and `Accept`
- ✅ Pre-request script generates timestamps (optional)
- ✅ Test script logs responses (optional)

---

## Quick Start: Your First Request

Before diving into all endpoints, let's create your first request to verify everything works.

### Step-by-Step: Create "Get Cluster Status" Request

#### Step 1: Create a New Request

1. In your collection **"Distributed Job Scheduler API"**, right-click
2. Select **"Add Request"**
3. Name it: `Get Cluster Status`
4. Press **Enter**

#### Step 2: Configure the Request

1. **Method**: Select **GET** from the dropdown (should be default)
2. **URL**: Enter `{{base_url}}/api/v1/cluster/status`
3. **Headers**: Already set at collection level (Content-Type, Accept)
4. **Body**: Not needed for GET requests

#### Step 3: Add Test Script

Click the **"Tests"** tab and add:

```javascript
pm.test("Status code is 200", function() {
    pm.response.to.have.status(200);
});

pm.test("Cluster has at least one node", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.nodes.length).to.be.above(0);
});

pm.test("Leader is elected", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.leaderNodeId).to.not.be.null;

    // Save leader info for later use
    pm.environment.set("leader_node_id", jsonData.leaderNodeId);

    console.log(`Leader: ${jsonData.leaderNodeId}`);
    console.log(`Total Nodes: ${jsonData.totalNodes}`);
    console.log(`Healthy Nodes: ${jsonData.healthyNodes}`);
});
```

#### Step 4: Send the Request

1. Click the blue **"Send"** button
2. Wait for the response (should be < 1 second)
3. Check the response panel below:
   - **Status**: Should show `200 OK` in green
   - **Body**: Should show JSON with cluster information
   - **Test Results**: Should show 3 passed tests (3/3)

#### Step 5: Verify Response

Your response should look like this:

```json
{
  "nodes": [
    {
      "nodeId": "scheduler-node-1",
      "role": "LEADER",
      "healthy": true,
      "epoch": 1,
      "lastHeartbeat": "2026-03-12T10:00:00Z",
      "startTime": "2026-03-12T09:00:00Z",
      "version": "1.0.0"
    }
  ],
  "leaderNodeId": "scheduler-node-1",
  "totalNodes": 1,
  "healthyNodes": 1,
  "totalJobs": 0,
  "activeJobs": 0,
  "pendingJobs": 0,
  "failedJobs": 0
}
```

#### Troubleshooting

**Problem**: "Could not get any response"
- **Solution**: Make sure Docker containers are running: `docker ps`
- **Solution**: Verify port 8080 is accessible: `curl http://localhost:8080/api/v1/cluster/status`

**Problem**: "Unresolved variable: base_url"
- **Solution**: Activate your environment (top-right dropdown)
- **Solution**: Verify `base_url` is set in environment (click eye icon 👁️)

**Problem**: 404 Not Found
- **Solution**: Check URL is correct: `{{base_url}}/api/v1/cluster/status` (not `/cluster` or `/status`)
- **Solution**: Verify application is running: `docker logs scheduler-node-1`

**Problem**: Tests failing
- **Solution**: Check response body in the "Body" tab
- **Solution**: Look at Console (View → Show Postman Console) for error messages

---

### Quick Start: Create Your First Job

Now let's create a job!

#### Step 1: Create "Create One-Time Job" Request

1. Right-click your collection → **"Add Request"**
2. Name: `Create One-Time Job`
3. **Method**: **POST**
4. **URL**: `{{base_url}}/api/v1/jobs`

#### Step 2: Configure Request Body

1. Click the **"Body"** tab
2. Select **"raw"** radio button
3. Select **"JSON"** from the dropdown (right side)
4. Paste this JSON:

```json
{
  "name": "test-job-{{timestamp}}",
  "description": "My first test job",
  "payload": "{\"message\": \"Hello from Postman!\"}",
  "maxRetries": 3,
  "timeoutSeconds": 300,
  "enabled": true
}
```

**Note**: `{{timestamp}}` will be replaced with a unique timestamp from the collection pre-request script.

#### Step 3: Add Test Script

Click **"Tests"** tab:

```javascript
pm.test("Job created successfully", function() {
    pm.response.to.have.status(201);

    var jsonData = pm.response.json();

    // Validate response
    pm.expect(jsonData.id).to.be.a("number");
    pm.expect(jsonData.status).to.eql("PENDING");
    pm.expect(jsonData.name).to.include("test-job-");

    // Save job ID for later use
    pm.environment.set("job_id", jsonData.id);

    console.log(`Created Job ID: ${jsonData.id}`);
    console.log(`Job Name: ${jsonData.name}`);
    console.log(`Job Status: ${jsonData.status}`);
});
```

#### Step 4: Send and Verify

1. Click **"Send"**
2. Expected response: **201 Created**
3. Check **Test Results**: Should show all tests passed
4. Check **Console**: Should show the created job ID

#### Step 5: Verify Job Was Created

1. Create a new request: `Get Job by ID`
2. **Method**: **GET**
3. **URL**: `{{base_url}}/api/v1/jobs/{{job_id}}`
4. Click **"Send"**
5. You should see your job details!

---

## API Endpoint Reference

### 1. Job Management Endpoints

#### 1.1 Create Job (One-Time)

**Endpoint**: `POST /api/v1/jobs`  
**Description**: Creates a one-time job that executes immediately

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "one-time-job-001",
  "description": "Test one-time job",
  "payload": "{\"message\": \"Hello World\"}",
  "maxRetries": 3,
  "timeoutSeconds": 300,
  "enabled": true
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "name": "one-time-job-001",
  "description": "Test one-time job",
  "status": "PENDING",
  "cronExpression": null,
  "nextRunTime": "2026-03-10T10:30:00Z",
  "retryCount": 0,
  "maxRetries": 3,
  "timeoutSeconds": 300,
  "enabled": true,
  "createdAt": "2026-03-10T10:30:00Z",
  "updatedAt": "2026-03-10T10:30:00Z"
}
```

---

#### 1.2 Create Job (Recurring with Cron)

**Endpoint**: `POST /api/v1/jobs`  
**Description**: Creates a recurring job with cron expression

**Request Body**:
```json
{
  "name": "daily-cleanup-job",
  "description": "Daily cleanup job at midnight",
  "cronExpression": "0 0 0 * * ?",
  "payload": "{\"action\": \"cleanup\", \"target\": \"temp_files\"}",
  "maxRetries": 5,
  "timeoutSeconds": 600,
  "enabled": true
}
```

**Common Cron Expressions**:
- `0 0 0 * * ?` - Daily at midnight
- `0 0 */6 * * ?` - Every 6 hours
- `0 */15 * * * ?` - Every 15 minutes
- `0 0 9 * * MON-FRI` - Weekdays at 9 AM

**Response**: `201 Created`
```json
{
  "id": 2,
  "name": "daily-cleanup-job",
  "description": "Daily cleanup job at midnight",
  "status": "PENDING",
  "cronExpression": "0 0 0 * * ?",
  "nextRunTime": "2026-03-11T00:00:00Z",
  "retryCount": 0,
  "maxRetries": 5,
  "timeoutSeconds": 600,
  "enabled": true,
  "createdAt": "2026-03-10T10:30:00Z",
  "updatedAt": "2026-03-10T10:30:00Z"
}
```

---

#### 1.3 Get Job by ID

**Endpoint**: `GET /api/v1/jobs/{id}`  
**Description**: Retrieves job details by ID

**Example**: `GET /api/v1/jobs/1`

**Response**: `200 OK`
```json
{
  "id": 1,
  "name": "one-time-job-001",
  "description": "Test one-time job",
  "status": "COMPLETED",
  "cronExpression": null,
  "nextRunTime": null,
  "retryCount": 0,
  "maxRetries": 3,
  "timeoutSeconds": 300,
  "enabled": true,
  "createdAt": "2026-03-10T10:30:00Z",
  "updatedAt": "2026-03-10T10:31:00Z"
}
```

**Error Response**: `404 Not Found`
```json
{
  "timestamp": "2026-03-10T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Job not found with id: 999",
  "path": "/api/v1/jobs/999"
}
```

---

#### 1.4 List All Jobs (Paginated)

**Endpoint**: `GET /api/v1/jobs`  
**Description**: Lists all jobs with pagination

**Query Parameters**:
- `page` (optional): Page number (0-indexed, default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort field and direction (default: `createdAt,desc`)
- `status` (optional): Filter by status (PENDING, SCHEDULED, RUNNING, COMPLETED, FAILED, PAUSED, RETRYING, DEAD_LETTER)

**Examples**:
- `GET /api/v1/jobs?page=0&size=10`
- `GET /api/v1/jobs?status=PENDING`
- `GET /api/v1/jobs?status=FAILED&page=0&size=20&sort=createdAt,desc`

**Response**: `200 OK`
```json
{
  "jobs": [
    {
      "id": 1,
      "name": "one-time-job-001",
      "status": "COMPLETED",
      ...
    },
    {
      "id": 2,
      "name": "daily-cleanup-job",
      "status": "PENDING",
      ...
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": false,
  "hasPrevious": false
}
```

---

#### 1.5 Update Job

**Endpoint**: `PUT /api/v1/jobs/{id}`  
**Description**: Updates job configuration (partial update)

**Request Body** (all fields optional):
```json
{
  "description": "Updated description",
  "cronExpression": "0 0 */12 * * ?",
  "payload": "{\"updated\": true}",
  "maxRetries": 5,
  "timeoutSeconds": 600,
  "enabled": false
}
```

**Response**: `200 OK`
```json
{
  "id": 2,
  "name": "daily-cleanup-job",
  "description": "Updated description",
  "status": "PENDING",
  "cronExpression": "0 0 */12 * * ?",
  "nextRunTime": "2026-03-10T12:00:00Z",
  "retryCount": 0,
  "maxRetries": 5,
  "timeoutSeconds": 600,
  "enabled": false,
  "createdAt": "2026-03-10T10:30:00Z",
  "updatedAt": "2026-03-10T10:35:00Z"
}
```

---

#### 1.6 Delete Job

**Endpoint**: `DELETE /api/v1/jobs/{id}`  
**Description**: Deletes a job (soft delete)

**Example**: `DELETE /api/v1/jobs/1`

**Response**: `204 No Content`

---

#### 1.7 Trigger Job Manually

**Endpoint**: `POST /api/v1/jobs/{id}/trigger`  
**Description**: Manually triggers job execution (ignores cron schedule)

**Example**: `POST /api/v1/jobs/2/trigger`

**Response**: `202 Accepted`
```json
{
  "id": 2,
  "name": "daily-cleanup-job",
  "status": "PENDING",
  "nextRunTime": "2026-03-10T10:36:00Z",
  ...
}
```

---

#### 1.8 Pause Job

**Endpoint**: `POST /api/v1/jobs/{id}/pause`  
**Description**: Pauses job execution

**Example**: `POST /api/v1/jobs/2/pause`

**Response**: `200 OK`
```json
{
  "id": 2,
  "name": "daily-cleanup-job",
  "status": "PAUSED",
  ...
}
```

---

#### 1.9 Resume Job

**Endpoint**: `POST /api/v1/jobs/{id}/resume`  
**Description**: Resumes paused job

**Example**: `POST /api/v1/jobs/2/resume`

**Response**: `200 OK`
```json
{
  "id": 2,
  "name": "daily-cleanup-job",
  "status": "PENDING",
  ...
}
```

---

### 2. Job Execution Endpoints

#### 2.1 Get Execution by ID

**Endpoint**: `GET /api/v1/executions/{id}`  
**Description**: Retrieves execution details by ID

**Example**: `GET /api/v1/executions/1`

**Response**: `200 OK`
```json
{
  "id": 1,
  "jobId": 1,
  "jobName": "one-time-job-001",
  "status": "SUCCESS",
  "nodeId": "scheduler-node-1",
  "startTime": "2026-03-10T10:30:05Z",
  "endTime": "2026-03-10T10:30:06Z",
  "durationMs": 1000,
  "errorMessage": null,
  "errorStackTrace": null,
  "fencingToken": "1-1710068405000",
  "retryAttempt": 0
}
```

---

#### 2.2 Get Job Execution History

**Endpoint**: `GET /api/v1/executions/job/{jobId}`  
**Description**: Retrieves execution history for a specific job

**Query Parameters**:
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort field (default: `startTime,desc`)

**Example**: `GET /api/v1/executions/job/1?page=0&size=10`

**Response**: `200 OK`
```json
{
  "executions": [
    {
      "id": 3,
      "jobId": 1,
      "status": "SUCCESS",
      "startTime": "2026-03-10T10:35:00Z",
      ...
    },
    {
      "id": 2,
      "jobId": 1,
      "status": "FAILED",
      "startTime": "2026-03-10T10:32:00Z",
      ...
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 10
}
```

---

#### 2.3 List All Executions

**Endpoint**: `GET /api/v1/executions`  
**Description**: Lists all executions with optional filtering

**Query Parameters**:
- `status` (optional): Filter by status (SUCCESS, FAILED, RUNNING, TIMEOUT, CANCELLED)
- `page`, `size`, `sort`: Pagination parameters

**Example**: `GET /api/v1/executions?status=FAILED&page=0&size=20`

**Response**: `200 OK` (same structure as execution history)

---

### 3. Cluster Management Endpoints

#### 3.1 Get Cluster Status

**Endpoint**: `GET /api/v1/cluster/status`  
**Description**: Retrieves overall cluster status

**Response**: `200 OK`
```json
{
  "nodes": [
    {
      "nodeId": "scheduler-node-1",
      "role": "LEADER",
      "healthy": true,
      "epoch": 5,
      "lastHeartbeat": "2026-03-10T10:40:00Z",
      "startTime": "2026-03-10T10:00:00Z",
      "version": "1.0.0"
    },
    {
      "nodeId": "scheduler-node-2",
      "role": "FOLLOWER",
      "healthy": true,
      "epoch": 5,
      "lastHeartbeat": "2026-03-10T10:40:00Z",
      "startTime": "2026-03-10T10:00:05Z",
      "version": "1.0.0"
    }
  ],
  "leaderNodeId": "scheduler-node-1",
  "totalNodes": 3,
  "healthyNodes": 3,
  "totalJobs": 10,
  "activeJobs": 2,
  "pendingJobs": 5,
  "failedJobs": 1
}
```

---

#### 3.2 List All Nodes

**Endpoint**: `GET /api/v1/cluster/nodes`  
**Description**: Lists all scheduler nodes

**Response**: `200 OK`
```json
[
  {
    "nodeId": "scheduler-node-1",
    "role": "LEADER",
    "healthy": true,
    "epoch": 5,
    "lastHeartbeat": "2026-03-10T10:40:00Z",
    "startTime": "2026-03-10T10:00:00Z",
    "version": "1.0.0"
  },
  {
    "nodeId": "scheduler-node-2",
    "role": "FOLLOWER",
    "healthy": true,
    "epoch": 5,
    "lastHeartbeat": "2026-03-10T10:40:00Z",
    "startTime": "2026-03-10T10:00:05Z",
    "version": "1.0.0"
  }
]
```

---

#### 3.3 Get Current Leader

**Endpoint**: `GET /api/v1/cluster/leader`  
**Description**: Retrieves current leader node information

**Response**: `200 OK`
```json
{
  "nodeId": "scheduler-node-1",
  "role": "LEADER",
  "healthy": true,
  "epoch": 5,
  "lastHeartbeat": "2026-03-10T10:40:00Z",
  "startTime": "2026-03-10T10:00:00Z",
  "version": "1.0.0"
}
```

**Error Response**: `404 Not Found` (if no leader elected)
```json
{
  "timestamp": "2026-03-10T10:40:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No leader currently elected",
  "path": "/api/v1/cluster/leader"
}
```

---

## Step-by-Step Testing Scenarios

### Scenario 1: Create and Execute One-Time Job

**Step 1**: Create a one-time job
```
POST {{base_url}}/api/v1/jobs
```
Body:
```json
{
  "name": "test-job-{{$timestamp}}",
  "description": "Test one-time job",
  "payload": "{\"duration\": 2000}",
  "maxRetries": 3,
  "timeoutSeconds": 300,
  "enabled": true
}
```

**Step 2**: Save job ID from response
```javascript
// In Tests tab
pm.test("Job created successfully", function() {
    pm.response.to.have.status(201);
    var jsonData = pm.response.json();
    pm.environment.set("job_id", jsonData.id);
    pm.expect(jsonData.status).to.eql("PENDING");
});
```

**Step 3**: Wait 5 seconds, then check job status
```
GET {{base_url}}/api/v1/jobs/{{job_id}}
```

**Step 4**: Verify job completed
```javascript
pm.test("Job completed successfully", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.be.oneOf(["COMPLETED", "RUNNING"]);
});
```

**Step 5**: Check execution history
```
GET {{base_url}}/api/v1/executions/job/{{job_id}}
```

---

### Scenario 2: Create Recurring Job with Cron

**Step 1**: Create recurring job
```
POST {{base_url}}/api/v1/jobs
```
Body:
```json
{
  "name": "hourly-report-{{$timestamp}}",
  "description": "Hourly report generation",
  "cronExpression": "0 0 * * * ?",
  "payload": "{\"reportType\": \"hourly\"}",
  "maxRetries": 5,
  "timeoutSeconds": 600,
  "enabled": true
}
```

**Step 2**: Verify cron expression parsed correctly
```javascript
pm.test("Cron job created with future nextRunTime", function() {
    pm.response.to.have.status(201);
    var jsonData = pm.response.json();
    pm.environment.set("job_id", jsonData.id);
    pm.expect(jsonData.cronExpression).to.eql("0 0 * * * ?");
    pm.expect(jsonData.nextRunTime).to.not.be.null;
    
    // Verify nextRunTime is in the future
    var nextRun = new Date(jsonData.nextRunTime);
    var now = new Date();
    pm.expect(nextRun.getTime()).to.be.above(now.getTime());
});
```

**Step 3**: Manually trigger the job
```
POST {{base_url}}/api/v1/jobs/{{job_id}}/trigger
```

**Step 4**: Verify job was triggered
```javascript
pm.test("Job triggered successfully", function() {
    pm.response.to.have.status(202);
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.eql("PENDING");
});
```

---

### Scenario 3: Test Job State Transitions

**Step 1**: Create job
```
POST {{base_url}}/api/v1/jobs
```

**Step 2**: Verify initial state is PENDING
```
GET {{base_url}}/api/v1/jobs/{{job_id}}
```
Expected: `status: "PENDING"`

**Step 3**: Wait for job to be scheduled (check status)
Expected: `status: "SCHEDULED"` or `"RUNNING"`

**Step 4**: Wait for completion
Expected: `status: "COMPLETED"`

**Step 5**: Pause the job
```
POST {{base_url}}/api/v1/jobs/{{job_id}}/pause
```
Expected: `status: "PAUSED"`

**Step 6**: Resume the job
```
POST {{base_url}}/api/v1/jobs/{{job_id}}/resume
```
Expected: `status: "PENDING"`

---

### Scenario 4: Test Pagination and Filtering

**Step 1**: Create multiple jobs (repeat 25 times with different names)
```
POST {{base_url}}/api/v1/jobs
```

**Step 2**: List jobs with default pagination
```
GET {{base_url}}/api/v1/jobs
```
Expected: 20 jobs per page (default)

**Step 3**: Test custom page size
```
GET {{base_url}}/api/v1/jobs?page=0&size=10
```

**Step 4**: Test pagination navigation
```javascript
pm.test("Pagination metadata is correct", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.totalElements).to.be.above(20);
    pm.expect(jsonData.totalPages).to.be.above(1);
    pm.expect(jsonData.hasNext).to.be.true;
    pm.expect(jsonData.hasPrevious).to.be.false;
});
```

**Step 5**: Filter by status
```
GET {{base_url}}/api/v1/jobs?status=COMPLETED
```

**Step 6**: Verify filtering works
```javascript
pm.test("All jobs have COMPLETED status", function() {
    var jsonData = pm.response.json();
    jsonData.jobs.forEach(function(job) {
        pm.expect(job.status).to.eql("COMPLETED");
    });
});
```

---

### Scenario 5: Test Job Updates

**Step 1**: Create a job
```
POST {{base_url}}/api/v1/jobs
```

**Step 2**: Update description only
```
PUT {{base_url}}/api/v1/jobs/{{job_id}}
```
Body:
```json
{
  "description": "Updated description"
}
```

**Step 3**: Verify only description changed
```javascript
pm.test("Partial update successful", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.description).to.eql("Updated description");
    // Other fields should remain unchanged
});
```

**Step 4**: Update cron expression
```
PUT {{base_url}}/api/v1/jobs/{{job_id}}
```
Body:
```json
{
  "cronExpression": "0 */30 * * * ?"
}
```

**Step 5**: Verify nextRunTime recalculated
```javascript
pm.test("Next run time recalculated", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.cronExpression).to.eql("0 */30 * * * ?");
    pm.expect(jsonData.nextRunTime).to.not.be.null;
});
```

---

## Postman Test Scripts

### Collection-Level Pre-Request Script

Add this to your collection's Pre-request Scripts tab:

```javascript
// Set timestamp for unique job names
pm.environment.set("timestamp", Date.now());

// Log request details
console.log(`[${new Date().toISOString()}] ${pm.request.method} ${pm.request.url}`);
```

---

### Test Script Templates

#### Template 1: Basic Status Code Validation

```javascript
pm.test("Status code is 200", function() {
    pm.response.to.have.status(200);
});

pm.test("Response time is acceptable", function() {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

pm.test("Content-Type is JSON", function() {
    pm.response.to.have.header("Content-Type");
    pm.expect(pm.response.headers.get("Content-Type")).to.include("application/json");
});
```

---

#### Template 2: Job Creation Validation

```javascript
pm.test("Job created successfully", function() {
    pm.response.to.have.status(201);

    var jsonData = pm.response.json();

    // Validate response structure
    pm.expect(jsonData).to.have.property("id");
    pm.expect(jsonData).to.have.property("name");
    pm.expect(jsonData).to.have.property("status");
    pm.expect(jsonData).to.have.property("createdAt");

    // Validate field values
    pm.expect(jsonData.id).to.be.a("number");
    pm.expect(jsonData.status).to.eql("PENDING");
    pm.expect(jsonData.enabled).to.be.true;
    pm.expect(jsonData.retryCount).to.eql(0);

    // Save job ID for subsequent requests
    pm.environment.set("job_id", jsonData.id);

    console.log(`Created job ID: ${jsonData.id}`);
});
```

---

#### Template 3: Cron Expression Validation

```javascript
pm.test("Cron expression validated correctly", function() {
    pm.response.to.have.status(201);

    var jsonData = pm.response.json();

    // Verify cron expression is stored
    pm.expect(jsonData.cronExpression).to.not.be.null;
    pm.expect(jsonData.cronExpression).to.match(/^[0-9\s\*\?\/\-,]+$/);

    // Verify nextRunTime is calculated and in the future
    pm.expect(jsonData.nextRunTime).to.not.be.null;

    var nextRun = new Date(jsonData.nextRunTime);
    var now = new Date();
    pm.expect(nextRun.getTime()).to.be.above(now.getTime());

    console.log(`Next run time: ${jsonData.nextRunTime}`);
});
```

---

#### Template 4: Pagination Validation

```javascript
pm.test("Pagination structure is correct", function() {
    pm.response.to.have.status(200);

    var jsonData = pm.response.json();

    // Validate pagination metadata
    pm.expect(jsonData).to.have.property("jobs");
    pm.expect(jsonData).to.have.property("totalElements");
    pm.expect(jsonData).to.have.property("totalPages");
    pm.expect(jsonData).to.have.property("currentPage");
    pm.expect(jsonData).to.have.property("pageSize");
    pm.expect(jsonData).to.have.property("hasNext");
    pm.expect(jsonData).to.have.property("hasPrevious");

    // Validate data types
    pm.expect(jsonData.jobs).to.be.an("array");
    pm.expect(jsonData.totalElements).to.be.a("number");
    pm.expect(jsonData.totalPages).to.be.a("number");

    // Validate page size
    pm.expect(jsonData.jobs.length).to.be.at.most(jsonData.pageSize);

    console.log(`Page ${jsonData.currentPage + 1} of ${jsonData.totalPages} (${jsonData.totalElements} total)`);
});
```

---

#### Template 5: Execution History Validation

```javascript
pm.test("Execution history retrieved successfully", function() {
    pm.response.to.have.status(200);

    var jsonData = pm.response.json();

    pm.expect(jsonData.executions).to.be.an("array");

    if (jsonData.executions.length > 0) {
        var execution = jsonData.executions[0];

        // Validate execution structure
        pm.expect(execution).to.have.property("id");
        pm.expect(execution).to.have.property("jobId");
        pm.expect(execution).to.have.property("status");
        pm.expect(execution).to.have.property("nodeId");
        pm.expect(execution).to.have.property("startTime");
        pm.expect(execution).to.have.property("fencingToken");

        // Validate fencing token format (epoch-nodeId)
        pm.expect(execution.fencingToken).to.match(/^\d+-\d+$/);

        // If execution completed, validate duration
        if (execution.status === "SUCCESS" || execution.status === "FAILED") {
            pm.expect(execution.endTime).to.not.be.null;
            pm.expect(execution.durationMs).to.be.a("number");
            pm.expect(execution.durationMs).to.be.above(0);
        }

        console.log(`Execution ${execution.id}: ${execution.status} (${execution.durationMs}ms)`);
    }
});
```

---

#### Template 6: Cluster Status Validation

```javascript
pm.test("Cluster status is healthy", function() {
    pm.response.to.have.status(200);

    var jsonData = pm.response.json();

    // Validate cluster structure
    pm.expect(jsonData).to.have.property("nodes");
    pm.expect(jsonData).to.have.property("leaderNodeId");
    pm.expect(jsonData).to.have.property("totalNodes");
    pm.expect(jsonData).to.have.property("healthyNodes");

    // Validate at least one node exists
    pm.expect(jsonData.nodes.length).to.be.above(0);
    pm.expect(jsonData.totalNodes).to.eql(jsonData.nodes.length);

    // Validate leader exists
    pm.expect(jsonData.leaderNodeId).to.not.be.null;

    // Find leader node
    var leader = jsonData.nodes.find(node => node.role === "LEADER");
    pm.expect(leader).to.not.be.undefined;
    pm.expect(leader.nodeId).to.eql(jsonData.leaderNodeId);
    pm.expect(leader.healthy).to.be.true;

    // Save leader info
    pm.environment.set("leader_node_id", leader.nodeId);
    pm.environment.set("current_epoch", leader.epoch);

    console.log(`Leader: ${leader.nodeId} (epoch ${leader.epoch})`);
    console.log(`Cluster: ${jsonData.healthyNodes}/${jsonData.totalNodes} healthy nodes`);
});
```

---

#### Template 7: Error Response Validation

```javascript
pm.test("Error response has correct structure", function() {
    pm.response.to.have.status(400); // or 404, 500, etc.

    var jsonData = pm.response.json();

    // Validate error structure
    pm.expect(jsonData).to.have.property("timestamp");
    pm.expect(jsonData).to.have.property("status");
    pm.expect(jsonData).to.have.property("error");
    pm.expect(jsonData).to.have.property("message");
    pm.expect(jsonData).to.have.property("path");

    // Validate status code matches
    pm.expect(jsonData.status).to.eql(400);

    console.log(`Error: ${jsonData.message}`);
});
```

---

## Distributed Systems Testing

### Test 1: Verify Leader Election

**Objective**: Confirm that exactly one node is elected as leader

**Steps**:

1. **Get cluster status**
```
GET {{base_url}}/api/v1/cluster/status
```

2. **Verify single leader**
```javascript
pm.test("Exactly one leader exists", function() {
    var jsonData = pm.response.json();

    var leaders = jsonData.nodes.filter(node => node.role === "LEADER");
    pm.expect(leaders.length).to.eql(1);

    var followers = jsonData.nodes.filter(node => node.role === "FOLLOWER");
    pm.expect(followers.length).to.eql(jsonData.totalNodes - 1);

    console.log(`Leader: ${leaders[0].nodeId}`);
    console.log(`Followers: ${followers.map(f => f.nodeId).join(", ")}`);
});
```

3. **Verify all nodes have same epoch**
```javascript
pm.test("All nodes have same epoch", function() {
    var jsonData = pm.response.json();

    var epochs = jsonData.nodes.map(node => node.epoch);
    var uniqueEpochs = [...new Set(epochs)];

    pm.expect(uniqueEpochs.length).to.eql(1);

    console.log(`Cluster epoch: ${uniqueEpochs[0]}`);
});
```

---

### Test 2: Verify Fencing Tokens

**Objective**: Ensure all job executions use valid fencing tokens

**Steps**:

1. **Create and trigger a job**
```
POST {{base_url}}/api/v1/jobs
POST {{base_url}}/api/v1/jobs/{{job_id}}/trigger
```

2. **Wait 5 seconds, then get execution history**
```
GET {{base_url}}/api/v1/executions/job/{{job_id}}
```

3. **Verify fencing token format**
```javascript
pm.test("Fencing token is valid", function() {
    var jsonData = pm.response.json();

    if (jsonData.executions.length > 0) {
        var execution = jsonData.executions[0];

        // Fencing token format: epoch-timestamp
        pm.expect(execution.fencingToken).to.match(/^\d+-\d+$/);

        var parts = execution.fencingToken.split("-");
        var epoch = parseInt(parts[0]);
        var timestamp = parseInt(parts[1]);

        // Verify epoch matches current cluster epoch
        var currentEpoch = pm.environment.get("current_epoch");
        pm.expect(epoch).to.eql(currentEpoch);

        console.log(`Fencing token: ${execution.fencingToken}`);
    }
});
```

---

### Test 3: Test Leader Failover

**Objective**: Verify automatic leader election after leader failure

**Prerequisites**: Docker cluster with 3 nodes running

**Steps**:

1. **Get current leader**
```
GET {{base_url}}/api/v1/cluster/leader
```
Save leader node ID and epoch

2. **Kill the leader container**
```bash
# In terminal
docker stop scheduler-node-1
```

3. **Wait 15 seconds for failover**

4. **Check cluster status from node 2**
```
GET {{node2_url}}/api/v1/cluster/status
```

5. **Verify new leader elected**
```javascript
pm.test("New leader elected after failover", function() {
    var jsonData = pm.response.json();

    var oldLeader = pm.environment.get("leader_node_id");
    var newLeader = jsonData.leaderNodeId;

    // New leader should be different
    pm.expect(newLeader).to.not.eql(oldLeader);

    // Epoch should have incremented
    var oldEpoch = pm.environment.get("current_epoch");
    var leader = jsonData.nodes.find(node => node.role === "LEADER");
    pm.expect(leader.epoch).to.be.above(oldEpoch);

    console.log(`Failover: ${oldLeader} -> ${newLeader}`);
    console.log(`Epoch: ${oldEpoch} -> ${leader.epoch}`);
});
```

6. **Restart old leader**
```bash
docker start scheduler-node-1
```

7. **Verify old leader rejoins as follower**
```
GET {{base_url}}/api/v1/cluster/status
```

---

### Test 4: Test Distributed Locking

**Objective**: Verify only one node executes a job at a time

**Steps**:

1. **Create a long-running job**
```
POST {{base_url}}/api/v1/jobs
```
Body:
```json
{
  "name": "long-running-job",
  "payload": "{\"duration\": 10000}",
  "timeoutSeconds": 30,
  "enabled": true
}
```

2. **Trigger job multiple times rapidly**
```
POST {{base_url}}/api/v1/jobs/{{job_id}}/trigger
POST {{base_url}}/api/v1/jobs/{{job_id}}/trigger
POST {{base_url}}/api/v1/jobs/{{job_id}}/trigger
```

3. **Wait 2 seconds, check running executions**
```
GET {{base_url}}/api/v1/executions?status=RUNNING
```

4. **Verify only one execution is running**
```javascript
pm.test("Only one execution running (distributed lock)", function() {
    var jsonData = pm.response.json();

    var runningExecutions = jsonData.executions.filter(e =>
        e.jobId === parseInt(pm.environment.get("job_id")) &&
        e.status === "RUNNING"
    );

    pm.expect(runningExecutions.length).to.be.at.most(1);

    console.log(`Running executions: ${runningExecutions.length}`);
});
```

---

### Test 5: Test Retry Logic

**Objective**: Verify exponential backoff retry mechanism

**Steps**:

1. **Create a job that will fail**
```
POST {{base_url}}/api/v1/jobs
```
Body:
```json
{
  "name": "failing-job",
  "payload": "{\"shouldFail\": true}",
  "maxRetries\": 3,
  "timeoutSeconds": 60,
  "enabled": true
}
```

2. **Trigger the job**
```
POST {{base_url}}/api/v1/jobs/{{job_id}}/trigger
```

3. **Wait 30 seconds, check execution history**
```
GET {{base_url}}/api/v1/executions/job/{{job_id}}
```

4. **Verify retry attempts**
```javascript
pm.test("Job retried with exponential backoff", function() {
    var jsonData = pm.response.json();

    // Should have multiple execution attempts
    pm.expect(jsonData.executions.length).to.be.above(1);

    // Verify retry attempt numbers
    var retryAttempts = jsonData.executions.map(e => e.retryAttempt).sort();
    pm.expect(retryAttempts).to.include(0); // Initial attempt
    pm.expect(retryAttempts).to.include(1); // First retry

    // Verify all failed
    jsonData.executions.forEach(function(execution) {
        pm.expect(execution.status).to.eql("FAILED");
        pm.expect(execution.errorMessage).to.not.be.null;
    });

    console.log(`Retry attempts: ${retryAttempts.join(", ")}`);
});
```

5. **Check final job status**
```
GET {{base_url}}/api/v1/jobs/{{job_id}}
```

Expected: `status: "FAILED"` or `"DEAD_LETTER"` (after max retries exceeded)

---

### Test 6: Test Orphaned Job Recovery

**Objective**: Verify orphaned jobs are detected and recovered

**Steps**:

1. **Create a job**
```
POST {{base_url}}/api/v1/jobs
```

2. **Trigger the job**
```
POST {{base_url}}/api/v1/jobs/{{job_id}}/trigger
```

3. **While job is running, kill the leader node**
```bash
docker stop scheduler-node-1
```

4. **Wait 30 seconds for orphan detection**

5. **Check job status from new leader**
```
GET {{node2_url}}/api/v1/jobs/{{job_id}}
```

6. **Verify job was recovered**
```javascript
pm.test("Orphaned job recovered", function() {
    var jsonData = pm.response.json();

    // Job should be rescheduled or marked as failed
    pm.expect(jsonData.status).to.be.oneOf(["PENDING", "FAILED", "RETRYING"]);

    console.log(`Job status after recovery: ${jsonData.status}`);
});
```

---

## Error Handling Tests

### Test 1: Invalid Job Name

**Request**:
```
POST {{base_url}}/api/v1/jobs
```
Body:
```json
{
  "name": "ab",
  "enabled": true
}
```

**Expected Response**: `400 Bad Request`
```json
{
  "timestamp": "2026-03-10T10:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/jobs",
  "validationErrors": [
    {
      "field": "name",
      "rejectedValue": "ab",
      "message": "Job name must be between 3 and 100 characters"
    }
  ]
}
```

---

### Test 2: Invalid Cron Expression

**Request**:
```
POST {{base_url}}/api/v1/jobs
```
Body:
```json
{
  "name": "invalid-cron-job",
  "cronExpression": "invalid cron",
  "enabled": true
}
```

**Expected Response**: `400 Bad Request`
```json
{
  "timestamp": "2026-03-10T10:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/jobs",
  "validationErrors": [
    {
      "field": "cronExpression",
      "rejectedValue": "invalid cron",
      "message": "Invalid cron expression format"
    }
  ]
}
```

---

### Test 3: Job Not Found

**Request**:
```
GET {{base_url}}/api/v1/jobs/999999
```

**Expected Response**: `404 Not Found`
```json
{
  "timestamp": "2026-03-10T10:40:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Job not found with id: 999999",
  "path": "/api/v1/jobs/999999"
}
```

---

### Test 4: Invalid State Transition

**Request**: Try to resume a job that's not paused
```
POST {{base_url}}/api/v1/jobs/{{job_id}}/resume
```
(where job status is RUNNING)

**Expected Response**: `400 Bad Request`
```json
{
  "timestamp": "2026-03-10T10:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot resume job in RUNNING state",
  "path": "/api/v1/jobs/1/resume"
}
```

---

### Test 5: Timeout Validation

**Request**:
```
POST {{base_url}}/api/v1/jobs
```
Body:
```json
{
  "name": "timeout-test",
  "timeoutSeconds": 5000,
  "enabled": true
}
```

**Expected Response**: `400 Bad Request`
```json
{
  "validationErrors": [
    {
      "field": "timeoutSeconds",
      "rejectedValue": 5000,
      "message": "Timeout cannot exceed 3600 seconds (1 hour)"
    }
  ]
}
```

---

## Postman Collection Structure

Organize your Postman collection as follows:

```
Distributed Job Scheduler API
├── 1. Job Management
│   ├── Create One-Time Job
│   ├── Create Recurring Job (Cron)
│   ├── Get Job by ID
│   ├── List All Jobs
│   ├── List Jobs by Status
│   ├── Update Job
│   ├── Delete Job
│   ├── Trigger Job
│   ├── Pause Job
│   └── Resume Job
├── 2. Job Execution
│   ├── Get Execution by ID
│   ├── Get Job Execution History
│   ├── List All Executions
│   └── List Executions by Status
├── 3. Cluster Management
│   ├── Get Cluster Status
│   ├── List All Nodes
│   └── Get Current Leader
├── 4. Error Handling Tests
│   ├── Invalid Job Name
│   ├── Invalid Cron Expression
│   ├── Job Not Found
│   ├── Invalid State Transition
│   └── Validation Errors
└── 5. Distributed Systems Tests
    ├── Verify Leader Election
    ├── Verify Fencing Tokens
    ├── Test Distributed Locking
    ├── Test Retry Logic
    └── Test Orphaned Job Recovery
```

---

## Quick Reference: Common Cron Expressions

| Expression | Description |
|------------|-------------|
| `0 0 * * * ?` | Every hour at minute 0 |
| `0 */15 * * * ?` | Every 15 minutes |
| `0 0 0 * * ?` | Daily at midnight |
| `0 0 12 * * ?` | Daily at noon |
| `0 0 0 * * MON` | Every Monday at midnight |
| `0 0 9 * * MON-FRI` | Weekdays at 9 AM |
| `0 0 0 1 * ?` | First day of every month |
| `0 0 */6 * * ?` | Every 6 hours |

---

## Tips and Best Practices

### 1. Use Environment Variables
- Store `base_url`, `job_id`, `execution_id` in environment variables
- Use `{{$timestamp}}` for unique job names
- Save IDs from responses for subsequent requests

### 2. Add Delays Between Requests
- Use `setTimeout()` in test scripts for async operations
- Wait 2-5 seconds after triggering jobs before checking status

### 3. Chain Requests
- Use Collection Runner for sequential test execution
- Set up dependencies using environment variables

### 4. Monitor Logs
- Check Docker logs: `docker logs scheduler-node-1 -f`
- Look for leader election, job execution, and error messages

### 5. Test Across Multiple Nodes
- Create separate environments for each node
- Verify read consistency across all nodes
- Test failover by switching between node URLs

---

## Troubleshooting Guide

### Common Postman Issues

#### Issue 1: "Could not get any response" Error

**Symptoms**:
- Red error message: "Could not get any response"
- No status code shown
- Request times out

**Solutions**:

1. **Verify Docker containers are running**
   ```bash
   docker ps
   ```
   Expected output: Should show `scheduler-node-1`, `mysql`, `redis` containers

2. **Check if port is accessible**
   ```bash
   curl http://localhost:8080/api/v1/cluster/status
   ```
   If this works but Postman doesn't, try disabling Postman's proxy:
   - Settings → Proxy → Turn off "Use System Proxy"

3. **Verify application started successfully**
   ```bash
   docker logs scheduler-node-1 --tail 50
   ```
   Look for: `Started DistributedJobSchedulerApplication`

4. **Check port mapping**
   ```bash
   docker port scheduler-node-1
   ```
   Expected: `8080/tcp -> 0.0.0.0:8080`

---

#### Issue 2: "Unresolved Variable" Warning

**Symptoms**:
- Orange/yellow text in URL: `{{base_url}}`
- Hover shows "Unresolved Variable"
- Request fails with 404 or connection error

**Solutions**:

1. **Activate your environment**
   - Top-right corner → Click environment dropdown
   - Select "Job Scheduler - Local"
   - Should change from "No Environment" to your environment name

2. **Verify variable exists**
   - Click eye icon 👁️ next to environment dropdown
   - Check if `base_url` is listed with value `http://localhost:8080`
   - If missing, add it in Environments panel

3. **Check variable scope**
   - Collection variables: `pm.collectionVariables.get("var")`
   - Environment variables: `pm.environment.get("var")`
   - Make sure you're using the right scope

---

#### Issue 3: 404 Not Found on All Endpoints

**Symptoms**:
- Status: `404 Not Found`
- Response body: HTML error page or JSON error

**Solutions**:

1. **Verify URL path is correct**
   - Correct: `{{base_url}}/api/v1/jobs`
   - Wrong: `{{base_url}}/jobs` (missing `/api/v1`)
   - Wrong: `{{base_url}}/api/v1/job` (missing `s`)

2. **Check base_url doesn't have trailing slash**
   - Correct: `http://localhost:8080`
   - Wrong: `http://localhost:8080/` (trailing slash causes `//api/v1/jobs`)

3. **Verify application context path**
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   If this returns 404, check `application.yml` for custom context path

---

#### Issue 4: 400 Bad Request - Validation Errors

**Symptoms**:
- Status: `400 Bad Request`
- Response shows validation errors

**Solutions**:

1. **Check request body format**
   - Make sure "Body" → "raw" → "JSON" is selected
   - Validate JSON syntax (use JSONLint.com if needed)
   - Check for missing required fields

2. **Common validation errors**:
   - **Job name too short**: Must be 3-100 characters
   - **Invalid cron expression**: Use valid cron format (e.g., `0 0 * * * ?`)
   - **Timeout out of range**: Must be 10-3600 seconds
   - **Max retries out of range**: Must be 0-10

3. **Example valid request**:
   ```json
   {
     "name": "valid-job-name",
     "description": "Optional description",
     "payload": "{}",
     "maxRetries": 3,
     "timeoutSeconds": 300,
     "enabled": true
   }
   ```

---

#### Issue 5: Tests Failing

**Symptoms**:
- Status code is 200 but tests show failures
- Test Results tab shows red X marks

**Solutions**:

1. **Check response structure**
   - Click "Body" tab to see actual response
   - Compare with expected structure in test script
   - Response might have different field names

2. **Use Postman Console for debugging**
   - View → Show Postman Console (or Alt+Ctrl+C)
   - Add `console.log(pm.response.json())` to see full response
   - Check for null values or missing fields

3. **Common test issues**:
   ```javascript
   // Wrong: Assumes field exists
   pm.expect(jsonData.jobs.length).to.be.above(0);

   // Right: Check field exists first
   pm.expect(jsonData).to.have.property("jobs");
   pm.expect(jsonData.jobs).to.be.an("array");
   if (jsonData.jobs.length > 0) {
       // Then check contents
   }
   ```

---

### Application-Specific Issues

#### Issue 6: Job Stays in PENDING Status

**Symptoms**:
- Job created successfully (201)
- Job status remains "PENDING" for minutes
- Job never transitions to "RUNNING" or "COMPLETED"

**Solutions**:

1. **Check if leader is elected**
   ```
   GET {{base_url}}/api/v1/cluster/leader
   ```
   - If 404: No leader elected, check Redis connection
   - If 200: Leader exists, check logs for errors

2. **Check application logs**
   ```bash
   docker logs scheduler-node-1 -f
   ```
   Look for:
   - `Leader elected: scheduler-node-1`
   - `Scheduling job: id=1`
   - Any error messages

3. **Verify job is enabled**
   ```
   GET {{base_url}}/api/v1/jobs/{{job_id}}
   ```
   Check: `"enabled": true`

4. **Check nextRunTime**
   - If `nextRunTime` is in the future, job will wait
   - For immediate execution, trigger manually:
     ```
     POST {{base_url}}/api/v1/jobs/{{job_id}}/trigger
     ```

---

#### Issue 7: Cron Job Not Executing

**Symptoms**:
- Recurring job created with cron expression
- `nextRunTime` is in the past but job didn't execute

**Solutions**:

1. **Verify cron expression is valid**
   - Test at: https://crontab.guru/
   - Common mistake: Using 5-field cron (Linux) instead of 6-field (Spring)
   - Spring format: `second minute hour day month weekday`
   - Example: `0 0 * * * ?` (every hour at minute 0)

2. **Check if job is enabled**
   ```
   GET {{base_url}}/api/v1/jobs/{{job_id}}
   ```
   Verify: `"enabled": true` and `"status": "PENDING"` or `"SCHEDULED"`

3. **Verify nextRunTime is calculated**
   - Should not be null
   - Should be in the future (unless job is due now)

4. **Check scheduler is running**
   ```bash
   docker logs scheduler-node-1 | grep "Scheduler started"
   ```

---

#### Issue 8: Fencing Token Validation Fails

**Symptoms**:
- Execution shows fencing token but validation fails
- Logs show "Invalid fencing token" errors

**Solutions**:

1. **Check epoch consistency**
   ```
   GET {{base_url}}/api/v1/cluster/status
   ```
   All nodes should have the same epoch number

2. **Verify fencing token format**
   - Expected format: `{epoch}-{timestamp}`
   - Example: `5-1710068405000`
   - First part is epoch, second is timestamp

3. **Check for split-brain scenario**
   - Multiple nodes think they're leader
   - Check Redis connection on all nodes
   - Restart cluster if needed:
     ```bash
     docker-compose down
     docker-compose up -d
     ```

---

#### Issue 9: Leader Failover Not Working

**Symptoms**:
- Killed leader node
- No new leader elected after 30+ seconds

**Solutions**:

1. **Check Redis is running**
   ```bash
   docker ps | grep redis
   docker logs redis
   ```

2. **Verify heartbeat interval**
   - Check `application.yml`: `scheduler.heartbeat-interval`
   - Default: 5 seconds
   - Leader election should happen within 15-30 seconds

3. **Check remaining nodes are healthy**
   ```
   GET {{node2_url}}/api/v1/cluster/status
   ```

4. **Restart cluster if stuck**
   ```bash
   docker-compose restart
   ```

---

### Postman Best Practices

#### Tip 1: Use Collection Runner for Sequential Tests

1. Click collection name → Click "Run" button
2. Select requests to run in order
3. Set delay between requests (e.g., 2000ms)
4. Click "Run Distributed Job Scheduler API"

**Use case**: Test complete workflow (create job → trigger → check status → verify execution)

---

#### Tip 2: Use Postman Console for Debugging

1. View → Show Postman Console (Alt+Ctrl+C / Cmd+Alt+C)
2. Add logging in test scripts:
   ```javascript
   console.log("Response:", JSON.stringify(pm.response.json(), null, 2));
   console.log("Job ID:", pm.environment.get("job_id"));
   ```
3. See all requests, responses, and console output

---

#### Tip 3: Save Responses as Examples

1. After successful request, click "Save Response"
2. Click "Save as Example"
3. Name it (e.g., "Success - Job Created")
4. Now you have documentation built into your collection!

---

#### Tip 4: Use Pre-Request Scripts for Dynamic Data

```javascript
// Generate unique job name
pm.collectionVariables.set("job_name", `job-${Date.now()}`);

// Generate random payload
pm.collectionVariables.set("random_value", Math.floor(Math.random() * 1000));

// Set current timestamp
pm.collectionVariables.set("current_time", new Date().toISOString());
```

Then use in request body:
```json
{
  "name": "{{job_name}}",
  "payload": "{\"value\": {{random_value}}}"
}
```

---

#### Tip 5: Export and Share Collections

1. Right-click collection → "Export"
2. Choose "Collection v2.1" format
3. Save as `Distributed_Job_Scheduler_API.postman_collection.json`
4. Share with team or commit to Git repository

**To import**:
1. Collections → Import
2. Select the JSON file
3. All requests, tests, and scripts are imported!

---

### Quick Diagnostic Checklist

Before asking for help, verify:

- ✅ Docker containers running: `docker ps`
- ✅ Application started: `docker logs scheduler-node-1 | grep Started`
- ✅ Postman environment activated (top-right corner)
- ✅ `base_url` variable set correctly (click eye icon 👁️)
- ✅ Collection headers configured (Content-Type, Accept)
- ✅ Request URL is correct (no typos, correct path)
- ✅ Request body is valid JSON (if POST/PUT)
- ✅ Leader elected: `GET /api/v1/cluster/leader` returns 200
- ✅ MySQL connected: Check logs for "HikariPool" messages
- ✅ Redis connected: Check logs for "Redisson" messages

---

## Complete Example Workflow

Here's a complete end-to-end workflow you can follow to test all major features.

### Workflow: Create, Execute, and Monitor a Job

#### Step 1: Verify Cluster is Healthy

**Request**: `GET {{base_url}}/api/v1/cluster/status`

**Expected Response**: 200 OK
```json
{
  "leaderNodeId": "scheduler-node-1",
  "totalNodes": 3,
  "healthyNodes": 3,
  ...
}
```

**Action**: Save `leaderNodeId` to environment variable

---

#### Step 2: Create a One-Time Job

**Request**: `POST {{base_url}}/api/v1/jobs`

**Body**:
```json
{
  "name": "demo-job-{{timestamp}}",
  "description": "Demo job for testing",
  "payload": "{\"duration\": 3000, \"message\": \"Hello World\"}",
  "maxRetries": 3,
  "timeoutSeconds": 300,
  "enabled": true
}
```

**Expected Response**: 201 Created

**Test Script**:
```javascript
pm.test("Job created", function() {
    pm.response.to.have.status(201);
    var jsonData = pm.response.json();
    pm.environment.set("job_id", jsonData.id);
    console.log(`Created Job ID: ${jsonData.id}`);
});
```

---

#### Step 3: Verify Job Details

**Request**: `GET {{base_url}}/api/v1/jobs/{{job_id}}`

**Expected Response**: 200 OK
```json
{
  "id": 1,
  "name": "demo-job-1710068405000",
  "status": "PENDING",
  "enabled": true,
  ...
}
```

---

#### Step 4: Wait for Job to Execute

**Wait**: 5 seconds (job should execute automatically)

**Request**: `GET {{base_url}}/api/v1/jobs/{{job_id}}`

**Expected Response**: 200 OK
```json
{
  "id": 1,
  "status": "COMPLETED",
  ...
}
```

---

#### Step 5: Check Execution History

**Request**: `GET {{base_url}}/api/v1/executions/job/{{job_id}}`

**Expected Response**: 200 OK
```json
{
  "executions": [
    {
      "id": 1,
      "jobId": 1,
      "status": "SUCCESS",
      "nodeId": "scheduler-node-1",
      "durationMs": 3000,
      "fencingToken": "1-1710068405000",
      ...
    }
  ],
  "totalElements": 1
}
```

**Test Script**:
```javascript
pm.test("Job executed successfully", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.executions.length).to.be.above(0);

    var execution = jsonData.executions[0];
    pm.expect(execution.status).to.eql("SUCCESS");
    pm.expect(execution.durationMs).to.be.above(0);

    pm.environment.set("execution_id", execution.id);
});
```

---

#### Step 6: Create a Recurring Job

**Request**: `POST {{base_url}}/api/v1/jobs`

**Body**:
```json
{
  "name": "hourly-job-{{timestamp}}",
  "description": "Runs every hour",
  "cronExpression": "0 0 * * * ?",
  "payload": "{\"reportType\": \"hourly\"}",
  "maxRetries": 5,
  "timeoutSeconds": 600,
  "enabled": true
}
```

**Expected Response**: 201 Created

**Test Script**:
```javascript
pm.test("Recurring job created", function() {
    pm.response.to.have.status(201);
    var jsonData = pm.response.json();
    pm.environment.set("recurring_job_id", jsonData.id);

    // Verify cron expression
    pm.expect(jsonData.cronExpression).to.eql("0 0 * * * ?");

    // Verify nextRunTime is in the future
    var nextRun = new Date(jsonData.nextRunTime);
    var now = new Date();
    pm.expect(nextRun.getTime()).to.be.above(now.getTime());
});
```

---

#### Step 7: Manually Trigger Recurring Job

**Request**: `POST {{base_url}}/api/v1/jobs/{{recurring_job_id}}/trigger`

**Expected Response**: 202 Accepted

**Test Script**:
```javascript
pm.test("Job triggered", function() {
    pm.response.to.have.status(202);
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.eql("PENDING");
});
```

---

#### Step 8: Pause the Recurring Job

**Request**: `POST {{base_url}}/api/v1/jobs/{{recurring_job_id}}/pause`

**Expected Response**: 200 OK
```json
{
  "id": 2,
  "status": "PAUSED",
  ...
}
```

---

#### Step 9: Resume the Job

**Request**: `POST {{base_url}}/api/v1/jobs/{{recurring_job_id}}/resume`

**Expected Response**: 200 OK
```json
{
  "id": 2,
  "status": "PENDING",
  ...
}
```

---

#### Step 10: Update Job Configuration

**Request**: `PUT {{base_url}}/api/v1/jobs/{{recurring_job_id}}`

**Body**:
```json
{
  "description": "Updated: Runs every 30 minutes",
  "cronExpression": "0 */30 * * * ?",
  "maxRetries": 10
}
```

**Expected Response**: 200 OK

**Test Script**:
```javascript
pm.test("Job updated", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.description).to.include("Updated");
    pm.expect(jsonData.cronExpression).to.eql("0 */30 * * * ?");
    pm.expect(jsonData.maxRetries).to.eql(10);
});
```

---

#### Step 11: List All Jobs

**Request**: `GET {{base_url}}/api/v1/jobs?page=0&size=10`

**Expected Response**: 200 OK

**Test Script**:
```javascript
pm.test("Jobs listed", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.jobs).to.be.an("array");
    pm.expect(jsonData.jobs.length).to.be.above(0);
    pm.expect(jsonData.totalElements).to.be.above(0);
});
```

---

#### Step 12: Filter Jobs by Status

**Request**: `GET {{base_url}}/api/v1/jobs?status=COMPLETED`

**Expected Response**: 200 OK

**Test Script**:
```javascript
pm.test("Filtered jobs", function() {
    var jsonData = pm.response.json();
    jsonData.jobs.forEach(function(job) {
        pm.expect(job.status).to.eql("COMPLETED");
    });
});
```

---

#### Step 13: Delete the One-Time Job

**Request**: `DELETE {{base_url}}/api/v1/jobs/{{job_id}}`

**Expected Response**: 204 No Content

**Test Script**:
```javascript
pm.test("Job deleted", function() {
    pm.response.to.have.status(204);
});
```

---

#### Step 14: Verify Job is Deleted

**Request**: `GET {{base_url}}/api/v1/jobs/{{job_id}}`

**Expected Response**: 404 Not Found

**Test Script**:
```javascript
pm.test("Job not found after deletion", function() {
    pm.response.to.have.status(404);
});
```

---

### Summary of Workflow

You've now tested:

✅ Cluster health check
✅ Job creation (one-time and recurring)
✅ Job retrieval
✅ Automatic job execution
✅ Execution history
✅ Manual job triggering
✅ Job state transitions (pause/resume)
✅ Job updates
✅ Job listing and filtering
✅ Job deletion

**Next Steps**:
- Test distributed features (leader failover, fencing tokens)
- Test error handling (invalid inputs, validation errors)
- Run Collection Runner to automate this workflow

---

## Postman Collection JSON (Import Ready)

Want to skip manual setup? Here's a minimal collection you can import:

```json
{
  "info": {
    "name": "Distributed Job Scheduler API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get Cluster Status",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{base_url}}/api/v1/cluster/status",
          "host": ["{{base_url}}"],
          "path": ["api", "v1", "cluster", "status"]
        }
      }
    },
    {
      "name": "Create One-Time Job",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"test-job-{{$timestamp}}\",\n  \"description\": \"Test job\",\n  \"payload\": \"{}\",\n  \"maxRetries\": 3,\n  \"timeoutSeconds\": 300,\n  \"enabled\": true\n}"
        },
        "url": {
          "raw": "{{base_url}}/api/v1/jobs",
          "host": ["{{base_url}}"],
          "path": ["api", "v1", "jobs"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "api_base_path",
      "value": "/api/v1"
    }
  ]
}
```

**To import**:
1. Copy the JSON above
2. Postman → Import → Raw text
3. Paste and click "Import"
4. Set up environment with `base_url` variable

---

## Conclusion

This guide provides comprehensive testing coverage for the Distributed Job Scheduler REST API. Use it to:

✅ **Verify all CRUD operations** - Create, read, update, delete jobs
✅ **Test distributed systems features** - Leader election, fencing tokens, distributed locking
✅ **Validate error handling** - Request validation, state transitions, edge cases
✅ **Monitor cluster health** - Node status, leader election, job metrics
✅ **Demonstrate system capabilities** - Perfect for interviews and demos
✅ **Automate testing** - Use Collection Runner and test scripts

---

## Additional Resources

### Project Documentation
- **`ARCHITECTURE.md`** - System design and distributed patterns
- **`DEVELOPMENT.md`** - Development workflow and progress tracking
- **`docs/PROJECT_CONTEXT.md`** - Project memory and technical context
- **`README.md`** - Project overview and setup instructions

### External Resources
- **Postman Learning Center**: https://learning.postman.com/
- **Cron Expression Tester**: https://crontab.guru/
- **JSON Validator**: https://jsonlint.com/
- **Spring Cron Documentation**: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html

### Interview Talking Points

When discussing this API in interviews, highlight:

1. **RESTful Design**: Proper HTTP verbs, status codes, resource naming
2. **Pagination**: Prevents overwhelming clients with large datasets
3. **Validation**: Bean Validation at API boundary ensures data integrity
4. **Idempotency**: PUT and DELETE operations are idempotent
5. **Error Handling**: Consistent error response format with field-level validation
6. **Observability**: Cluster status endpoints provide system health visibility
7. **Distributed Coordination**: Leader election, fencing tokens, distributed locking
8. **Fault Tolerance**: Automatic failover, orphaned job recovery, retry logic

---

## Feedback and Contributions

Found an issue or have a suggestion?

- Check the [Troubleshooting Guide](#troubleshooting-guide) first
- Review application logs: `docker logs scheduler-node-1`
- Check Postman Console for detailed error messages
- Verify environment variables are set correctly

---

**Happy Testing! 🚀**

**Last Updated**: 2026-03-12
**Version**: 1.0.0
**Maintained by**: Distributed Job Scheduler Team

