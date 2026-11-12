import { Component, OnChanges, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CatalogoAdminService } from '../../core/api/catalogo-admin.service';
import { ErroresFormulario, erroresDe } from '../../core/api/problema';

const SIN_ERRORES: ErroresFormulario = { general: null, campos: {} };

/**
 * RF05 / CU-15: título, meta descripción, palabras clave, URL canónica y datos Schema.org
 * adicionales de un producto o una categoría. Lo que se deja vacío se completa automáticamente.
 */
@Component({
  selector: 'app-editor-seo',
  imports: [ReactiveFormsModule],
  template: `
    <form class="formulario" [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <p class="nota">Si dejas un campo vacío, Nalu lo completa con el nombre y la descripción. Precio y disponibilidad se publican siempre actualizados.</p>
      @if (errores().general; as g) { <p class="aviso" role="alert">{{ g }}</p> }
      @if (guardado()) { <p class="aviso aviso--exito" role="status">SEO guardado.</p> }
      <div class="campo">
        <label [for]="prefijo() + '-titulo'">Título de la página ({{ formulario.controls.titulo.value.length }}/160)</label>
        <input [id]="prefijo() + '-titulo'" formControlName="titulo" maxlength="160" />
      </div>
      <div class="campo">
        <label [for]="prefijo() + '-desc'">Meta descripción ({{ formulario.controls.metaDescripcion.value.length }}/320)</label>
        <textarea [id]="prefijo() + '-desc'" formControlName="metaDescripcion" maxlength="320"></textarea>
      </div>
      <div class="formulario__fila">
        <div class="campo">
          <label [for]="prefijo() + '-claves'">Palabras clave</label>
          <input [id]="prefijo() + '-claves'" formControlName="palabrasClave" maxlength="255" placeholder="anillo, plata 925, esmeralda" />
        </div>
        <div class="campo">
          <label [for]="prefijo() + '-canonica'">URL canónica</label>
          <input [id]="prefijo() + '-canonica'" formControlName="urlCanonica" maxlength="255" placeholder="Automática" />
          @if (errores().campos['urlCanonica']; as e) { <p class="campo__error">{{ e }}</p> }
        </div>
      </div>
      <div class="campo">
        <label [for]="prefijo() + '-json'">Datos estructurados adicionales (JSON, opcional)</label>
        <textarea [id]="prefijo() + '-json'" formControlName="datos" spellcheck="false"
                  placeholder='{"brand": {"@type": "Brand", "name": "Nalu"}}'></textarea>
        @if (errorJson() || errores().campos['datosEstructurados']; as e) { <p class="campo__error">{{ e }}</p> }
      </div>
      <div class="acciones">
        <button class="boton boton--primario" type="submit" [disabled]="guardando()">Guardar SEO</button>
      </div>
    </form>
  `,
})
export class EditorSeo implements OnChanges {
  readonly tipo = input.required<'productos' | 'categorias'>();
  readonly idRecurso = input.required<number>();

  private readonly catalogo = inject(CatalogoAdminService);
  protected readonly errores = signal<ErroresFormulario>(SIN_ERRORES);
  protected readonly errorJson = signal<string | null>(null);
  protected readonly guardado = signal(false);
  protected readonly guardando = signal(false);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    titulo: ['', Validators.maxLength(160)],
    metaDescripcion: ['', Validators.maxLength(320)],
    palabrasClave: [''],
    urlCanonica: [''],
    datos: [''],
  });

  protected prefijo(): string {
    return `seo-${this.tipo()}-${this.idRecurso()}`;
  }

  ngOnChanges(): void {
    this.guardado.set(false);
    this.catalogo.seo(this.tipo(), this.idRecurso()).subscribe((s) =>
      this.formulario.setValue({
        titulo: s.titulo ?? '',
        metaDescripcion: s.metaDescripcion ?? '',
        palabrasClave: s.palabrasClave ?? '',
        urlCanonica: s.urlCanonica ?? '',
        datos: s.datosEstructurados ? JSON.stringify(s.datosEstructurados, null, 2) : '',
      }),
    );
  }

  protected guardar(): void {
    this.errores.set(SIN_ERRORES);
    this.errorJson.set(null);
    this.guardado.set(false);
    const v = this.formulario.getRawValue();
    let datos: Record<string, unknown> | null = null;
    if (v.datos.trim()) {
      try {
        const leido: unknown = JSON.parse(v.datos);
        if (!leido || typeof leido !== 'object' || Array.isArray(leido)) throw new Error();
        datos = leido as Record<string, unknown>;
      } catch {
        this.errorJson.set('Debe ser un objeto JSON válido, por ejemplo {"brand": {"@type": "Brand", "name": "Nalu"}}');
        return;
      }
    }
    this.guardando.set(true);
    this.catalogo
      .guardarSeo(this.tipo(), this.idRecurso(), {
        titulo: v.titulo || null,
        metaDescripcion: v.metaDescripcion || null,
        palabrasClave: v.palabrasClave || null,
        urlCanonica: v.urlCanonica || null,
        datosEstructurados: datos,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.guardado.set(true);
        },
        error: (e: unknown) => {
          this.guardando.set(false);
          this.errores.set(erroresDe(e));
        },
      });
  }
}
