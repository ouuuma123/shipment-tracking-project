import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ShipmentService } from '../services/shipment-service';
import { CreateShipmentRequest } from '../models/shipment.model';
import { catchError, of } from 'rxjs';

@Component({
  imports: [ReactiveFormsModule, CommonModule],
  selector: 'app-create-shipment',
  styleUrl: './create-shipment.css',
  templateUrl: './create-shipment.html',
})
export class CreateShipment {
  private shipmentService = inject(ShipmentService);
  private fb = inject(FormBuilder); // ce service permet de créer des formulaires
  isSubmitting = signal(false);

  // FormGroup : permet de récupérer les valeurs du formulaire
  // ici shipmentForm : est notre formulaire avec les champs origin, destination et estimatedDelivery
  shipmentForm: FormGroup = this.fb.group({
    origin: ['', [Validators.required, Validators.minLength(2)]],
    destination: ['', [Validators.required, Validators.minLength(2)]],
    estimatedDelivery: [''],
  });

  createShipment(): void {
    if(this.shipmentForm.invalid || this.isSubmitting()) {
      this.shipmentForm.markAllAsTouched();
      return;
    }
    this.isSubmitting.set(true);
    const shipment: CreateShipmentRequest = this.shipmentForm.value;
    this.shipmentService.createShipment(shipment)
      .pipe(
        catchError(() => {
          this.isSubmitting.set(false);
          return of(null);
        }),
      )
      .subscribe(() => {
        this.shipmentForm.reset();
        this.isSubmitting.set(false);
      });
  }

  hasError(fieldName: string): boolean {
    const field = this.shipmentForm.get(fieldName); // récupère le champ du formulaire
    return !!(field?.invalid && field?.touched); // si le champ est invalide et touché
  }

  getErrorMessage(fieldName: string): string {
    const field = this.shipmentForm.get(fieldName);
    if (field?.hasError('required')) {
      return 'This field is required';
    }
    if(field?.hasError('minlength')) {
      return 'Please enter at least 2 characters';
    }
    return '';
  }

}
