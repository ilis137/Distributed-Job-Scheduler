import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';
import { JobService } from '../../../core/services/job.service';

@Component({
  selector: 'app-job-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatCardModule
  ],
  templateUrl: './job-form.component.html',
  styleUrls: ['./job-form.component.scss']
})
export class JobFormComponent implements OnInit {
  jobForm: FormGroup;
  isEditMode = false;
  jobId: number | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private jobService: JobService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.jobForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      description: [''],
      cronExpression: [''],
      enabled: [true],
      maxRetries: [3, [Validators.min(0), Validators.max(10)]],
      timeoutSeconds: [300, [Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.jobId = parseInt(id, 10);
      this.loadJob();
    }
  }

  loadJob(): void {
    if (!this.jobId) return;

    this.loading = true;
    this.jobService.getJobById(this.jobId).subscribe({
      next: (job) => {
        this.jobForm.patchValue(job);
        this.loading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.jobForm.invalid) return;

    this.loading = true;
    const formValue = this.jobForm.value;

    const request = this.isEditMode
      ? this.jobService.updateJob(this.jobId!, formValue)
      : this.jobService.createJob(formValue);

    request.subscribe({
      next: () => {
        this.router.navigate(['/jobs']);
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/jobs']);
  }
}

