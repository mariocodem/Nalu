import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CuentaService } from '../../core/api/cuenta.service';
import { ErroresFormulario, erroresDe } from '../../core/api/problema';

type Campo = 'nombre' | 'apellido' | 'correo' | 'telefono' | 'contrasena' | 'aceptaPolitica';

const MENSAJES: Record<Campo, string> = {
  nombre: 'Escribe tu nombre',
  apellido: 'Escribe tu apellido',
  correo: 'Ingresa un correo válido',
  telefono: 'El teléfono debe tener entre 7 y 15 dígitos',
  contrasena: 'La contraseña debe tener al menos 8 caracteres',
  aceptaPolitica: 'Debes aceptar la política de tratamiento de datos',
};

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="pagina pagina--estrecha">
      <h1>Crear cuenta</h1>
      <form class="formulario" [formGroup]="formulario" (ngSubmit)="enviar()" novalidate>
        @if (errores().general; as general) {
          <p class="aviso" role="alert">{{ general }}</p>
        }
        <div class="formulario__fila">
          <div class="campo">
            <label for="nombre">Nombre</label>
            <input id="nombre" formControlName="nombre" autocomplete="given-name" maxlength="60"
                   [attr.aria-invalid]="invalido('nombre')" aria-describedby="nombre-error" />
            @if (invalido('nombre')) { <p id="nombre-error" class="campo__error">{{ mensaje('nombre') }}</p> }
          </div>
          <div class="campo">
            <label for="apellido">Apellido</label>
            <input id="apellido" formControlName="apellido" autocomplete="family-name" maxlength="60"
                   [attr.aria-invalid]="invalido('apellido')" aria-describedby="apellido-error" />
            @if (invalido('apellido')) { <p id="apellido-error" class="campo__error">{{ mensaje('apellido') }}</p> }
          </div>
        </div>
        <div class="campo">
          <label for="correo">Correo electrónico</label>
          <input id="correo" type="email" formControlName="correo" autocomplete="email" maxlength="120"
                 [attr.aria-invalid]="invalido('correo')" aria-describedby="correo-error" />
          @if (invalido('correo')) { <p id="correo-error" class="campo__error">{{ mensaje('correo') }}</p> }
        </div>
        <div class="campo">
          <label for="telefono">Teléfono</label>
          <input id="telefono" type="tel" formControlName="telefono" autocomplete="tel" inputmode="tel"
                 [attr.aria-invalid]="invalido('telefono')" aria-describedby="telefono-error" />
          @if (invalido('telefono')) { <p id="telefono-error" class="campo__error">{{ mensaje('telefono') }}</p> }
        </div>
        <div class="campo">
          <label for="contrasena">Contraseña</label>
          <input id="contrasena" type="password" formControlName="contrasena" autocomplete="new-password"
                 [attr.aria-invalid]="invalido('contrasena')" aria-describedby="contrasena-error contrasena-ayuda" />
          <p id="contrasena-ayuda" class="nota">Mínimo 8 caracteres.</p>
          @if (invalido('contrasena')) { <p id="contrasena-error" class="campo__error">{{ mensaje('contrasena') }}</p> }
        </div>
        <div class="campo campo--casilla">
          <!-- TODO: enlazar el texto de la política cuando el propietario lo entregue. -->
          <input id="aceptaPolitica" type="checkbox" formControlName="aceptaPolitica"
                 [attr.aria-invalid]="invalido('aceptaPolitica')" aria-describedby="politica-error" />
          <label for="aceptaPolitica">Acepto la política de tratamiento de datos personales (Ley 1581 de 2012).</label>
        </div>
        @if (invalido('aceptaPolitica')) {
          <p id="politica-error" class="campo__error">{{ mensaje('aceptaPolitica') }}</p>
        }
        <div class="campo campo--casilla">
          <input id="aceptaComunicaciones" type="checkbox" formControlName="aceptaComunicaciones" />
          <label for="aceptaComunicaciones">Quiero recibir novedades y lanzamientos por correo.</label>
        </div>
        <button class="boton boton--primario" type="submit" [disabled]="enviando()">
          {{ enviando() ? 'Creando cuenta…' : 'Crear cuenta' }}
        </button>
        <p class="nota">¿Ya tienes cuenta? <a routerLink="/cuenta/login">Ingresa</a>.</p>
      </form>
    </section>
  `,
})
export class Registro {
  private readonly cuenta = inject(CuentaService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(60)]],
    apellido: ['', [Validators.required, Validators.maxLength(60)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    telefono: ['', [Validators.required, Validators.pattern(/^\+?[0-9 ]{7,20}$/)]],
    contrasena: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
    aceptaPolitica: [false, Validators.requiredTrue],
    aceptaComunicaciones: [false],
  });
  protected readonly enviando = signal(false);
  protected readonly errores = signal<ErroresFormulario>({ general: null, campos: {} });
  private intentado = false;

  protected invalido(campo: Campo): boolean {
    const control = this.formulario.controls[campo];
    return !!this.errores().campos[campo] || (control.invalid && (control.touched || this.intentado));
  }

  protected mensaje(campo: Campo): string {
    return this.errores().campos[campo] ?? MENSAJES[campo];
  }

  protected enviar(): void {
    this.intentado = true;
    this.errores.set({ general: null, campos: {} });
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.cuenta.registrar(this.formulario.getRawValue()).subscribe({
      next: (sesion) => {
        this.auth.establecerSesion(sesion);
        void this.router.navigateByUrl('/');
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        const errores = erroresDe(error);
        // 409: el correo ya está registrado; se muestra junto al campo.
        if (error && (error as { status?: number }).status === 409 && errores.general) {
          this.errores.set({ general: null, campos: { correo: errores.general } });
        } else {
          this.errores.set(errores);
        }
      },
    });
  }
}
