import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { JobService } from '../../../core/services/job.service';
import { JobExecutionService } from '../../../core/services/job-execution.service';
import { Job } from '../../../core/models/job.model';
import { JobExecution } from '../../../core/models/job-execution.model';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './job-detail.component.html',
  styleUrls: ['./job-detail.component.scss']
})
export class JobDetailComponent implements OnInit {
  job: Job | null = null;
  executions: JobExecution[] = [];
  loading = false;
  error: string | null = null;
  displayedColumns = ['id', 'status', 'nodeId', 'startTime', 'endTime', 'retryAttempt'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobService: JobService,
    private executionService: JobExecutionService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadJob(parseInt(id, 10));
      this.loadExecutions(parseInt(id, 10));
    }
  }

  loadJob(id: number): void {
    this.loading = true;
    this.jobService.getJobById(id).subscribe({
      next: (job) => {
        this.job = job;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  loadExecutions(jobId: number): void {
    this.executionService.getJobExecutionHistory(jobId, 0, 10).subscribe({
      next: (response) => {
        this.executions = response.executions;  // Backend uses 'executions', not 'content'
      },
      error: (err) => {
        console.error('Failed to load executions:', err);
      }
    });
  }

  triggerJob(): void {
    if (!this.job) return;
    this.jobService.triggerJob(this.job.id).subscribe({
      next: () => this.loadJob(this.job!.id),
      error: (err) => this.error = err.message
    });
  }

  deleteJob(): void {
    if (!this.job) return;
    if (confirm(`Delete job "${this.job.name}"?`)) {
      this.jobService.deleteJob(this.job.id).subscribe({
        next: () => this.router.navigate(['/jobs']),
        error: (err) => this.error = err.message
      });
    }
  }
}

