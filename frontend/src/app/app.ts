import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Asistente } from './shared/asistente/asistente';
import { Cabecera } from './shared/cabecera/cabecera';
import { Pie } from './shared/pie/pie';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Cabecera, Pie, Asistente],
  template: `
    <a class="saltar" href="#contenido">Saltar al contenido</a>
    <app-cabecera />
    <main id="contenido" tabindex="-1"><router-outlet /></main>
    <app-pie />
    <app-asistente />
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 100dvh; }
    main { flex: 1; }
  `,
})
export class App {}
