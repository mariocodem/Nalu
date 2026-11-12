import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ErroresFormulario, erroresDe } from '../../core/api/problema';
import { destinoSeguro } from '../destino-seguro';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="pagina pagina--estrecha">
      <h1>Ingresar</h1>
      <form class="formulario" [formGroup]="formulario" (ngSubmit)="enviar()" novalidate>
        @if (errores().general; as general) {
          <p class="aviso" role="alert">{{ general }}</p>
        }
        <div class="campo">
          <label for="correo">Correo electrónico</label>
          <input id="correo" type="email" formControlName="correo" autocomplete="email"
                 [attr.aria-invalid]="invalido('correo')" aria-describedby="correo-error" />
          @if (invalido('correo')) {
            <p id="correo-error" class="campo__error">{{ errores().campos['correo'] || 'Ingresa un correo válido' }}</p>
          }
        </div>
        <div class="campo">
          <label for="contrasena">Contraseña</label>
          <input id="contrasena" type="password" formControlName="contrasena" autocomplete="current-password"
                 [attr.aria-invalid]="invalido('contrasena')" aria-describedby="contrasena-error" />
          @if (invalido('contrasena')) {
            <p id="contrasena-error" class="campo__error">{{ errores().campos['contrasena'] || 'Ingresa tu contraseña' }}</p>
          }
        </div>
        <button class="boton boton--primario" type="submit" [disabled]="enviando()">
          {{ enviando() ? 'Ingresando…' : 'Ingresar' }}
        </button>
        <p class="nota">¿No tienes cuenta? <a routerLink="/cuenta/registro">Crea una</a>.</p>
      </form>
    </section>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', Validators.required],
  });
  protected readonly enviando = signal(false);
  protected readonly errores = signal<ErroresFormulario>({ general: null, campos: {} });
  private intentado = false;

  protected invalido(campo: 'correo' | 'contrasena'): boolean {
    const control = this.formulario.controls[campo];
    return !!this.errores().campos[campo] || (control.invalid && (control.touched || this.intentado));
  }

  protected enviar(): void {
    this.intentado = true;
    this.errores.set({ general: null, campos: {} });
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { correo, contrasena } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.auth.login(correo, contrasena).subscribe({
      next: () => {
        const destino = destinoSeguro(this.ruta.snapshot.queryParamMap.get('redirect'));
        void this.router.navigateByUrl(destino);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.errores.set(erroresDe(error));
      },
    });
  }
}
