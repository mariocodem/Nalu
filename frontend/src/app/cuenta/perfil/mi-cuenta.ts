import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CuentaService } from '../../core/api/cuenta.service';
import { Direccion, DireccionRequest } from '../../core/api/modelos';
import { ErroresFormulario, erroresDe } from '../../core/api/problema';

const SIN_ERRORES: ErroresFormulario = { general: null, campos: {} };
const TELEFONO = /^\+?[0-9 ]{7,20}$/;

/** RF02: perfil y direcciones de envío del cliente. */
@Component({
  selector: 'app-mi-cuenta',
  imports: [ReactiveFormsModule],
  template: `
    <section class="pagina">
      <h1>Mi cuenta</h1>

      <section class="panel-seccion" aria-labelledby="titulo-perfil">
        <h2 id="titulo-perfil">Datos de contacto</h2>
        <form class="formulario" [formGroup]="perfil" (ngSubmit)="guardarPerfil()" novalidate>
          @if (erroresPerfil().general; as g) { <p class="aviso" role="alert">{{ g }}</p> }
          @if (perfilGuardado()) { <p class="aviso aviso--exito" role="status">Tus datos se guardaron.</p> }
          <p class="nota">Correo: {{ correo() }}</p>
          <div class="formulario__fila">
            <div class="campo">
              <label for="p-nombre">Nombre</label>
              <input id="p-nombre" formControlName="nombre" autocomplete="given-name"
                     [attr.aria-invalid]="!!erroresPerfil().campos['nombre']" />
              @if (erroresPerfil().campos['nombre']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
            <div class="campo">
              <label for="p-apellido">Apellido</label>
              <input id="p-apellido" formControlName="apellido" autocomplete="family-name"
                     [attr.aria-invalid]="!!erroresPerfil().campos['apellido']" />
              @if (erroresPerfil().campos['apellido']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
          </div>
          <div class="formulario__fila">
            <div class="campo">
              <label for="p-telefono">Teléfono</label>
              <input id="p-telefono" type="tel" formControlName="telefono" autocomplete="tel"
                     [attr.aria-invalid]="!!erroresPerfil().campos['telefono']" />
              @if (erroresPerfil().campos['telefono']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
            <div class="campo">
              <label for="p-documento">Documento (opcional)</label>
              <input id="p-documento" formControlName="documento" maxlength="20" />
            </div>
          </div>
          <div class="campo campo--casilla">
            <input id="p-comunicaciones" type="checkbox" formControlName="aceptaComunicaciones" />
            <label for="p-comunicaciones">Quiero recibir novedades y lanzamientos por correo.</label>
          </div>
          <div class="acciones">
            <button class="boton boton--primario" type="submit" [disabled]="perfil.invalid || guardandoPerfil()">
              Guardar datos
            </button>
          </div>
        </form>
      </section>

      <section class="panel-seccion" aria-labelledby="titulo-direcciones">
        <h2 id="titulo-direcciones">Direcciones de envío</h2>
        @if (direcciones().length === 0) {
          <p class="nota">Todavía no tienes direcciones guardadas.</p>
        }
        <ul class="direcciones">
          @for (d of direcciones(); track d.id) {
            <li class="direccion">
              <div>
                <strong>{{ d.etiqueta }}</strong>
                <p>{{ d.destinatario }} · {{ d.telefono }}<br />{{ d.direccion }}, {{ d.ciudad }}, {{ d.departamento }}
                  @if (d.codigoPostal) { ({{ d.codigoPostal }}) }</p>
              </div>
              <div class="acciones">
                <button type="button" class="boton boton--secundario boton--pequeno" (click)="editar(d)">Editar</button>
                <button type="button" class="boton boton--secundario boton--pequeno" (click)="eliminar(d)"
                        [attr.aria-label]="'Eliminar la dirección ' + d.etiqueta">Eliminar</button>
              </div>
            </li>
          }
        </ul>

        <h3 class="subtitulo">{{ editando() ? 'Editar dirección' : 'Nueva dirección' }}</h3>
        <form class="formulario" [formGroup]="direccion" (ngSubmit)="guardarDireccion()" novalidate>
          @if (erroresDireccion().general; as g) { <p class="aviso" role="alert">{{ g }}</p> }
          <div class="formulario__fila">
            @for (c of camposDireccion; track c.nombre) {
              <div class="campo">
                <label [for]="'d-' + c.nombre">{{ c.etiqueta }}</label>
                <input [id]="'d-' + c.nombre" [formControlName]="c.nombre" [attr.autocomplete]="c.autocompletar"
                       [attr.maxlength]="c.maximo" [attr.aria-invalid]="!!erroresDireccion().campos[c.nombre]" />
                @if (erroresDireccion().campos[c.nombre]; as e) { <p class="campo__error">{{ e }}</p> }
              </div>
            }
          </div>
          <div class="acciones">
            <button class="boton boton--primario" type="submit" [disabled]="direccion.invalid || guardandoDireccion()">
              {{ editando() ? 'Guardar cambios' : 'Añadir dirección' }}
            </button>
            @if (editando()) {
              <button type="button" class="boton boton--secundario" (click)="cancelarEdicion()">Cancelar</button>
            }
          </div>
        </form>
      </section>
    </section>
  `,
  styles: `
    .direcciones { list-style: none; margin: 0 0 1.5rem; padding: 0; display: grid; gap: 0.75rem; }
    .direccion {
      display: flex; flex-wrap: wrap; justify-content: space-between; gap: 0.75rem;
      padding: 0.9rem 1rem; border: 1px solid var(--regla-suave);
    }
    .direccion p { margin: 0.25rem 0 0; color: var(--tinta-2); }
    .subtitulo { font-size: 1.1rem; margin: 0 0 0.75rem; }
  `,
})
export class MiCuenta implements OnInit {
  private readonly cuenta = inject(CuentaService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly correo = signal('');
  protected readonly direcciones = signal<Direccion[]>([]);
  protected readonly editando = signal<Direccion | null>(null);
  protected readonly erroresPerfil = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly erroresDireccion = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly perfilGuardado = signal(false);
  protected readonly guardandoPerfil = signal(false);
  protected readonly guardandoDireccion = signal(false);

  protected readonly perfil = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(60)]],
    apellido: ['', [Validators.required, Validators.maxLength(60)]],
    telefono: ['', [Validators.required, Validators.pattern(TELEFONO)]],
    documento: [''],
    aceptaComunicaciones: [false],
  });

  protected readonly direccion = this.fb.group({
    etiqueta: ['', [Validators.required, Validators.maxLength(40)]],
    destinatario: ['', [Validators.required, Validators.maxLength(120)]],
    departamento: ['', [Validators.required, Validators.maxLength(60)]],
    ciudad: ['', [Validators.required, Validators.maxLength(60)]],
    direccion: ['', [Validators.required, Validators.maxLength(160)]],
    codigoPostal: ['', Validators.maxLength(12)],
    telefono: ['', [Validators.required, Validators.pattern(TELEFONO)]],
  });

  protected readonly camposDireccion = [
    { nombre: 'etiqueta', etiqueta: 'Etiqueta (Casa, Oficina…)', maximo: 40, autocompletar: 'off' },
    { nombre: 'destinatario', etiqueta: 'Quién recibe', maximo: 120, autocompletar: 'name' },
    { nombre: 'departamento', etiqueta: 'Departamento', maximo: 60, autocompletar: 'address-level1' },
    { nombre: 'ciudad', etiqueta: 'Ciudad', maximo: 60, autocompletar: 'address-level2' },
    { nombre: 'direccion', etiqueta: 'Dirección', maximo: 160, autocompletar: 'street-address' },
    { nombre: 'codigoPostal', etiqueta: 'Código postal (opcional)', maximo: 12, autocompletar: 'postal-code' },
    { nombre: 'telefono', etiqueta: 'Teléfono de contacto', maximo: 20, autocompletar: 'tel' },
  ] as const;

  ngOnInit(): void {
    this.cuenta.perfil().subscribe({
      next: (p) => {
        this.correo.set(p.correo);
        this.perfil.setValue({
          nombre: p.nombre,
          apellido: p.apellido,
          telefono: p.telefono ?? '',
          documento: p.documento ?? '',
          aceptaComunicaciones: p.aceptaComunicaciones,
        });
      },
      error: (e: unknown) => this.erroresPerfil.set(erroresDe(e)),
    });
    this.cargarDirecciones();
  }

  protected guardarPerfil(): void {
    this.perfilGuardado.set(false);
    this.erroresPerfil.set(SIN_ERRORES);
    this.guardandoPerfil.set(true);
    const v = this.perfil.getRawValue();
    this.cuenta.actualizarPerfil({ ...v, documento: v.documento || null }).subscribe({
      next: () => {
        this.guardandoPerfil.set(false);
        this.perfilGuardado.set(true);
      },
      error: (e: unknown) => {
        this.guardandoPerfil.set(false);
        this.erroresPerfil.set(erroresDe(e));
      },
    });
  }

  protected editar(d: Direccion): void {
    this.editando.set(d);
    this.erroresDireccion.set(SIN_ERRORES);
    this.direccion.setValue({
      etiqueta: d.etiqueta,
      destinatario: d.destinatario,
      departamento: d.departamento,
      ciudad: d.ciudad,
      direccion: d.direccion,
      codigoPostal: d.codigoPostal ?? '',
      telefono: d.telefono,
    });
  }

  protected cancelarEdicion(): void {
    this.editando.set(null);
    this.direccion.reset();
  }

  protected guardarDireccion(): void {
    this.erroresDireccion.set(SIN_ERRORES);
    this.guardandoDireccion.set(true);
    const v = this.direccion.getRawValue();
    const datos: DireccionRequest = { ...v, codigoPostal: v.codigoPostal || null };
    const actual = this.editando();
    const peticion = actual
      ? this.cuenta.actualizarDireccion(actual.id, datos)
      : this.cuenta.crearDireccion(datos);
    peticion.subscribe({
      next: () => {
        this.guardandoDireccion.set(false);
        this.cancelarEdicion();
        this.cargarDirecciones();
      },
      error: (e: unknown) => {
        this.guardandoDireccion.set(false);
        this.erroresDireccion.set(erroresDe(e));
      },
    });
  }

  protected eliminar(d: Direccion): void {
    if (!confirm(`¿Eliminar la dirección «${d.etiqueta}»?`)) {
      return;
    }
    this.cuenta.eliminarDireccion(d.id).subscribe({
      next: () => this.cargarDirecciones(),
      error: (e: unknown) => this.erroresDireccion.set(erroresDe(e)),
    });
  }

  private cargarDirecciones(): void {
    this.cuenta.direcciones().subscribe({
      next: (lista) => this.direcciones.set(lista),
      error: (e: unknown) => this.erroresDireccion.set(erroresDe(e)),
    });
  }
}
