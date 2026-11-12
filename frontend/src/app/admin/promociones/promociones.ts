import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CatalogoAdminService, Promocion } from '../../core/api/catalogo-admin.service';
import { ErroresFormulario, erroresDe } from '../../core/api/problema';

const SIN_ERRORES: ErroresFormulario = { general: null, campos: {} };

/** RF11: cupones de descuento. El código no se cambia una vez creado. */
@Component({
  selector: 'app-admin-promociones',
  imports: [ReactiveFormsModule, CurrencyPipe],
  template: `
    <section class="pagina">
      <h1>Cupones</h1>
      @if (errores().general; as g) { <p class="aviso" role="alert">{{ g }}</p> }
      <div class="tabla-contenedor panel-seccion">
        <table class="tabla">
          <thead><tr><th scope="col">Código</th><th scope="col">Descuento</th><th scope="col">Vigencia</th><th scope="col">Usos</th><th scope="col">Estado</th><th scope="col"><span class="sr">Acciones</span></th></tr></thead>
          <tbody>
            @for (p of promociones(); track p.id) {
              <tr>
                <td><code>{{ p.codigo }}</code></td>
                <td>{{ p.tipo === 'PORCENTAJE' ? p.valor + ' %' : (p.valor | currency: 'COP' : 'symbol-narrow' : '1.0-0') }}</td>
                <td>{{ p.fechaInicio }} → {{ p.fechaFin }}</td>
                <td>{{ p.usosActuales }}{{ p.usosMaximos ? ' / ' + p.usosMaximos : '' }}</td>
                <td>{{ p.activa ? 'Activo' : 'Inactivo' }}</td>
                <td><button type="button" class="boton boton--secundario boton--pequeno" (click)="editar(p)">Editar</button></td>
              </tr>
            } @empty { <tr><td colspan="6" class="nota">Aún no hay cupones.</td></tr> }
          </tbody>
        </table>
      </div>
      <section class="panel-seccion">
        <h2>{{ editando() ? 'Editar ' + editando()!.codigo : 'Nuevo cupón' }}</h2>
        <form class="formulario" [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
          <div class="formulario__fila">
            <div class="campo">
              <label for="p-codigo">Código</label>
              <input id="p-codigo" formControlName="codigo" maxlength="30" placeholder="BIENVENIDA" />
              @if (errores().campos['codigo']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
            <div class="campo">
              <label for="p-tipo">Tipo</label>
              <select id="p-tipo" formControlName="tipo">
                <option value="PORCENTAJE">Porcentaje</option>
                <option value="VALOR_FIJO">Valor fijo (COP)</option>
              </select>
            </div>
            <div class="campo">
              <label for="p-valor">Valor</label>
              <input id="p-valor" type="number" min="0.01" step="0.01" formControlName="valor" />
              @if (errores().campos['valor']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
          </div>
          <div class="formulario__fila">
            <div class="campo"><label for="p-inicio">Desde</label><input id="p-inicio" type="date" formControlName="fechaInicio" /></div>
            <div class="campo">
              <label for="p-fin">Hasta</label><input id="p-fin" type="date" formControlName="fechaFin" />
              @if (errores().campos['fechaFin']; as e) { <p class="campo__error">{{ e }}</p> }
            </div>
            <div class="campo"><label for="p-usos">Usos máximos (vacío: sin límite)</label><input id="p-usos" type="number" min="1" step="1" formControlName="usosMaximos" /></div>
          </div>
          <div class="campo campo--casilla">
            <input id="p-activa" type="checkbox" formControlName="activa" /><label for="p-activa">Activo</label>
          </div>
          <div class="acciones">
            <button class="boton boton--primario" type="submit" [disabled]="formulario.invalid || guardando()">Guardar</button>
            @if (editando()) { <button type="button" class="boton boton--secundario" (click)="cancelar()">Cancelar</button> }
          </div>
        </form>
      </section>
    </section>
  `,
})
export class Promociones implements OnInit {
  private readonly catalogo = inject(CatalogoAdminService);
  protected readonly promociones = signal<Promocion[]>([]);
  protected readonly editando = signal<Promocion | null>(null);
  protected readonly errores = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly guardando = signal(false);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    codigo: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9-]{3,30}$/)]],
    tipo: ['PORCENTAJE' as 'PORCENTAJE' | 'VALOR_FIJO'],
    valor: [10, [Validators.required, Validators.min(0.01)]],
    fechaInicio: ['', Validators.required],
    fechaFin: ['', Validators.required],
    usosMaximos: [null as number | null],
    activa: [true],
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected editar(p: Promocion): void {
    this.editando.set(p);
    this.formulario.setValue({ codigo: p.codigo, tipo: p.tipo, valor: p.valor, fechaInicio: p.fechaInicio,
      fechaFin: p.fechaFin, usosMaximos: p.usosMaximos, activa: p.activa });
    this.formulario.controls.codigo.disable();
  }

  protected cancelar(): void {
    this.editando.set(null);
    this.formulario.controls.codigo.enable();
    this.formulario.reset();
  }

  protected guardar(): void {
    this.errores.set(SIN_ERRORES);
    this.guardando.set(true);
    const datos = this.formulario.getRawValue();
    const actual = this.editando();
    (actual ? this.catalogo.actualizarPromocion(actual.id, datos) : this.catalogo.crearPromocion(datos)).subscribe({
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
    this.catalogo.promociones().subscribe({ next: (p) => this.promociones.set(p), error: (e: unknown) => this.errores.set(erroresDe(e)) });
  }
}
