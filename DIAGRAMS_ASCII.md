# Distributed Job Scheduler - ASCII Architecture Diagrams

## Quick Reference Diagrams

### 1. High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                                       │
│  ┌──────────────────┐              ┌──────────────────┐                    │
│  │   Angular UI     │              │  External API    │                    │
│  │  (Job Dashboard) │              │    Clients       │                    │
│  └────────┬─────────┘              └────────┬─────────┘                    │
└───────────┼──────────────────────────────────┼──────────────────────────────┘
            │                                  │
            │         HTTPS/REST               │
            └──────────────┬───────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────────────────┐
│                      LOAD BALANCER (NGINX)                                   │
│                   Routes to any healthy node                                 │
└──────────────────────────┬───────────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼────────┐  ┌──────▼───────┐  ┌──────▼───────┐
│  Node 1        │  │  Node 2      │  │  Node 3      │
│  LEADER ★      │  │  FOLLOWER    │  │  FOLLOWER    │
│  Epoch: 5      │  │  Standby     │  │  Standby     │
│                │  │              │  │              │
│ ┌────────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │JobScheduler│ │  │ │ Monitoring│ │  │ │Monitoring│ │
│ │  (Active)  │ │  │ │  Leader   │ │  │ │  Leader  │ │
│ └────────────┘ │  │ └──────────┘ │  │ └──────────┘ │
│ ┌────────────┐ │  │              │  │              │
│ │JobExecutor │ │  │              │  │              │
│ │VirtualThrd │ │  │              │  │              │
│ └────────────┘ │  │              │  │              │
└───────┬────────┘  └──────┬───────┘  └──────┬───────┘
        │                  │                  │
        │    Heartbeat     │    Monitor       │    Monitor
        │    Every 3s      │    Every 5s      │    Every 5s
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────────────────┐
│                    REDIS CLUSTER (Coordination)                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Key: scheduler:leader                                               │    │
│  │ Value: "node1:epoch5:1709812345"                                    │    │
│  │ TTL: 10 seconds                                                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Key: job:lock:12345                                                 │    │
│  │ Value: "node1:exec-uuid-789:epoch5"                                 │    │
│  │ TTL: 60 seconds                                                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Key: job:idempotency:client-key-xyz                                 │    │
│  │ Value: "exec-uuid-789"                                              │    │
│  │ TTL: 24 hours                                                       │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                    MYSQL DATABASE (Persistent Storage)                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   jobs           │  │ job_executions   │  │  audit_log       │          │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤          │
│  │ id               │  │ id               │  │ id               │          │
│  │ name             │  │ execution_id     │  │ entity_type      │          │
│  │ cron_expression  │  │ job_id (FK)      │  │ entity_id        │          │
│  │ payload          │  │ node_id (FK)     │  │ action           │          │
│  │ status           │  │ fencing_token    │  │ old_value        │          │
│  │ next_run_time    │  │ status           │  │ new_value        │          │
│  │ retry_count      │  │ start_time       │  │ created_at       │          │
│  │ max_retries      │  │ end_time         │  └──────────────────┘          │
│  │ created_at       │  │ result           │                                 │
│  └──────────────────┘  │ error_message    │                                 │
│                        └──────────────────┘                                 │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY STACK                                       │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐              │
│  │  Prometheus  │─────▶│   Grafana    │─────▶│ AlertManager │              │
│  │  (Metrics)   │      │ (Dashboards) │      │  (Alerts)    │              │
│  └──────────────┘      └──────────────┘      └──────────────┘              │
│         ▲                                            │                       │
│         │                                            ▼                       │
│    Scrape /actuator/prometheus          ┌────────────────────┐             │
│         │                                │  PagerDuty/Slack   │             │
│         │                                └────────────────────┘             │
└─────────┼──────────────────────────────────────────────────────────────────┘
          │
    All Scheduler Nodes
```

### 2. Leader Election Flow

```
TIME: T=0s (System Startup)
─────────────────────────────────────────────────────────────────────────
Node 1: SET scheduler:leader "node1:epoch1:ts" NX PX 10000
Redis:  ✓ OK (Lock acquired)
Node 1: → Transition to LEADER state
Node 1: → Start JobScheduler thread

Node 2: SET scheduler:leader "node2:epoch1:ts" NX PX 10000
Redis:  ✗ NULL (Lock already held by Node 1)
Node 2: → Transition to FOLLOWER state

Node 3: SET scheduler:leader "node3:epoch1:ts" NX PX 10000
Redis:  ✗ NULL (Lock already held by Node 1)
Node 3: → Transition to FOLLOWER state

─────────────────────────────────────────────────────────────────────────
TIME: T=3s, T=6s, T=9s (Heartbeat)
─────────────────────────────────────────────────────────────────────────
Node 1: SET scheduler:leader "node1:epoch1:ts" XX PX 10000
Redis:  ✓ OK (Lease renewed)
Node 1: → Still LEADER

Node 2: GET scheduler:leader
Redis:  "node1:epoch1:ts"
Node 2: → Still FOLLOWER

Node 3: GET scheduler:leader
Redis:  "node1:epoch1:ts"
Node 3: → Still FOLLOWER

─────────────────────────────────────────────────────────────────────────
TIME: T=12s (Leader Crash)
─────────────────────────────────────────────────────────────────────────
Node 1: ❌ CRASH (No more heartbeats)

─────────────────────────────────────────────────────────────────────────
TIME: T=22s (TTL Expired, Failover)
─────────────────────────────────────────────────────────────────────────
Redis:  DEL scheduler:leader (TTL expired after 10s)

Node 2: GET scheduler:leader
Redis:  NULL
Node 2: SET scheduler:leader "node2:epoch2:ts" NX PX 10000
Redis:  ✓ OK (Lock acquired)
Node 2: → Transition to LEADER state
Node 2: → Increment epoch to 2
Node 2: → Start JobScheduler thread

Node 3: GET scheduler:leader
Redis:  "node2:epoch2:ts"
Node 3: → Still FOLLOWER

─────────────────────────────────────────────────────────────────────────
TIME: T=30s (Old Leader Recovers)
─────────────────────────────────────────────────────────────────────────
Node 1: 🔄 RECOVERY
Node 1: SET scheduler:leader "node1:epoch1:ts" XX PX 10000
Redis:  ✗ NULL (Not current leader)
Node 1: → Transition to FOLLOWER state
Node 1: → Stop JobScheduler thread

RESULT: Node 2 is now LEADER with epoch2
        Fencing tokens prevent Node 1 from corrupting state
```

### 3. Job Execution with Distributed Lock

```
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: Job Submission                                              │
└─────────────────────────────────────────────────────────────────────┘
User → POST /api/jobs {name, cron, payload}
     → JobController.createJob()
     → JobService.save()
     → MySQL: INSERT INTO jobs (status='PENDING', nextRunTime='2026-03-07 10:00:00')
     ← 201 Created {jobId: 12345}

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: Leader Polls for Due Jobs                                   │
└─────────────────────────────────────────────────────────────────────┘
Leader → @Scheduled(fixedDelay=1000)
       → SELECT * FROM jobs WHERE nextRunTime <= NOW() AND status='PENDING'
       ← [Job 12345, Job 12346, ...]

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3: Distributed Lock Acquisition                                │
└─────────────────────────────────────────────────────────────────────┘
Worker Thread → Redis: SET job:lock:12345 "node1:exec-789:epoch5" NX PX 60000
              ← OK (Lock acquired)
              → MySQL: UPDATE jobs SET status='RUNNING' WHERE id=12345
              → MySQL: INSERT INTO job_executions (fencingToken='epoch5')

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: Job Execution                                                │
└─────────────────────────────────────────────────────────────────────┘
Worker Thread → Execute job logic (HTTP call, DB operation, etc.)
              ← Result: SUCCESS

              → MySQL: UPDATE job_executions SET status='COMPLETED'
              → MySQL: UPDATE jobs SET status='PENDING', nextRunTime=calculateNext(cron)
              → Redis: SET job:idempotency:key "exec-789" EX 86400
              → Redis: DEL job:lock:12345
              → Prometheus: job_execution_success_total++

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 5: Retry on Failure                                            │
└─────────────────────────────────────────────────────────────────────┘
Worker Thread → Execute job logic
              ← Result: FAILURE (HTTP 500)

              → Calculate backoff: delay = min(30 * 2^retryCount, 300) + jitter
              → MySQL: UPDATE jobs SET retryCount=retryCount+1, nextRunTime=NOW()+delay
              → MySQL: UPDATE job_executions SET status='FAILED_RETRYING'
              → Redis: DEL job:lock:12345
              → Prometheus: job_execution_retry_total++
```


