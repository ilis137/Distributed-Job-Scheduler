/**
 * Cluster status response matching backend ClusterStatusResponse DTO
 */
export interface ClusterStatus {
  nodes: NodeStatus[];
  leaderNodeId: string | null;
  totalNodes: number;
  healthyNodes: number;      // Backend uses 'healthyNodes', not 'activeNodes'
  totalJobs: number;
  activeJobs: number;
  pendingJobs: number;
  failedJobs: number;
}

/**
 * Node status matching backend NodeStatusResponse DTO
 */
export interface NodeStatus {
  nodeId: string;
  role: NodeRole;            // Backend uses 'role' (LEADER/FOLLOWER), not 'isLeader'
  healthy: boolean;          // Backend uses 'healthy', not 'status'
  epoch: number;
  lastHeartbeat: string;
  startTime: string;         // Backend includes startTime
  version: string;           // Backend includes version
}

/**
 * Node role enum matching backend NodeRole
 */
export enum NodeRole {
  LEADER = 'LEADER',
  FOLLOWER = 'FOLLOWER'
}

