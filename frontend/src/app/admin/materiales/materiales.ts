import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CatalogoAdminService } from '../../core/api/catalogo-admin.service';
import { Material, nombreMaterial } from '../../core/api/modelos';
import { ErroresFormulario, erroresDe } from '../../core/api/problema';

const SIN_ERRORES: ErroresFormulario = { general: null, campos: {} };

@Component({
  selector: 'app-admin-materiales',
  imports: [ReactiveFormsModule],
  template: `
    <section class="pagina">
      <h1>Materiales</h1>
      <p class="nota">El factor de precio aún no se aplica: la fórmula de precio está pendiente de decisión.</p>
      @if (errores().general; as g) { <p class="aviso" role="alert">{{ g }}</p> }
      <div class="tabla-contenedor panel-seccion">
        <table class="tabla">
          <thead><tr><th scope="col">Material</th><th scope="col">Certificado</th><th scope="col">Factor</th><th scope="col"><span class="sr">Acciones</span></th></tr></thead>
          <tbody>
            @for (m of materiales(); track m.id) {
              <tr>
                <td>{{ nombre(m) }}</td>
                <td>{{ m.requiereCertificado ? 'Requiere' : 'No' }}</td>
                <td>{{ m.factorPrecio }}</td>
                <td><button type="button" class="boton boton--secundario boton--pequeno" (click)="editar(m)">Editar</button></td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="nota">Aún no hay materiales.</td></tr>
            }
          </tbody>
        </table>
      </div>
      <section class="panel-seccion">
        <h2>{{ editando() ? 'Editar material' : 'Nuevo material' }}</h2>
        <form class="formulario" [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
          <div class="formulario__fila">
            <div class="campo">
              <label for="m-tipo">Tipo</label>
              <input id="m-tipo" formControlName="tipo" maxlength="30" placeholder="Plata" />
              @if (errores().campos['tipo']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
            <div class="campo">
              <label for="m-ley">Ley (opcional)</label>
              <input id="m-ley" formControlName="ley" maxlength="20" placeholder="925" />
            </div>
            <div class="campo">
              <label for="m-factor">Factor de precio</label>
              <input id="m-factor" type="number" step="0.001" min="0.001" formControlName="factorPrecio" />
              @if (errores().campos['factorPrecio']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
          </div>
          <div class="campo campo--casilla">
            <input id="m-certificado" type="checkbox" formControlName="requiereCertificado" />
            <label for="m-certificado">Requiere certificado</label>
          </div>
          <div class="acciones">
            <button class="boton boton--primario" type="submit" [disabled]="formulario.invalid || guardando()">
              {{ editando() ? 'Guardar cambios' : 'Crear material' }}</button>
            @if (editando()) { <button type="button" class="boton boton--secundario" (click)="cancelar()">Cancelar</button> }
          </div>
        </form>
      </section>
    </section>
  `,
})
export class Materiales implements OnInit {
  private readonly catalogo = inject(CatalogoAdminService);
  protected readonly materiales = signal<Material[]>([]);
  protected readonly editando = signal<Material | null>(null);
  protected readonly errores = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly guardando = signal(false);
  protected readonly nombre = nombreMaterial;

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    tipo: ['', [Validators.required, Validators.maxLength(30)]],
    ley: [''],
    factorPrecio: [1, [Validators.required, Validators.min(0.001)]],
    requiereCertificado: [false],
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected editar(m: Material): void {
    this.editando.set(m);
    this.formulario.setValue({ tipo: m.tipo, ley: m.ley ?? '', factorPrecio: m.factorPrecio, requiereCertificado: m.requiereCertificado });
  }

  protected cancelar(): void {
    this.editando.set(null);
    this.formulario.reset();
  }

  protected guardar(): void {
    this.errores.set(SIN_ERRORES);
    this.guardando.set(true);
    const v = this.formulario.getRawValue();
    const datos = { ...v, ley: v.ley || null };
    const actual = this.editando();
    (actual ? this.catalogo.actualizarMaterial(actual.id, datos) : this.catalogo.crearMaterial(datos)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.cancelar();
        this.cargar();
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.errores.set(erroresDe(e));
      },
    });
  }

  private cargar(): void {
    this.catalogo.materiales().subscribe({
      next: (m) => this.materiales.set(m),
      error: (e: unknown) => this.errores.set(erroresDe(e)),
    });
  }
}
