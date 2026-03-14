import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { JobService } from '../../../core/services/job.service';
import { Job, JobStatus } from '../../../core/models/job.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-job-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatMenuModule
  ],
  templateUrl: './job-list.component.html',
  styleUrls: ['./job-list.component.scss']
})
export class JobListComponent implements OnInit, OnDestroy {
  jobs: Job[] = [];
  displayedColumns: string[] = ['id', 'name', 'status', 'cronExpression', 'nextRunTime', 'actions'];
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  loading = false;
  error: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(private jobService: JobService) {}

  ngOnInit(): void {
    this.loadJobs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadJobs(): void {
    this.loading = true;
    this.error = null;

    this.jobService.getJobs(this.pageIndex, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.jobs = response.jobs;  // Backend uses 'jobs', not 'content'
          this.totalElements = response.totalElements;
          this.loading = false;
        },
        error: (err) => {
          this.error = err.message;
          this.loading = false;
        }
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadJobs();
  }

  pauseJob(job: Job): void {
    this.jobService.pauseJob(job.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadJobs(),
        error: (err) => this.error = err.message
      });
  }

  resumeJob(job: Job): void {
    this.jobService.resumeJob(job.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadJobs(),
        error: (err) => this.error = err.message
      });
  }

  triggerJob(job: Job): void {
    this.jobService.triggerJob(job.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadJobs(),
        error: (err) => this.error = err.message
      });
  }

  deleteJob(job: Job): void {
    if (confirm(`Are you sure you want to delete job "${job.name}"?`)) {
      this.jobService.deleteJob(job.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => this.loadJobs(),
          error: (err) => this.error = err.message
        });
    }
  }

  getStatusClass(status: JobStatus): string {
    return `status-${status.toLowerCase()}`;
  }

  /**
   * Checks if a timestamp is in the future.
   * Returns true if the timestamp is after the current time.
   *
   * @param timestamp ISO 8601 timestamp string
   * @returns true if timestamp is in the future, false otherwise
   */
  isFutureTime(timestamp: string | null): boolean {
    if (!timestamp) {
      return false;
    }
    const nextRunDate = new Date(timestamp);
    const now = new Date();
    return nextRunDate > now;
  }

  /**
   * Formats the next run time for display.
   * Returns formatted timestamp if in the future, otherwise returns "N/A".
   *
   * @param timestamp ISO 8601 timestamp string
   * @returns Formatted display string
   */
  getNextRunDisplay(timestamp: string | null): string {
    if (!timestamp) {
      return 'N/A';
    }
    return this.isFutureTime(timestamp) ? timestamp : '-';
  }
}

